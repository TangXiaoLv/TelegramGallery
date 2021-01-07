package com.tangxiaolv.telegramgallery.utils;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.tangxiaolv.telegramgallery.AnimatedFileDrawable;
import com.tangxiaolv.telegramgallery.DispatchQueue;
import com.tangxiaolv.telegramgallery.Gallery;
import com.tangxiaolv.telegramgallery.ImageReceiver;
import com.tangxiaolv.telegramgallery.tl.Document;
import com.tangxiaolv.telegramgallery.tl.FileLocation;
import com.tangxiaolv.telegramgallery.tl.InputEncryptedFile;
import com.tangxiaolv.telegramgallery.tl.InputFile;
import com.tangxiaolv.telegramgallery.tl.PhotoSize;
import com.tangxiaolv.telegramgallery.tl.TLObject;
import com.tangxiaolv.telegramgallery.secretmedia.EncryptedFileInputStream;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GalleryImageLoader {

    private HashMap<String, Integer> bitmapUseCounts = new HashMap<>();
    private LruCache memCache;
    private HashMap<String, CacheImage> imageLoadingByUrl = new HashMap<>();
    private HashMap<String, CacheImage> imageLoadingByKeys = new HashMap<>();
    private HashMap<Integer, CacheImage> imageLoadingByTag = new HashMap<>();
    private HashMap<String, ThumbGenerateInfo> waitingForQualityThumb = new HashMap<>();
    private HashMap<Integer, String> waitingForQualityThumbByTag = new HashMap<>();
    private LinkedList<HttpImageTask> httpTasks = new LinkedList<>();
    private DispatchQueue cacheOutQueue = new DispatchQueue("cacheOutQueue");
    private DispatchQueue cacheThumbOutQueue = new DispatchQueue("cacheThumbOutQueue");
    private DispatchQueue thumbGeneratingQueue = new DispatchQueue("thumbGeneratingQueue");
    private DispatchQueue imageLoadQueue = new DispatchQueue("imageLoadQueue");
    private ConcurrentHashMap<String, Float> fileProgresses = new ConcurrentHashMap<>();
    private HashMap<String, ThumbGenerateTask> thumbGenerateTasks = new HashMap<>();
    private static byte[] bytes;
    private static byte[] bytesThumb;
    private static byte[] header = new byte[12];
    private static byte[] headerThumb = new byte[12];
    private int currentHttpTasksCount = 0;

    private LinkedList<HttpFileTask> httpFileLoadTasks = new LinkedList<>();
    private HashMap<String, HttpFileTask> httpFileLoadTasksByKeys = new HashMap<>();
    private HashMap<String, Runnable> retryHttpsTasks = new HashMap<>();
    private int currentHttpFileLoadTasksCount = 0;

    private String ignoreRemoval = null;

    private volatile long lastCacheOutTime = 0;
    private int lastImageNum = 0;
    private long lastProgressUpdateTime = 0;

    private File telegramPath = null;

    private class ThumbGenerateInfo {
        private int count;
        private FileLocation fileLocation;
        private String filter;
    }

    private class HttpFileTask extends AsyncTask<Void, Void, Boolean> {

        private String url;
        private File tempFile;
        private String ext;
        private int fileSize;
        private RandomAccessFile fileOutputStream = null;
        private boolean canRetry = true;
        private long lastProgressTime;

        public HttpFileTask(String url, File tempFile, String ext) {
            this.url = url;
            this.tempFile = tempFile;
            this.ext = ext;
        }

        private void reportProgress(final float progress) {
            long currentTime = System.currentTimeMillis();
            if (progress == 1 || lastProgressTime == 0 || lastProgressTime < currentTime - 500) {
                lastProgressTime = currentTime;
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        fileProgresses.put(url, progress);
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileLoadProgressChanged, url, progress);
                            }
                        });
                    }
                });
            }
        }

        protected Boolean doInBackground(Void... voids) {
            InputStream httpConnectionStream = null;
            boolean done = false;

            URLConnection httpConnection = null;
            try {
                URL downloadUrl = new URL(url);
                httpConnection = downloadUrl.openConnection();
                httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
                //httpConnection.addRequestProperty("Referer", "google.com");
                httpConnection.setConnectTimeout(5000);
                httpConnection.setReadTimeout(5000);
                if (httpConnection instanceof HttpURLConnection) {
                    HttpURLConnection httpURLConnection = (HttpURLConnection) httpConnection;
                    httpURLConnection.setInstanceFollowRedirects(true);
                    int status = httpURLConnection.getResponseCode();
                    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == HttpURLConnection.HTTP_SEE_OTHER) {
                        String newUrl = httpURLConnection.getHeaderField("Location");
                        String cookies = httpURLConnection.getHeaderField("Set-Cookie");
                        downloadUrl = new URL(newUrl);
                        httpConnection = downloadUrl.openConnection();
                        httpConnection.setRequestProperty("Cookie", cookies);
                        httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
                        //httpConnection.addRequestProperty("Referer", "google.com");
                    }
                }
                httpConnection.connect();
                httpConnectionStream = httpConnection.getInputStream();

                fileOutputStream = new RandomAccessFile(tempFile, "rws");
            } catch (Throwable e) {
                if (e instanceof UnknownHostException) {
                    canRetry = false;
                } else if (e instanceof SocketException) {
                    if (e.getMessage() != null && e.getMessage().contains("ECONNRESET")) {
                        canRetry = false;
                    }
                } else if (e instanceof FileNotFoundException) {
                    canRetry = false;
                }
                FileLog.e(e);
            }

            if (canRetry) {
                try {
                    if (httpConnection != null && httpConnection instanceof HttpURLConnection) {
                        int code = ((HttpURLConnection) httpConnection).getResponseCode();
                        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_ACCEPTED && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
                            canRetry = false;
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (httpConnection != null) {
                    try {
                        Map<String, List<String>> headerFields = httpConnection.getHeaderFields();
                        if (headerFields != null) {
                            List values = headerFields.get("content-Length");
                            if (values != null && !values.isEmpty()) {
                                String length = (String) values.get(0);
                                if (length != null) {
                                    fileSize = Utilities.parseInt(length);
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }

                if (httpConnectionStream != null) {
                    try {
                        byte[] data = new byte[1024 * 32];
                        int totalLoaded = 0;
                        while (true) {
                            if (isCancelled()) {
                                break;
                            }
                            try {
                                int read = httpConnectionStream.read(data);
                                if (read > 0) {
                                    fileOutputStream.write(data, 0, read);
                                    totalLoaded += read;
                                    if (fileSize > 0) {
                                        reportProgress(totalLoaded / (float) fileSize);
                                    }
                                } else if (read == -1) {
                                    done = true;
                                    if (fileSize != 0) {
                                        reportProgress(1.0f);
                                    }
                                    break;
                                } else {
                                    break;
                                }
                            } catch (Exception e) {
                                FileLog.e(e);
                                break;
                            }
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }

                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                        fileOutputStream = null;
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }

                try {
                    if (httpConnectionStream != null) {
                        httpConnectionStream.close();
                    }
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }

            return done;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            runHttpFileLoadTasks(this, result ? 2 : 1);
        }

        @Override
        protected void onCancelled() {
            runHttpFileLoadTasks(this, 2);
        }
    }

    private class HttpImageTask extends AsyncTask<Void, Void, Boolean> {

        private CacheImage cacheImage = null;
        private RandomAccessFile fileOutputStream = null;
        private int imageSize;
        private long lastProgressTime;
        private boolean canRetry = true;
        private URLConnection httpConnection = null;

        public HttpImageTask(CacheImage cacheImage, int size) {
            this.cacheImage = cacheImage;
            imageSize = size;
        }

        private void reportProgress(final float progress) {
            long currentTime = System.currentTimeMillis();
            if (progress == 1 || lastProgressTime == 0 || lastProgressTime < currentTime - 500) {
                lastProgressTime = currentTime;
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        fileProgresses.put(cacheImage.url, progress);
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileLoadProgressChanged, cacheImage.url, progress);
                            }
                        });
                    }
                });
            }
        }

        protected Boolean doInBackground(Void... voids) {
            InputStream httpConnectionStream = null;
            boolean done = false;

            if (!isCancelled()) {
                try {
                    URL downloadUrl = new URL(cacheImage.httpUrl);
                    httpConnection = downloadUrl.openConnection();
                    httpConnection.addRequestProperty("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 10_0 like Mac OS X) AppleWebKit/602.1.38 (KHTML, like Gecko) Version/10.0 Mobile/14A5297c Safari/602.1");
                    //httpConnection.addRequestProperty("Referer", "google.com");
                    httpConnection.setConnectTimeout(5000);
                    httpConnection.setReadTimeout(5000);
                    if (httpConnection instanceof HttpURLConnection) {
                        ((HttpURLConnection) httpConnection).setInstanceFollowRedirects(true);
                    }
                    if (!isCancelled()) {
                        httpConnection.connect();
                        httpConnectionStream = httpConnection.getInputStream();
                        fileOutputStream = new RandomAccessFile(cacheImage.tempFilePath, "rws");
                    }
                } catch (Throwable e) {
                    if (e instanceof UnknownHostException) {
                        canRetry = false;
                    } else if (e instanceof SocketException) {
                        if (e.getMessage() != null && e.getMessage().contains("ECONNRESET")) {
                            canRetry = false;
                        }
                    } else if (e instanceof FileNotFoundException) {
                        canRetry = false;
                    }
                    FileLog.e(e);
                }
            }

            if (!isCancelled()) {
                try {
                    if (httpConnection != null && httpConnection instanceof HttpURLConnection) {
                        int code = ((HttpURLConnection) httpConnection).getResponseCode();
                        if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_ACCEPTED && code != HttpURLConnection.HTTP_NOT_MODIFIED) {
                            canRetry = false;
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }
                if (imageSize == 0 && httpConnection != null) {
                    try {
                        Map<String, List<String>> headerFields = httpConnection.getHeaderFields();
                        if (headerFields != null) {
                            List values = headerFields.get("content-Length");
                            if (values != null && !values.isEmpty()) {
                                String length = (String) values.get(0);
                                if (length != null) {
                                    imageSize = Utilities.parseInt(length);
                                }
                            }
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }

                if (httpConnectionStream != null) {
                    try {
                        byte[] data = new byte[1024 * 8];
                        int totalLoaded = 0;
                        while (true) {
                            if (isCancelled()) {
                                break;
                            }
                            try {
                                int read = httpConnectionStream.read(data);
                                if (read > 0) {
                                    totalLoaded += read;
                                    fileOutputStream.write(data, 0, read);
                                    if (imageSize != 0) {
                                        reportProgress(totalLoaded / (float) imageSize);
                                    }
                                } else if (read == -1) {
                                    done = true;
                                    if (imageSize != 0) {
                                        reportProgress(1.0f);
                                    }
                                    break;
                                } else {
                                    break;
                                }
                            } catch (Exception e) {
                                FileLog.e(e);
                                break;
                            }
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                }
            }

            try {
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                    fileOutputStream = null;
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }

            try {
                if (httpConnectionStream != null) {
                    httpConnectionStream.close();
                }
            } catch (Throwable e) {
                FileLog.e(e);
            }

            if (done) {
                if (cacheImage.tempFilePath != null) {
                    if (!cacheImage.tempFilePath.renameTo(cacheImage.finalFilePath)) {
                        cacheImage.finalFilePath = cacheImage.tempFilePath;
                    }
                }
            }

            return done;
        }

        @Override
        protected void onPostExecute(final Boolean result) {
            if (result || !canRetry) {
                fileDidLoaded(cacheImage.url, cacheImage.finalFilePath, FileLoader.MEDIA_DIR_IMAGE);
            } else {
                httpFileLoadError(cacheImage.url);
            }
            Utilities.stageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    fileProgresses.remove(cacheImage.url);
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            if (result) {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileDidLoaded, cacheImage.url);
                            } else {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileDidFailedLoad, cacheImage.url, 2);
                            }
                        }
                    });
                }
            });
            imageLoadQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    runHttpTasks(true);
                }
            });
        }

        @Override
        protected void onCancelled() {
            imageLoadQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    runHttpTasks(true);
                }
            });
            Utilities.stageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    fileProgresses.remove(cacheImage.url);
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileDidFailedLoad, cacheImage.url, 1);
                        }
                    });
                }
            });
        }
    }

    private class ThumbGenerateTask implements Runnable {

        private File originalPath;
        private int mediaType;
        private FileLocation thumbLocation;
        private String filter;

        public ThumbGenerateTask(int type, File path, FileLocation location, String f) {
            mediaType = type;
            originalPath = path;
            thumbLocation = location;
            filter = f;
        }

        private void removeTask() {
            if (thumbLocation == null) {
                return;
            }
            final String name = FileLoader.getAttachFileName(thumbLocation);
            imageLoadQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    thumbGenerateTasks.remove(name);
                }
            });
        }

        @Override
        public void run() {
            try {
                if (thumbLocation == null) {
                    removeTask();
                    return;
                }
                final String key = thumbLocation.volume_id + "_" + thumbLocation.local_id;
                File thumbFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), "q_" + key + ".jpg");
                if (thumbFile.exists() || !originalPath.exists()) {
                    removeTask();
                    return;
                }
                int size = Math.min(180, Math.min(AndroidUtilities.displaySize.x, AndroidUtilities.displaySize.y) / 4);
                Bitmap originalBitmap = null;
                if (mediaType == FileLoader.MEDIA_DIR_IMAGE) {
                    originalBitmap = GalleryImageLoader.loadBitmap(originalPath.toString(), null, size, size, false);
                } else if (mediaType == FileLoader.MEDIA_DIR_VIDEO) {
                    originalBitmap = ThumbnailUtils.createVideoThumbnail(originalPath.toString(), MediaStore.Video.Thumbnails.MINI_KIND);
                } else if (mediaType == FileLoader.MEDIA_DIR_DOCUMENT) {
                    String path = originalPath.toString().toLowerCase();
                    if (!path.endsWith(".jpg") && !path.endsWith(".jpeg") && !path.endsWith(".png") && !path.endsWith(".gif")) {
                        removeTask();
                        return;
                    }
                    originalBitmap = GalleryImageLoader.loadBitmap(path, null, size, size, false);
                }
                if (originalBitmap == null) {
                    removeTask();
                    return;
                }

                int w = originalBitmap.getWidth();
                int h = originalBitmap.getHeight();
                if (w == 0 || h == 0) {
                    removeTask();
                    return;
                }
                float scaleFactor = Math.min((float) w / size, (float) h / size);
                Bitmap scaledBitmap = Bitmaps.createScaledBitmap(originalBitmap, (int) (w / scaleFactor), (int) (h / scaleFactor), true);
                if (scaledBitmap != originalBitmap) {
                    originalBitmap.recycle();
                }
                originalBitmap = scaledBitmap;
                FileOutputStream stream = null;
                try {
                    stream = new FileOutputStream(thumbFile);
                    originalBitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream);
                } finally {
                    if (null != stream) {
                        stream.close();
                    }
                }
                final BitmapDrawable bitmapDrawable = new BitmapDrawable(originalBitmap);
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        removeTask();

                        String kf = key;
                        if (filter != null) {
                            kf += "@" + filter;
                        }
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.messageThumbGenerated, bitmapDrawable, kf);
                        /*BitmapDrawable old = memCache.get(kf);
                        if (old != null) {
                            Bitmap image = old.getBitmap();
                            if (runtimeHack != null) {
                                runtimeHack.trackAlloc(image.getRowBytes() * image.getHeight());
                            }
                            if (!image.isRecycled()) {
                                image.recycle();
                            }
                        }*/
                        memCache.put(kf, bitmapDrawable);
                    }
                });
            } catch (Throwable e) {
                FileLog.e(e);
                removeTask();
            }
        }
    }

    private class CacheOutTask implements Runnable {
        private Thread runningThread;
        private final Object sync = new Object();

        private CacheImage cacheImage;
        private boolean isCancelled;

        public CacheOutTask(CacheImage image) {
            cacheImage = image;
        }

        @Override
        public void run() {
            synchronized (sync) {
                runningThread = Thread.currentThread();
                Thread.interrupted();
                if (isCancelled) {
                    return;
                }
            }

            if (cacheImage.animatedFile) {
                synchronized (sync) {
                    if (isCancelled) {
                        return;
                    }
                }
                AnimatedFileDrawable fileDrawable = new AnimatedFileDrawable(cacheImage.finalFilePath, cacheImage.filter != null && cacheImage.filter.equals("d"));
                Thread.interrupted();
                onPostExecute(fileDrawable);
            } else {
                Long mediaId = null;
                boolean mediaIsVideo = false;
                Bitmap image = null;
                File cacheFileFinal = cacheImage.finalFilePath;
                boolean inEncryptedFile = cacheImage.encryptionKeyPath != null && cacheFileFinal != null && cacheFileFinal.getAbsolutePath().endsWith(".enc");
                boolean canDeleteFile = true;
                boolean useNativeWebpLoaded = false;

                if (Build.VERSION.SDK_INT < 19) {
                    RandomAccessFile randomAccessFile = null;
                    try {
                        randomAccessFile = new RandomAccessFile(cacheFileFinal, "r");
                        byte[] bytes;
                        if (cacheImage.thumb) {
                            bytes = headerThumb;
                        } else {
                            bytes = header;
                        }
                        randomAccessFile.readFully(bytes, 0, bytes.length);
                        String str = new String(bytes).toLowerCase();
                        str = str.toLowerCase();
                        if (str.startsWith("riff") && str.endsWith("webp")) {
                            useNativeWebpLoaded = true;
                        }
                        randomAccessFile.close();
                    } catch (Exception e) {
                        FileLog.e(e);
                    } finally {
                        if (randomAccessFile != null) {
                            try {
                                randomAccessFile.close();
                            } catch (Exception e) {
                                FileLog.e(e);
                            }
                        }
                    }
                }

                if (cacheImage.thumb) {
                    int blurType = 0;
                    if (cacheImage.filter != null) {
                        if (cacheImage.filter.contains("b2")) {
                            blurType = 3;
                        } else if (cacheImage.filter.contains("b1")) {
                            blurType = 2;
                        } else if (cacheImage.filter.contains("b")) {
                            blurType = 1;
                        }
                    }

                    try {
                        lastCacheOutTime = System.currentTimeMillis();
                        synchronized (sync) {
                            if (isCancelled) {
                                return;
                            }
                        }

                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 1;

                        if (Build.VERSION.SDK_INT < 21) {
                            opts.inPurgeable = true;
                        }

                        if (useNativeWebpLoaded) {
                            RandomAccessFile file = new RandomAccessFile(cacheFileFinal, "r");
                            ByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, cacheFileFinal.length());

                            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                            bmOptions.inJustDecodeBounds = true;
                            Utilities.loadWebpImage(null, buffer, buffer.limit(), bmOptions, true);
                            image = Bitmaps.createBitmap(bmOptions.outWidth, bmOptions.outHeight, Bitmap.Config.ARGB_8888);

                            Utilities.loadWebpImage(image, buffer, buffer.limit(), null, !opts.inPurgeable);
                            file.close();
                        } else {
                            if (opts.inPurgeable) {
                                RandomAccessFile f = new RandomAccessFile(cacheFileFinal, "r");
                                int len = (int) f.length();
                                byte[] data = bytesThumb != null && bytesThumb.length >= len ? bytesThumb : null;
                                if (data == null) {
                                    bytesThumb = data = new byte[len];
                                }
                                f.readFully(data, 0, len);
                                f.close();
                                if (inEncryptedFile) {
                                    EncryptedFileInputStream.decryptBytesWithKeyFile(data, 0, len, cacheImage.encryptionKeyPath);
                                }
                                image = BitmapFactory.decodeByteArray(data, 0, len, opts);
                            } else {
                                FileInputStream is;
                                if (inEncryptedFile) {
                                    is = new EncryptedFileInputStream(cacheFileFinal, cacheImage.encryptionKeyPath);
                                } else {
                                    is = new FileInputStream(cacheFileFinal);
                                }
                                image = BitmapFactory.decodeStream(is, null, opts);
                                is.close();
                            }
                        }

                        if (image == null) {
                            if (cacheFileFinal.length() == 0 || cacheImage.filter == null) {
                                cacheFileFinal.delete();
                            }
                        } else {
                            if (blurType == 1) {
                                if (image.getConfig() == Bitmap.Config.ARGB_8888) {
                                    Utilities.blurBitmap(image, 3, opts.inPurgeable ? 0 : 1, image.getWidth(), image.getHeight(), image.getRowBytes());
                                }
                            } else if (blurType == 2) {
                                if (image.getConfig() == Bitmap.Config.ARGB_8888) {
                                    Utilities.blurBitmap(image, 1, opts.inPurgeable ? 0 : 1, image.getWidth(), image.getHeight(), image.getRowBytes());
                                }
                            } else if (blurType == 3) {
                                if (image.getConfig() == Bitmap.Config.ARGB_8888) {
                                    Utilities.blurBitmap(image, 7, opts.inPurgeable ? 0 : 1, image.getWidth(), image.getHeight(), image.getRowBytes());
                                    Utilities.blurBitmap(image, 7, opts.inPurgeable ? 0 : 1, image.getWidth(), image.getHeight(), image.getRowBytes());
                                    Utilities.blurBitmap(image, 7, opts.inPurgeable ? 0 : 1, image.getWidth(), image.getHeight(), image.getRowBytes());
                                }
                            } else if (blurType == 0 && opts.inPurgeable) {
                                Utilities.pinBitmap(image);
                            }
                        }
                    } catch (Throwable e) {
                        FileLog.e(e);
                    }
                } else {
                    try {
                        String mediaThumbPath = null;
                        if (cacheImage.httpUrl != null) {
                            if (cacheImage.httpUrl.startsWith("thumb://")) {
                                int idx = cacheImage.httpUrl.indexOf(":", 8);
                                if (idx >= 0) {
                                    mediaId = Long.parseLong(cacheImage.httpUrl.substring(8, idx));
                                    mediaIsVideo = false;
                                    mediaThumbPath = cacheImage.httpUrl.substring(idx + 1);
                                }
                                canDeleteFile = false;
                            } else if (cacheImage.httpUrl.startsWith("vthumb://")) {
                                int idx = cacheImage.httpUrl.indexOf(":", 9);
                                if (idx >= 0) {
                                    mediaId = Long.parseLong(cacheImage.httpUrl.substring(9, idx));
                                    mediaIsVideo = true;
                                }
                                canDeleteFile = false;
                            } else if (!cacheImage.httpUrl.startsWith("http")) {
                                canDeleteFile = false;
                            }
                        }

                        int delay = 20;
                        if (mediaId != null) {
                            delay = 0;
                        }
                        if (delay != 0 && lastCacheOutTime != 0 && lastCacheOutTime > System.currentTimeMillis() - delay && Build.VERSION.SDK_INT < 21) {
                            Thread.sleep(delay);
                        }
                        lastCacheOutTime = System.currentTimeMillis();
                        synchronized (sync) {
                            if (isCancelled) {
                                return;
                            }
                        }

                        BitmapFactory.Options opts = new BitmapFactory.Options();
                        opts.inSampleSize = 1;

                        float w_filter = 0;
                        float h_filter = 0;
                        boolean blur = false;
                        if (cacheImage.filter != null) {
                            String args[] = cacheImage.filter.split("_");
                            if (args.length >= 2) {
                                w_filter = Float.parseFloat(args[0]) * AndroidUtilities.density;
                                h_filter = Float.parseFloat(args[1]) * AndroidUtilities.density;
                            }
                            if (cacheImage.filter.contains("b")) {
                                blur = true;
                            }
                            if (w_filter != 0 && h_filter != 0) {
                                opts.inJustDecodeBounds = true;

                                if (mediaId != null && mediaThumbPath == null) {
                                    if (mediaIsVideo) {
                                        MediaStore.Video.Thumbnails.getThumbnail(Gallery.applicationContext.getContentResolver(), mediaId, MediaStore.Video.Thumbnails.MINI_KIND, opts);
                                    } else {
                                        MediaStore.Images.Thumbnails.getThumbnail(Gallery.applicationContext.getContentResolver(), mediaId, MediaStore.Images.Thumbnails.MINI_KIND, opts);
                                    }
                                } else {
                                    FileInputStream is;
                                    if (inEncryptedFile) {
                                        is = new EncryptedFileInputStream(cacheFileFinal, cacheImage.encryptionKeyPath);
                                    } else {
                                        is = new FileInputStream(cacheFileFinal);
                                    }
                                    image = BitmapFactory.decodeStream(is, null, opts);
                                    is.close();
                                }

                                float photoW = opts.outWidth;
                                float photoH = opts.outHeight;
                                float scaleFactor = Math.max(photoW / w_filter, photoH / h_filter);
                                if (scaleFactor < 1) {
                                    scaleFactor = 1;
                                }
                                opts.inJustDecodeBounds = false;
                                opts.inSampleSize = (int) scaleFactor;
                            }
                        } else if (mediaThumbPath != null) {
                            opts.inJustDecodeBounds = true;
                            FileInputStream is = new FileInputStream(cacheFileFinal);
                            image = BitmapFactory.decodeStream(is, null, opts);
                            is.close();
                            float photoW = opts.outWidth;
                            float photoH = opts.outHeight;
                            float scaleFactor = Math.max(photoW / 512, photoH / 384);
                            if (scaleFactor < 1) {
                                scaleFactor = 1;
                            }
                            opts.inJustDecodeBounds = false;
                            int sample = 1;
                            do {
                                sample *= 2;
                            } while (sample * 2 < scaleFactor);
                            opts.inSampleSize = sample;
                        }
                        synchronized (sync) {
                            if (isCancelled) {
                                return;
                            }
                        }

                        if (cacheImage.filter == null || blur || cacheImage.httpUrl != null) {
                            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        } else {
                            opts.inPreferredConfig = Bitmap.Config.RGB_565;
                        }
                        if (Build.VERSION.SDK_INT < 21) {
                            opts.inPurgeable = true;
                        }

                        opts.inDither = false;
                        if (mediaId != null && mediaThumbPath == null) {
                            if (mediaIsVideo) {
                                image = MediaStore.Video.Thumbnails.getThumbnail(Gallery.applicationContext.getContentResolver(), mediaId, MediaStore.Video.Thumbnails.MINI_KIND, opts);
                            } else {
                                image = MediaStore.Images.Thumbnails.getThumbnail(Gallery.applicationContext.getContentResolver(), mediaId, MediaStore.Images.Thumbnails.MINI_KIND, opts);
                            }
                        }
                        if (image == null) {
                            if (useNativeWebpLoaded) {
                                RandomAccessFile file = new RandomAccessFile(cacheFileFinal, "r");
                                ByteBuffer buffer = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, cacheFileFinal.length());

                                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                                bmOptions.inJustDecodeBounds = true;
                                Utilities.loadWebpImage(null, buffer, buffer.limit(), bmOptions, true);
                                image = Bitmaps.createBitmap(bmOptions.outWidth, bmOptions.outHeight, Bitmap.Config.ARGB_8888);

                                Utilities.loadWebpImage(image, buffer, buffer.limit(), null, !opts.inPurgeable);
                                file.close();
                            } else {
                                if (opts.inPurgeable) {
                                    RandomAccessFile f = new RandomAccessFile(cacheFileFinal, "r");
                                    int len = (int) f.length();
                                    byte[] data = bytes != null && bytes.length >= len ? bytes : null;
                                    if (data == null) {
                                        bytes = data = new byte[len];
                                    }
                                    f.readFully(data, 0, len);
                                    f.close();
                                    if (inEncryptedFile) {
                                        EncryptedFileInputStream.decryptBytesWithKeyFile(data, 0, len, cacheImage.encryptionKeyPath);
                                    }
                                    image = BitmapFactory.decodeByteArray(data, 0, len, opts);
                                } else {
                                    FileInputStream is;
                                    if (inEncryptedFile) {
                                        is = new EncryptedFileInputStream(cacheFileFinal, cacheImage.encryptionKeyPath);
                                    } else {
                                        is = new FileInputStream(cacheFileFinal);
                                    }
                                    image = BitmapFactory.decodeStream(is, null, opts);
                                    is.close();
                                }
                            }
                        }
                        if (image == null) {
                            if (canDeleteFile && (cacheFileFinal.length() == 0 || cacheImage.filter == null)) {
                                cacheFileFinal.delete();
                            }
                        } else {
                            boolean blured = false;
                            if (cacheImage.filter != null) {
                                float bitmapW = image.getWidth();
                                float bitmapH = image.getHeight();
                                if (!opts.inPurgeable && w_filter != 0 && bitmapW != w_filter && bitmapW > w_filter + 20) {
                                    float scaleFactor = bitmapW / w_filter;
                                    Bitmap scaledBitmap = Bitmaps.createScaledBitmap(image, (int) w_filter, (int) (bitmapH / scaleFactor), true);
                                    if (image != scaledBitmap) {
                                        image.recycle();
                                        image = scaledBitmap;
                                    }
                                }
                                if (image != null && blur && bitmapH < 100 && bitmapW < 100) {
                                    if (image.getConfig() == Bitmap.Config.ARGB_8888) {
                                        Utilities.blurBitmap(image, 3, opts.inPurgeable ? 0 : 1, image.getWidth(), image.getHeight(), image.getRowBytes());
                                    }
                                    blured = true;
                                }
                            }
                            if (!blured && opts.inPurgeable) {
                                Utilities.pinBitmap(image);
                            }
                        }
                    } catch (Throwable ignore) {
                        ignore.getStackTrace();
                    }
                }
                Thread.interrupted();
                onPostExecute(image != null ? new BitmapDrawable(image) : null);
            }
        }

        private void onPostExecute(final BitmapDrawable bitmapDrawable) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    BitmapDrawable toSet = null;
                    if (bitmapDrawable instanceof AnimatedFileDrawable) {
                        toSet = bitmapDrawable;
                    } else if (bitmapDrawable != null) {
                        toSet = memCache.get(cacheImage.key);
                        if (toSet == null) {
                            memCache.put(cacheImage.key, bitmapDrawable);
                            toSet = bitmapDrawable;
                        } else {
                            Bitmap image = bitmapDrawable.getBitmap();
                            image.recycle();
                        }
                    }
                    final BitmapDrawable toSetFinal = toSet;
                    imageLoadQueue.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            cacheImage.setImageAndClear(toSetFinal);
                        }
                    });
                }
            });
        }

        public void cancel() {
            synchronized (sync) {
                try {
                    isCancelled = true;
                    if (runningThread != null) {
                        runningThread.interrupt();
                    }
                } catch (Exception e) {
                    //don't promt
                    e.getStackTrace();
                }
            }
        }
    }

    private class CacheImage {
        protected String key;
        protected String url;
        protected String filter;
        protected String ext;
        protected TLObject location;
        protected boolean animatedFile;

        protected File finalFilePath;
        protected File tempFilePath;
        protected boolean thumb;

        protected File encryptionKeyPath;

        protected String httpUrl;
        protected HttpImageTask httpTask;
        protected CacheOutTask cacheTask;

        protected ArrayList<ImageReceiver> imageReceiverArray = new ArrayList<>();
        protected ArrayList<String> keys = new ArrayList<>();
        protected ArrayList<String> filters = new ArrayList<>();

        public void addImageReceiver(ImageReceiver imageReceiver, String key, String filter) {
            if (imageReceiverArray.contains(imageReceiver)) {
                return;
            }
            imageReceiverArray.add(imageReceiver);
            keys.add(key);
            filters.add(filter);
            imageLoadingByTag.put(imageReceiver.getTag(thumb), this);
        }

        public void removeImageReceiver(ImageReceiver imageReceiver) {
            for (int a = 0; a < imageReceiverArray.size(); a++) {
                ImageReceiver obj = imageReceiverArray.get(a);
                if (obj == null || obj == imageReceiver) {
                    imageReceiverArray.remove(a);
                    keys.remove(a);
                    filters.remove(a);
                    if (obj != null) {
                        imageLoadingByTag.remove(obj.getTag(thumb));
                    }
                    a--;
                }
            }
            if (imageReceiverArray.size() == 0) {
                for (int a = 0; a < imageReceiverArray.size(); a++) {
                    imageLoadingByTag.remove(imageReceiverArray.get(a).getTag(thumb));
                }
                imageReceiverArray.clear();
                if (location != null) {
                    if (location instanceof FileLocation) {
                        FileLoader.getInstance().cancelLoadFile((FileLocation) location, ext);
                    } else if (location instanceof Document) {
                        FileLoader.getInstance().cancelLoadFile((Document) location);
                    }
                }
                if (cacheTask != null) {
                    if (thumb) {
                        cacheThumbOutQueue.cancelRunnable(cacheTask);
                    } else {
                        cacheOutQueue.cancelRunnable(cacheTask);
                    }
                    cacheTask.cancel();
                    cacheTask = null;
                }
                if (httpTask != null) {
                    httpTasks.remove(httpTask);
                    httpTask.cancel(true);
                    httpTask = null;
                }
                if (url != null) {
                    imageLoadingByUrl.remove(url);
                }
                if (key != null) {
                    imageLoadingByKeys.remove(key);
                }
            }
        }

        public void setImageAndClear(final BitmapDrawable image) {
            if (image != null) {
                final ArrayList<ImageReceiver> finalImageReceiverArray = new ArrayList<>(imageReceiverArray);
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (image instanceof AnimatedFileDrawable) {
                            boolean imageSet = false;
                            AnimatedFileDrawable fileDrawable = (AnimatedFileDrawable) image;
                            for (int a = 0; a < finalImageReceiverArray.size(); a++) {
                                ImageReceiver imgView = finalImageReceiverArray.get(a);
                                if (imgView.setImageBitmapByKey(a == 0 ? fileDrawable : fileDrawable.makeCopy(), key, thumb, false)) {
                                    imageSet = true;
                                }
                            }
                            if (!imageSet) {
                                ((AnimatedFileDrawable) image).recycle();
                            }
                        } else {
                            for (int a = 0; a < finalImageReceiverArray.size(); a++) {
                                ImageReceiver imgView = finalImageReceiverArray.get(a);
                                imgView.setImageBitmapByKey(image, key, thumb, false);
                            }
                        }
                    }
                });
            }
            for (int a = 0; a < imageReceiverArray.size(); a++) {
                ImageReceiver imageReceiver = imageReceiverArray.get(a);
                imageLoadingByTag.remove(imageReceiver.getTag(thumb));
            }
            imageReceiverArray.clear();
            if (url != null) {
                imageLoadingByUrl.remove(url);
            }
            if (key != null) {
                imageLoadingByKeys.remove(key);
            }
        }
    }

    private static volatile GalleryImageLoader Instance = null;

    public static GalleryImageLoader getInstance() {
        GalleryImageLoader localInstance = Instance;
        if (localInstance == null) {
            synchronized (GalleryImageLoader.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new GalleryImageLoader();
                }
            }
        }
        return localInstance;
    }

    public GalleryImageLoader() {

        cacheOutQueue.setPriority(Thread.MIN_PRIORITY);
        cacheThumbOutQueue.setPriority(Thread.MIN_PRIORITY);
        thumbGeneratingQueue.setPriority(Thread.MIN_PRIORITY);
        imageLoadQueue.setPriority(Thread.MIN_PRIORITY);

        int cacheSize = Math.min(15, ((ActivityManager) Gallery.applicationContext.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryClass() / 7) * 1024 * 1024;

        memCache = new LruCache(cacheSize) {
            @Override
            protected int sizeOf(String key, BitmapDrawable value) {
                return value.getBitmap().getByteCount();
            }

            @Override
            protected void entryRemoved(boolean evicted, String key, final BitmapDrawable oldValue, BitmapDrawable newValue) {
                if (ignoreRemoval != null && key != null && ignoreRemoval.equals(key)) {
                    return;
                }
                final Integer count = bitmapUseCounts.get(key);
                if (count == null || count == 0) {
                    Bitmap b = oldValue.getBitmap();
                    if (!b.isRecycled()) {
                        b.recycle();
                    }
                }
            }
        };

        FileLoader.getInstance().setDelegate(new FileLoader.FileLoaderDelegate() {
            @Override
            public void fileUploadProgressChanged(final String location, final float progress, final boolean isEncrypted) {
                fileProgresses.put(location, progress);
                long currentTime = System.currentTimeMillis();
                if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 500) {
                    lastProgressUpdateTime = currentTime;

                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileUploadProgressChanged, location, progress, isEncrypted);
                        }
                    });
                }
            }

            @Override
            public void fileDidUploaded(final String location, final InputFile inputFile, final InputEncryptedFile inputEncryptedFile, final byte[] key, final byte[] iv, final long totalFileSize) {
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileDidUpload, location, inputFile, inputEncryptedFile, key, iv, totalFileSize);
                            }
                        });
                        fileProgresses.remove(location);
                    }
                });
            }

            @Override
            public void fileDidFailedUpload(final String location, final boolean isEncrypted) {
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        AndroidUtilities.runOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileDidFailUpload, location, isEncrypted);
                            }
                        });
                        fileProgresses.remove(location);
                    }
                });
            }

            @Override
            public void fileDidLoaded(final String location, final File finalFile, final int type) {
                fileProgresses.remove(location);
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        if (MediaController.getInstance().canSaveToGallery() && telegramPath != null && finalFile != null && (location.endsWith(".mp4") || location.endsWith(".jpg"))) {
                            if (finalFile.toString().startsWith(telegramPath.toString())) {
                                AndroidUtilities.addMediaToGallery(finalFile.toString());
                            }
                        }
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileDidLoaded, location);
                        GalleryImageLoader.this.fileDidLoaded(location, finalFile, type);
                    }
                });
            }

            @Override
            public void fileDidFailedLoad(final String location, final int canceled) {
                fileProgresses.remove(location);
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        GalleryImageLoader.this.fileDidFailedLoad(location, canceled);
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileDidFailedLoad, location, canceled);
                    }
                });
            }

            @Override
            public void fileLoadProgressChanged(final String location, final float progress) {
                fileProgresses.put(location, progress);
                long currentTime = System.currentTimeMillis();
                if (lastProgressUpdateTime == 0 || lastProgressUpdateTime < currentTime - 500) {
                    lastProgressUpdateTime = currentTime;
                    AndroidUtilities.runOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.FileLoadProgressChanged, location, progress);
                        }
                    });
                }
            }
        });

        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context arg0, Intent intent) {
                FileLog.e("file system changed");
                Runnable r = new Runnable() {
                    public void run() {
                        checkMediaPaths();
                    }
                };
                if (Intent.ACTION_MEDIA_UNMOUNTED.equals(intent.getAction())) {
                    AndroidUtilities.runOnUIThread(r, 1000);
                } else {
                    r.run();
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        filter.addAction(Intent.ACTION_MEDIA_CHECKING);
        filter.addAction(Intent.ACTION_MEDIA_EJECT);
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_NOFS);
        filter.addAction(Intent.ACTION_MEDIA_REMOVED);
        filter.addAction(Intent.ACTION_MEDIA_SHARED);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTABLE);
        filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addDataScheme("file");
        Gallery.applicationContext.registerReceiver(receiver, filter);

        HashMap<Integer, File> mediaDirs = new HashMap<>();
        File cachePath = AndroidUtilities.getCacheDir();
        if (!cachePath.isDirectory()) {
            try {
                cachePath.mkdirs();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        try {
            new File(cachePath, ".nomedia").createNewFile();
        } catch (Exception e) {
            FileLog.e(e);
        }
        mediaDirs.put(FileLoader.MEDIA_DIR_CACHE, cachePath);
        FileLoader.getInstance().setMediaDirs(mediaDirs);

        checkMediaPaths();
    }

    public void checkMediaPaths() {
        cacheOutQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                final HashMap<Integer, File> paths = createMediaPaths();
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        FileLoader.getInstance().setMediaDirs(paths);
                    }
                });
            }
        });
    }

    public HashMap<Integer, File> createMediaPaths() {
        HashMap<Integer, File> mediaDirs = new HashMap<>();
        File cachePath = AndroidUtilities.getCacheDir();
        if (!cachePath.isDirectory()) {
            try {
                cachePath.mkdirs();
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        try {
            new File(cachePath, ".nomedia").createNewFile();
        } catch (Exception e) {
            FileLog.e(e);
        }

        mediaDirs.put(FileLoader.MEDIA_DIR_CACHE, cachePath);
        FileLog.e("cache path = " + cachePath);

        try {
            if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
                telegramPath = new File(Environment.getExternalStorageDirectory(), "toon");
                telegramPath.mkdirs();

                if (telegramPath.isDirectory()) {
                    try {
                        File imagePath = new File(telegramPath, "Toon Images");
                        imagePath.mkdir();
                        if (imagePath.isDirectory() && canMoveFiles(cachePath, imagePath, FileLoader.MEDIA_DIR_IMAGE)) {
                            mediaDirs.put(FileLoader.MEDIA_DIR_IMAGE, imagePath);
                            FileLog.e("image path = " + imagePath);
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }

                    try {
                        File videoPath = new File(telegramPath, "Toon Video");
                        videoPath.mkdir();
                        if (videoPath.isDirectory() && canMoveFiles(cachePath, videoPath, FileLoader.MEDIA_DIR_VIDEO)) {
                            mediaDirs.put(FileLoader.MEDIA_DIR_VIDEO, videoPath);
                            FileLog.e("video path = " + videoPath);
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }

                    try {
                        File audioPath = new File(telegramPath, "Toon Audio");
                        audioPath.mkdir();
                        if (audioPath.isDirectory() && canMoveFiles(cachePath, audioPath, FileLoader.MEDIA_DIR_AUDIO)) {
                            new File(audioPath, ".nomedia").createNewFile();
                            mediaDirs.put(FileLoader.MEDIA_DIR_AUDIO, audioPath);
                            FileLog.e("audio path = " + audioPath);
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }

                    try {
                        File documentPath = new File(telegramPath, "Toon Documents");
                        documentPath.mkdir();
                        if (documentPath.isDirectory() && canMoveFiles(cachePath, documentPath, FileLoader.MEDIA_DIR_DOCUMENT)) {
                            new File(documentPath, ".nomedia").createNewFile();
                            mediaDirs.put(FileLoader.MEDIA_DIR_DOCUMENT, documentPath);
                            FileLog.e("documents path = " + documentPath);
                        }
                    } catch (Exception e) {
                        FileLog.e(e);
                    }
                }
            } else {
                FileLog.e("this Android can't rename files");
            }
            MediaController.getInstance().checkSaveToGalleryFiles();
        } catch (Exception e) {
            FileLog.e(e);
        }

        return mediaDirs;
    }

    private boolean canMoveFiles(File from, File to, int type) {
        RandomAccessFile file = null;
        try {
            File srcFile = null;
            File dstFile = null;
            if (type == FileLoader.MEDIA_DIR_IMAGE) {
                srcFile = new File(from, "000000000_999999_temp.jpg");
                dstFile = new File(to, "000000000_999999.jpg");
            } else if (type == FileLoader.MEDIA_DIR_DOCUMENT) {
                srcFile = new File(from, "000000000_999999_temp.doc");
                dstFile = new File(to, "000000000_999999.doc");
            } else if (type == FileLoader.MEDIA_DIR_AUDIO) {
                srcFile = new File(from, "000000000_999999_temp.ogg");
                dstFile = new File(to, "000000000_999999.ogg");
            } else if (type == FileLoader.MEDIA_DIR_VIDEO) {
                srcFile = new File(from, "000000000_999999_temp.mp4");
                dstFile = new File(to, "000000000_999999.mp4");
            }
            byte[] buffer = new byte[1024];
            srcFile.createNewFile();
            file = new RandomAccessFile(srcFile, "rws");
            file.write(buffer);
            file.close();
            file = null;
            boolean canRename = srcFile.renameTo(dstFile);
            srcFile.delete();
            dstFile.delete();
            if (canRename) {
                return true;
            }
        } catch (Exception e) {
            FileLog.e(e);
        } finally {
            try {
                if (file != null) {
                    file.close();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
        return false;
    }

    public Float getFileProgress(String location) {
        if (location == null) {
            return null;
        }
        return fileProgresses.get(location);
    }

    private void performReplace(String oldKey, String newKey) {
        BitmapDrawable b = memCache.get(oldKey);
        if (b != null) {
            BitmapDrawable oldBitmap = memCache.get(newKey);
            boolean dontChange = false;
            if (oldBitmap != null && oldBitmap.getBitmap() != null && b.getBitmap() != null) {
                Bitmap oldBitmapObject = oldBitmap.getBitmap();
                Bitmap newBitmapObject = b.getBitmap();
                if (oldBitmapObject.getWidth() > newBitmapObject.getWidth() || oldBitmapObject.getHeight() > newBitmapObject.getHeight()) {
                    dontChange = true;
                }
            }
            if (!dontChange) {
                ignoreRemoval = oldKey;
                memCache.remove(oldKey);
                memCache.put(newKey, b);
                ignoreRemoval = null;
            } else {
                memCache.remove(oldKey);
            }
        }
        Integer val = bitmapUseCounts.get(oldKey);
        if (val != null) {
            bitmapUseCounts.put(newKey, val);
            bitmapUseCounts.remove(oldKey);
        }
    }

    public void incrementUseCount(String key) {
        Integer count = bitmapUseCounts.get(key);
        if (count == null) {
            bitmapUseCounts.put(key, 1);
        } else {
            bitmapUseCounts.put(key, count + 1);
        }
    }

    public boolean decrementUseCount(String key) {
        Integer count = bitmapUseCounts.get(key);
        if (count == null) {
            return true;
        }
        if (count == 1) {
            bitmapUseCounts.remove(key);
            return true;
        } else {
            bitmapUseCounts.put(key, count - 1);
        }
        return false;
    }

    public void removeImage(String key) {
        bitmapUseCounts.remove(key);
        memCache.remove(key);
    }

    public boolean isInCache(String key) {
        return memCache.get(key) != null;
    }

    public void clearMemory() {
        memCache.evictAll();
    }

    private void removeFromWaitingForThumb(Integer TAG) {
        String location = waitingForQualityThumbByTag.get(TAG);
        if (location != null) {
            ThumbGenerateInfo info = waitingForQualityThumb.get(location);
            if (info != null) {
                info.count--;
                if (info.count == 0) {
                    waitingForQualityThumb.remove(location);
                }
            }
            waitingForQualityThumbByTag.remove(TAG);
        }
    }

    public void cancelLoadingForImageReceiver(final ImageReceiver imageReceiver, final int type) {
        if (imageReceiver == null) {
            return;
        }
        imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                int start = 0;
                int count = 2;
                if (type == 1) {
                    count = 1;
                } else if (type == 2) {
                    start = 1;
                }
                for (int a = start; a < count; a++) {
                    Integer TAG = imageReceiver.getTag(a == 0);
                    if (a == 0) {
                        removeFromWaitingForThumb(TAG);
                    }
                    if (TAG != null) {
                        CacheImage ei = imageLoadingByTag.get(TAG);
                        if (ei != null) {
                            ei.removeImageReceiver(imageReceiver);
                        }
                    }
                }
            }
        });
    }

    public BitmapDrawable getImageFromMemory(String key) {
        return memCache.get(key);
    }

    public BitmapDrawable getImageFromMemory(TLObject fileLocation, String httpUrl, String filter) {
        if (fileLocation == null && httpUrl == null) {
            return null;
        }
        String key = null;
        if (httpUrl != null) {
            key = Utilities.MD5(httpUrl);
        } else {
            if (fileLocation instanceof FileLocation) {
                FileLocation location = (FileLocation) fileLocation;
                key = location.volume_id + "_" + location.local_id;
            } else if (fileLocation instanceof Document) {
                Document location = (Document) fileLocation;
                if (location.version == 0) {
                    key = location.dc_id + "_" + location.id;
                } else {
                    key = location.dc_id + "_" + location.id + "_" + location.version;
                }
            }
        }
        if (filter != null) {
            key += "@" + filter;
        }
        return memCache.get(key);
    }

    private void replaceImageInCacheInternal(final String oldKey, final String newKey, final FileLocation newLocation) {
        ArrayList<String> arr = memCache.getFilterKeys(oldKey);
        if (arr != null) {
            for (int a = 0; a < arr.size(); a++) {
                String filter = arr.get(a);
                String oldK = oldKey + "@" + filter;
                String newK = newKey + "@" + filter;
                performReplace(oldK, newK);
                NotificationCenter.getInstance().postNotificationName(NotificationCenter.didReplacedPhotoInMemCache, oldK, newK, newLocation);
            }
        } else {
            performReplace(oldKey, newKey);
            NotificationCenter.getInstance().postNotificationName(NotificationCenter.didReplacedPhotoInMemCache, oldKey, newKey, newLocation);
        }
    }

    public void replaceImageInCache(final String oldKey, final String newKey, final FileLocation newLocation, boolean post) {
        if (post) {
            AndroidUtilities.runOnUIThread(new Runnable() {
                @Override
                public void run() {
                    replaceImageInCacheInternal(oldKey, newKey, newLocation);
                }
            });
        } else {
            replaceImageInCacheInternal(oldKey, newKey, newLocation);
        }
    }

    public void putImageToCache(BitmapDrawable bitmap, String key) {
        memCache.put(key, bitmap);
    }

    private void generateThumb(int mediaType, File originalPath, FileLocation thumbLocation, String filter) {
        if (mediaType != FileLoader.MEDIA_DIR_IMAGE && mediaType != FileLoader.MEDIA_DIR_VIDEO && mediaType != FileLoader.MEDIA_DIR_DOCUMENT || originalPath == null || thumbLocation == null) {
            return;
        }
        String name = FileLoader.getAttachFileName(thumbLocation);
        ThumbGenerateTask task = thumbGenerateTasks.get(name);
        if (task == null) {
            task = new ThumbGenerateTask(mediaType, originalPath, thumbLocation, filter);
            thumbGeneratingQueue.postRunnable(task);
        }
    }

    private void createLoadOperationForImageReceiver(final ImageReceiver imageReceiver, final String key, final String url, final String ext, final TLObject imageLocation, final String httpLocation, final String filter, final int size, final int cacheType, final int thumb) {
        if (imageReceiver == null || url == null || key == null) {
            return;
        }
        Integer TAG = imageReceiver.getTag(thumb != 0);
        if (TAG == null) {
            imageReceiver.setTag(TAG = lastImageNum, thumb != 0);
            lastImageNum++;
            if (lastImageNum == Integer.MAX_VALUE) {
                lastImageNum = 0;
            }
        }

        final Integer finalTag = TAG;
        final boolean finalIsNeedsQualityThumb = imageReceiver.isNeedsQualityThumb();
        final boolean shouldGenerateQualityThumb = imageReceiver.isShouldGenerateQualityThumb();
        imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                boolean added = false;
                if (thumb != 2) {
                    CacheImage alreadyLoadingUrl = imageLoadingByUrl.get(url);
                    CacheImage alreadyLoadingCache = imageLoadingByKeys.get(key);
                    CacheImage alreadyLoadingImage = imageLoadingByTag.get(finalTag);
                    if (alreadyLoadingImage != null) {
                        if (alreadyLoadingImage == alreadyLoadingUrl || alreadyLoadingImage == alreadyLoadingCache) {
                            added = true;
                        } else {
                            alreadyLoadingImage.removeImageReceiver(imageReceiver);
                        }
                    }

                    if (!added && alreadyLoadingCache != null) {
                        alreadyLoadingCache.addImageReceiver(imageReceiver, key, filter);
                        added = true;
                    }
                    if (!added && alreadyLoadingUrl != null) {
                        alreadyLoadingUrl.addImageReceiver(imageReceiver, key, filter);
                        added = true;
                    }
                }

                if (!added) {
                    boolean onlyCache = false;
                    boolean isQuality = false;
                    File cacheFile = null;
                    boolean cacheFileExists = false;

                    if (httpLocation != null) {
                        if (!httpLocation.startsWith("http")) {
                            onlyCache = true;
                            if (httpLocation.startsWith("thumb://")) {
                                int idx = httpLocation.indexOf(":", 8);
                                if (idx >= 0) {
                                    cacheFile = new File(httpLocation.substring(idx + 1));
                                }
                            } else if (httpLocation.startsWith("vthumb://")) {
                                int idx = httpLocation.indexOf(":", 9);
                                if (idx >= 0) {
                                    cacheFile = new File(httpLocation.substring(idx + 1));
                                }
                            } else {
                                cacheFile = new File(httpLocation);
                            }
                        }
                    } else if (thumb != 0) {
                        if (finalIsNeedsQualityThumb) {
                            cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), "q_" + url);
                            if (!cacheFile.exists()) {
                                cacheFile = null;
                            } else {
                                cacheFileExists = true;
                            }
                        }
                    }

                    if (thumb != 2) {
                        boolean isEncrypted = imageLocation instanceof Document.TL_documentEncrypted ||
                                imageLocation instanceof FileLocation.TL_fileEncryptedLocation;
                        CacheImage img = new CacheImage();
                        if (httpLocation != null &&
                                !httpLocation.startsWith("vthumb") &&
                                !httpLocation.startsWith("thumb") &&
                                (httpLocation.endsWith("mp4") || httpLocation.endsWith("gif")) ||
                                imageLocation instanceof Document) {
                            img.animatedFile = true;
                        }

                        if (cacheFile == null) {
                            if (cacheType != 0 || size == 0 || httpLocation != null || isEncrypted) {
                                cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), url);
                                if (cacheFile.exists()) {
                                    cacheFileExists = true;
                                } else if (cacheType == 2) {
                                    cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), url + ".enc");
                                }
                            } else if (imageLocation instanceof Document) {
                                cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_DOCUMENT), url);
                            } else {
                                cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_IMAGE), url);
                            }
                        }

                        img.thumb = thumb != 0;
                        img.key = key;
                        img.filter = filter;
                        img.httpUrl = httpLocation;
                        img.ext = ext;
                        if (cacheType == 2) {
                            img.encryptionKeyPath = new File(FileLoader.getInternalCacheDir(), url + ".enc.key");
                        }
                        img.addImageReceiver(imageReceiver, key, filter);
                        if (onlyCache || cacheFileExists || cacheFile.exists()) {
                            img.finalFilePath = cacheFile;
                            img.cacheTask = new CacheOutTask(img);
                            imageLoadingByKeys.put(key, img);
                            if (thumb != 0) {
                                cacheThumbOutQueue.postRunnable(img.cacheTask);
                            } else {
                                cacheOutQueue.postRunnable(img.cacheTask);
                            }
                        } else {
                            img.url = url;
                            img.location = imageLocation;
                            imageLoadingByUrl.put(url, img);
                            if (httpLocation == null) {
                                if (imageLocation instanceof FileLocation) {
                                    FileLocation location = (FileLocation) imageLocation;
                                    int localCacheType = cacheType;
                                    if (localCacheType == 0 && (size == 0 || location.key != null)) {
                                        localCacheType = 1;
                                    }
                                    FileLoader.getInstance().loadFile(location, ext, size, localCacheType);
                                } else if (imageLocation instanceof Document) {
                                    FileLoader.getInstance().loadFile((Document) imageLocation, true, cacheType);
                                }
                            } else {
                                String file = Utilities.MD5(httpLocation);
                                File cacheDir = FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE);
                                img.tempFilePath = new File(cacheDir, file + "_temp.jpg");
                                img.finalFilePath = cacheFile;
                                img.httpTask = new HttpImageTask(img, size);
                                httpTasks.add(img.httpTask);
                                runHttpTasks(false);
                            }
                        }
                    }
                }
            }
        });
    }

    public void loadImageForImageReceiver(ImageReceiver imageReceiver) {
        if (imageReceiver == null) {
            return;
        }

        boolean imageSet = false;
        String key = imageReceiver.getKey();
        if (key != null) {
            BitmapDrawable bitmapDrawable = memCache.get(key);
            if (bitmapDrawable != null) {
                cancelLoadingForImageReceiver(imageReceiver, 0);
                imageReceiver.setImageBitmapByKey(bitmapDrawable, key, false, true);
                imageSet = true;
                if (!imageReceiver.isForcePreview()) {
                    return;
                }
            }
        }
        boolean thumbSet = false;
        String thumbKey = imageReceiver.getThumbKey();
        if (thumbKey != null) {
            BitmapDrawable bitmapDrawable = memCache.get(thumbKey);
            if (bitmapDrawable != null) {
                imageReceiver.setImageBitmapByKey(bitmapDrawable, thumbKey, true, true);
                cancelLoadingForImageReceiver(imageReceiver, 1);
                if (imageSet && imageReceiver.isForcePreview()) {
                    return;
                }
                thumbSet = true;
            }
        }

        FileLocation thumbLocation = imageReceiver.getThumbLocation();
        TLObject imageLocation = imageReceiver.getImageLocation();
        String httpLocation = imageReceiver.getHttpImageLocation();

        boolean saveImageToCache = false;

        String url = null;
        String thumbUrl = null;
        key = null;
        thumbKey = null;
        String ext = imageReceiver.getExt();
        if (ext == null) {
            ext = "jpg";
        }
        if (httpLocation != null) {
            key = Utilities.MD5(httpLocation);
            url = key + "." + getHttpUrlExtension(httpLocation, "jpg");
        } else if (imageLocation != null) {
            if (imageLocation instanceof FileLocation) {
                FileLocation location = (FileLocation) imageLocation;
                key = location.volume_id + "_" + location.local_id;
                url = key + "." + ext;
                if (imageReceiver.getExt() != null || location.key != null || location.volume_id == Integer.MIN_VALUE && location.local_id < 0) {
                    saveImageToCache = true;
                }
            } else if (imageLocation instanceof Document) {
                Document document = (Document) imageLocation;
                if (document.id == 0 || document.dc_id == 0) {
                    return;
                }
                if (document.version == 0) {
                    key = document.dc_id + "_" + document.id;
                } else {
                    key = document.dc_id + "_" + document.id + "_" + document.version;
                }

                String docExt = FileLoader.getDocumentFileName(document);
                int idx;
                if (docExt == null || (idx = docExt.lastIndexOf('.')) == -1) {
                    docExt = "";
                } else {
                    docExt = docExt.substring(idx);
                }
                if (docExt.length() <= 1) {
                    if (document.mime_type != null && document.mime_type.equals("video/mp4")) {
                        docExt = ".mp4";
                    } else {
                        docExt = "";
                    }
                }
                url = key + docExt;
                if (thumbKey != null) {
                    thumbUrl = thumbKey + "." + ext;
                }
            }
            if (imageLocation == thumbLocation) {
                imageLocation = null;
                key = null;
                url = null;
            }
        }

        if (thumbLocation != null) {
            thumbKey = thumbLocation.volume_id + "_" + thumbLocation.local_id;
            thumbUrl = thumbKey + "." + ext;
        }

        String filter = imageReceiver.getFilter();
        String thumbFilter = imageReceiver.getThumbFilter();
        if (key != null && filter != null) {
            key += "@" + filter;
        }
        if (thumbKey != null && thumbFilter != null) {
            thumbKey += "@" + thumbFilter;
        }

        if (httpLocation != null) {
            createLoadOperationForImageReceiver(imageReceiver, thumbKey, thumbUrl, ext, thumbLocation, null, thumbFilter, 0, 1, thumbSet ? 2 : 1);
            createLoadOperationForImageReceiver(imageReceiver, key, url, ext, null, httpLocation, filter, 0, 1, 0);
        } else {
            int cacheType = imageReceiver.getCacheType();
            if (cacheType == 0 && saveImageToCache) {
                cacheType = 1;
            }
            createLoadOperationForImageReceiver(imageReceiver, thumbKey, thumbUrl, ext, thumbLocation, null, thumbFilter, 0, cacheType == 0 ? 1 : cacheType, thumbSet ? 2 : 1);
            createLoadOperationForImageReceiver(imageReceiver, key, url, ext, imageLocation, null, filter, imageReceiver.getSize(), cacheType, 0);
        }
    }

    private void httpFileLoadError(final String location) {
        imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                CacheImage img = imageLoadingByUrl.get(location);
                if (img == null) {
                    return;
                }
                HttpImageTask oldTask = img.httpTask;
                img.httpTask = new HttpImageTask(oldTask.cacheImage, oldTask.imageSize);
                httpTasks.add(img.httpTask);
                runHttpTasks(false);
            }
        });
    }

    private void fileDidLoaded(final String location, final File finalFile, final int type) {
        imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                ThumbGenerateInfo info = waitingForQualityThumb.get(location);
                if (info != null) {
                    generateThumb(type, finalFile, info.fileLocation, info.filter);
                    waitingForQualityThumb.remove(location);
                }
                CacheImage img = imageLoadingByUrl.get(location);
                if (img == null) {
                    return;
                }
                imageLoadingByUrl.remove(location);
                ArrayList<CacheOutTask> tasks = new ArrayList<>();
                for (int a = 0; a < img.imageReceiverArray.size(); a++) {
                    String key = img.keys.get(a);
                    String filter = img.filters.get(a);
                    ImageReceiver imageReceiver = img.imageReceiverArray.get(a);
                    CacheImage cacheImage = imageLoadingByKeys.get(key);
                    if (cacheImage == null) {
                        cacheImage = new CacheImage();
                        cacheImage.finalFilePath = finalFile;
                        cacheImage.key = key;
                        cacheImage.httpUrl = img.httpUrl;
                        cacheImage.thumb = img.thumb;
                        cacheImage.ext = img.ext;
                        cacheImage.encryptionKeyPath = img.encryptionKeyPath;
                        cacheImage.cacheTask = new CacheOutTask(cacheImage);
                        cacheImage.filter = filter;
                        cacheImage.animatedFile = img.animatedFile;
                        imageLoadingByKeys.put(key, cacheImage);
                        tasks.add(cacheImage.cacheTask);
                    }
                    cacheImage.addImageReceiver(imageReceiver, key, filter);
                }
                for (int a = 0; a < tasks.size(); a++) {
                    if (img.thumb) {
                        cacheThumbOutQueue.postRunnable(tasks.get(a));
                    } else {
                        cacheOutQueue.postRunnable(tasks.get(a));
                    }
                }
            }
        });
    }

    private void fileDidFailedLoad(final String location, int canceled) {
        if (canceled == 1) {
            return;
        }
        imageLoadQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                CacheImage img = imageLoadingByUrl.get(location);
                if (img != null) {
                    img.setImageAndClear(null);
                }
            }
        });
    }

    private void runHttpTasks(boolean complete) {
        if (complete) {
            currentHttpTasksCount--;
        }
        while (currentHttpTasksCount < 4 && !httpTasks.isEmpty()) {
            HttpImageTask task = httpTasks.poll();
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
            currentHttpTasksCount++;
        }
    }

    public boolean isLoadingHttpFile(String url) {
        return httpFileLoadTasksByKeys.containsKey(url);
    }

    public void loadHttpFile(String url, String defaultExt) {
        if (url == null || url.length() == 0 || httpFileLoadTasksByKeys.containsKey(url)) {
            return;
        }
        String ext = getHttpUrlExtension(url, defaultExt);
        File file = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.MD5(url) + "_temp." + ext);
        file.delete();

        HttpFileTask task = new HttpFileTask(url, file, ext);
        httpFileLoadTasks.add(task);
        httpFileLoadTasksByKeys.put(url, task);
        runHttpFileLoadTasks(null, 0);
    }

    public void cancelLoadHttpFile(String url) {
        HttpFileTask task = httpFileLoadTasksByKeys.get(url);
        if (task != null) {
            task.cancel(true);
            httpFileLoadTasksByKeys.remove(url);
            httpFileLoadTasks.remove(task);
        }
        Runnable runnable = retryHttpsTasks.get(url);
        if (runnable != null) {
            AndroidUtilities.cancelRunOnUIThread(runnable);
        }
        runHttpFileLoadTasks(null, 0);
    }

    private void runHttpFileLoadTasks(final HttpFileTask oldTask, final int reason) {
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                if (oldTask != null) {
                    currentHttpFileLoadTasksCount--;
                }
                if (oldTask != null) {
                    if (reason == 1) {
                        if (oldTask.canRetry) {
                            final HttpFileTask newTask = new HttpFileTask(oldTask.url, oldTask.tempFile, oldTask.ext);
                            Runnable runnable = new Runnable() {
                                @Override
                                public void run() {
                                    httpFileLoadTasks.add(newTask);
                                    runHttpFileLoadTasks(null, 0);
                                }
                            };
                            retryHttpsTasks.put(oldTask.url, runnable);
                            AndroidUtilities.runOnUIThread(runnable, 1000);
                        } else {
                            httpFileLoadTasksByKeys.remove(oldTask.url);
                            NotificationCenter.getInstance().postNotificationName(NotificationCenter.httpFileDidFailedLoad, oldTask.url, 0);
                        }
                    } else if (reason == 2) {
                        httpFileLoadTasksByKeys.remove(oldTask.url);
                        File file = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), Utilities.MD5(oldTask.url) + "." + oldTask.ext);
                        String result = oldTask.tempFile.renameTo(file) ? file.toString() : oldTask.tempFile.toString();
                        NotificationCenter.getInstance().postNotificationName(NotificationCenter.httpFileDidLoaded, oldTask.url, result);
                    }
                }
                while (currentHttpFileLoadTasksCount < 2 && !httpFileLoadTasks.isEmpty()) {
                    HttpFileTask task = httpFileLoadTasks.poll();
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null, null, null);
                    currentHttpFileLoadTasksCount++;
                }
            }
        });
    }

    public static Bitmap loadBitmap(String path, Uri uri, float maxWidth, float maxHeight, boolean useMaxScale) {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        InputStream inputStream = null;

        if (path == null && uri != null && uri.getScheme() != null) {
            String imageFilePath = null;
            if (uri.getScheme().contains("file")) {
                path = uri.getPath();
            } else {
                try {
                    path = AndroidUtilities.getPath(uri);
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        }

        if (path != null) {
            BitmapFactory.decodeFile(path, bmOptions);
        } else if (uri != null) {
            boolean error = false;
            try {
                inputStream = Gallery.applicationContext.getContentResolver().openInputStream(uri);
                BitmapFactory.decodeStream(inputStream, null, bmOptions);
                inputStream.close();
                inputStream = Gallery.applicationContext.getContentResolver().openInputStream(uri);
            } catch (Throwable e) {
                FileLog.e(e);
                return null;
            }
        }
        float photoW = bmOptions.outWidth;
        float photoH = bmOptions.outHeight;
        float scaleFactor = useMaxScale ? Math.max(photoW / maxWidth, photoH / maxHeight) : Math.min(photoW / maxWidth, photoH / maxHeight);
        if (scaleFactor < 1) {
            scaleFactor = 1;
        }
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = (int) scaleFactor;
        if (bmOptions.inSampleSize % 2 != 0) {
            int sample = 1;
            while (sample * 2 < bmOptions.inSampleSize) {
                sample *= 2;
            }
            bmOptions.inSampleSize = sample;
        }
        bmOptions.inPurgeable = Build.VERSION.SDK_INT < 21;

        String exifPath = null;
        if (path != null) {
            exifPath = path;
        } else if (uri != null) {
            exifPath = AndroidUtilities.getPath(uri);
        }

        Matrix matrix = null;

        if (exifPath != null) {
            ExifInterface exif;
            try {
                exif = new ExifInterface(exifPath);
                int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
                matrix = new Matrix();
                switch (orientation) {
                    case ExifInterface.ORIENTATION_ROTATE_90:
                        matrix.postRotate(90);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_180:
                        matrix.postRotate(180);
                        break;
                    case ExifInterface.ORIENTATION_ROTATE_270:
                        matrix.postRotate(270);
                        break;
                }
            } catch (Throwable ignore) {
                ignore.getStackTrace();
            }
        }

        Bitmap b = null;
        if (path != null) {
            try {
                b = BitmapFactory.decodeFile(path, bmOptions);
                if (b != null) {
                    if (bmOptions.inPurgeable) {
                        Utilities.pinBitmap(b);
                    }
                    Bitmap newBitmap = Bitmaps.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                    if (newBitmap != b) {
                        b.recycle();
                        b = newBitmap;
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
                GalleryImageLoader.getInstance().clearMemory();
                try {
                    if (b == null) {
                        b = BitmapFactory.decodeFile(path, bmOptions);
                        if (b != null && bmOptions.inPurgeable) {
                            Utilities.pinBitmap(b);
                        }
                    }
                    if (b != null) {
                        Bitmap newBitmap = Bitmaps.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                        if (newBitmap != b) {
                            b.recycle();
                            b = newBitmap;
                        }
                    }
                } catch (Throwable e2) {
                    FileLog.e(e2);
                }
            }
        } else if (uri != null) {
            try {
                b = BitmapFactory.decodeStream(inputStream, null, bmOptions);
                if (b != null) {
                    if (bmOptions.inPurgeable) {
                        Utilities.pinBitmap(b);
                    }
                    Bitmap newBitmap = Bitmaps.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
                    if (newBitmap != b) {
                        b.recycle();
                        b = newBitmap;
                    }
                }
            } catch (Throwable e) {
                FileLog.e(e);
            } finally {
                try {
                    inputStream.close();
                } catch (Throwable e) {
                    FileLog.e(e);
                }
            }
        }

        return b;
    }

    public static void fillPhotoSizeWithBytes(PhotoSize photoSize) {
        if (photoSize == null || photoSize.bytes != null) {
            return;
        }
        File file = FileLoader.getPathToAttach(photoSize, true);
        try {
            RandomAccessFile f = new RandomAccessFile(file, "r");
            int len = (int) f.length();
            if (len < 20000) {
                photoSize.bytes = new byte[(int) f.length()];
                f.readFully(photoSize.bytes, 0, photoSize.bytes.length);
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    private static PhotoSize scaleAndSaveImageInternal(Bitmap bitmap, int w, int h, float photoW, float photoH, float scaleFactor, int quality, boolean cache, boolean scaleAnyway) throws IOException {
        Bitmap scaledBitmap;
        if (scaleFactor > 1 || scaleAnyway) {
            scaledBitmap = Bitmaps.createScaledBitmap(bitmap, w, h, true);
        } else {
            scaledBitmap = bitmap;
        }

        FileLocation.TL_fileLocation location = new FileLocation.TL_fileLocation();
        location.volume_id = Integer.MIN_VALUE;
        location.dc_id = Integer.MIN_VALUE;
        PhotoSize size = new PhotoSize.TL_photoSize();
        size.location = location;
        size.w = scaledBitmap.getWidth();
        size.h = scaledBitmap.getHeight();
        if (size.w <= 100 && size.h <= 100) {
            size.type = "s";
        } else if (size.w <= 320 && size.h <= 320) {
            size.type = "m";
        } else if (size.w <= 800 && size.h <= 800) {
            size.type = "x";
        } else if (size.w <= 1280 && size.h <= 1280) {
            size.type = "y";
        } else {
            size.type = "w";
        }

        FileOutputStream stream = null;
        ByteArrayOutputStream stream2 = null;
        try {
            String fileName = location.volume_id + "_" + location.local_id + ".jpg";
            final File cacheFile = new File(FileLoader.getInstance().getDirectory(FileLoader.MEDIA_DIR_CACHE), fileName);
            stream = new FileOutputStream(cacheFile);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            if (cache) {
                stream2 = new ByteArrayOutputStream();
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream2);
                size.bytes = stream2.toByteArray();
                size.size = size.bytes.length;
            } else {
                size.size = (int) stream.getChannel().size();
            }
        } finally {
            if (null != stream) {
                try {
                    stream.close();
                } catch (IOException e) {
                    /*igone*/
                    e.getStackTrace();
                }
            }

            if (null != stream2) {
                try {
                    stream2.close();
                } catch (IOException e) {
                /*igone*/
                e.getStackTrace();
                }
            }
        }
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle();
        }

        return size;
    }

    public static PhotoSize scaleAndSaveImage(Bitmap bitmap, float maxWidth, float maxHeight, int quality, boolean cache) {
        return scaleAndSaveImage(bitmap, maxWidth, maxHeight, quality, cache, 0, 0);
    }

    public static PhotoSize scaleAndSaveImage(Bitmap bitmap, float maxWidth, float maxHeight, int quality, boolean cache, int minWidth, int minHeight) {
        if (bitmap == null) {
            return null;
        }
        float photoW = bitmap.getWidth();
        float photoH = bitmap.getHeight();
        if (photoW == 0 || photoH == 0) {
            return null;
        }
        boolean scaleAnyway = false;
        float scaleFactor = Math.max(photoW / maxWidth, photoH / maxHeight);
        if (minWidth != 0 && minHeight != 0 && (photoW < minWidth || photoH < minHeight)) {
            if (photoW < minWidth && photoH > minHeight) {
                scaleFactor = photoW / minWidth;
            } else if (photoW > minWidth && photoH < minHeight) {
                scaleFactor = photoH / minHeight;
            } else {
                scaleFactor = Math.max(photoW / minWidth, photoH / minHeight);
            }
            scaleAnyway = true;
        }
        int w = (int) (photoW / scaleFactor);
        int h = (int) (photoH / scaleFactor);
        if (h == 0 || w == 0) {
            return null;
        }

        try {
            return scaleAndSaveImageInternal(bitmap, w, h, photoW, photoH, scaleFactor, quality, cache, scaleAnyway);
        } catch (Throwable e) {
            FileLog.e(e);
            GalleryImageLoader.getInstance().clearMemory();
            System.gc();
            try {
                return scaleAndSaveImageInternal(bitmap, w, h, photoW, photoH, scaleFactor, quality, cache, scaleAnyway);
            } catch (Throwable e2) {
                FileLog.e(e2);
                return null;
            }
        }
    }

    public static String getHttpUrlExtension(String url, String defaultExt) {
        String ext = null;
        int idx = url.lastIndexOf('.');
        if (idx != -1) {
            ext = url.substring(idx + 1);
        }
        if (ext == null || ext.length() == 0 || ext.length() > 4) {
            ext = defaultExt;
        }
        return ext;
    }
}
