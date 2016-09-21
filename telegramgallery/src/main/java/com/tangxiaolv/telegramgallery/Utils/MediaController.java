
package com.tangxiaolv.telegramgallery.Utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.tangxiaolv.telegramgallery.Gallery;
import com.tangxiaolv.telegramgallery.R;
import com.tangxiaolv.telegramgallery.TL.Document;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

public class MediaController implements NotificationCenter.NotificationCenterDelegate {
    public static int[] readArgs = new int[3];
    private boolean saveToGallery = true;

    public interface FileDownloadProgressListener {
        void onFailedDownload(String fileName);

        void onSuccessDownload(String fileName);

        void onProgressDownload(String fileName, float progress);

        void onProgressUpload(String fileName, float progress, boolean isEncrypted);

        int getObserverTag();
    }

    private class AudioBuffer {
        public AudioBuffer(int capacity) {
            buffer = ByteBuffer.allocateDirect(capacity);
            bufferBytes = new byte[capacity];
        }

        ByteBuffer buffer;
        byte[] bufferBytes;
        int size;
        int finished;
        long pcmOffset;
    }

    private static final String[] projectionPhotos = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION
//             MediaStore.Files.FileColumns.MIME_TYPE
    };

    private static final String[] projectionVideo = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_TAKEN
    };

    public static class AlbumEntry {
        public int bucketId;
        public String bucketName;
        public PhotoEntry coverPhoto;
        public ArrayList<PhotoEntry> photos = new ArrayList<>();
        public HashMap<Integer, PhotoEntry> photosByIds = new HashMap<>();
        public boolean isVideo;

        public AlbumEntry(int bucketId, String bucketName, PhotoEntry coverPhoto, boolean isVideo) {
            this.bucketId = bucketId;
            this.bucketName = bucketName;
            this.coverPhoto = coverPhoto;
            this.isVideo = isVideo;
        }

        public void addPhoto(PhotoEntry photoEntry) {
            photos.add(photoEntry);
            photosByIds.put(photoEntry.imageId, photoEntry);
        }
    }

    public static class PhotoEntry {
        public int sortindex;
        public int bucketId;
        public int imageId;
        public long dateTaken;
        public String path;
        public int orientation;
        public String thumbPath;
        public String imagePath;
        public boolean isVideo;
        public CharSequence caption;

        public PhotoEntry(int bucketId, int imageId, long dateTaken, String path, int orientation,
                boolean isVideo) {
            this.bucketId = bucketId;
            this.imageId = imageId;
            this.dateTaken = dateTaken;
            this.path = path;
            this.orientation = orientation;
            this.isVideo = isVideo;
        }
    }

    public static class SearchImage {
        public String id;
        public String imageUrl;
        public String thumbUrl;
        public String localUrl;
        public int width;
        public int height;
        public int size;
        public int type;
        public int date;
        public String thumbPath;
        public String imagePath;
        public CharSequence caption;
        public Document document;

    }

    private HashMap<Long, Long> typingTimes = new HashMap<>();

    public static final int AUTODOWNLOAD_MASK_PHOTO = 1;
    public static final int AUTODOWNLOAD_MASK_AUDIO = 2;
    public static final int AUTODOWNLOAD_MASK_MUSIC = 16;
    public static final int AUTODOWNLOAD_MASK_GIF = 32;
    public int mobileDataDownloadMask = 0;
    public int wifiDownloadMask = 0;
    public int roamingDownloadMask = 0;

    private Runnable refreshGalleryRunnable;
    public static AlbumEntry allPhotosAlbumEntry;

    private HashMap<String, ArrayList<WeakReference<FileDownloadProgressListener>>> loadingFileObservers = new HashMap<>();
    private HashMap<Integer, String> observersByTag = new HashMap<>();
    private boolean listenerInProgress = false;
    private HashMap<String, FileDownloadProgressListener> addLaterArray = new HashMap<>();
    private ArrayList<FileDownloadProgressListener> deleteLaterArray = new ArrayList<>();

    private int playerBufferSize = 0;

    private ArrayList<AudioBuffer> freePlayerBuffers = new ArrayList<>();

    private final Object sync = new Object();

    private ArrayList<ByteBuffer> recordBuffers = new ArrayList<>();
    private int recordBufferSize;

    private class InternalObserver extends ContentObserver {
        public InternalObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            processMediaObserver(MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        }
    }

    private class ExternalObserver extends ContentObserver {
        public ExternalObserver() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            processMediaObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
    }

    private class GalleryObserverInternal extends ContentObserver {
        public GalleryObserverInternal() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (refreshGalleryRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(refreshGalleryRunnable);
            }
            AndroidUtilities.runOnUIThread(refreshGalleryRunnable = new Runnable() {
                @Override
                public void run() {
                    refreshGalleryRunnable = null;
                    loadGalleryPhotosAlbums(0, null);
                }
            }, 2000);
        }
    }

    private class GalleryObserverExternal extends ContentObserver {
        public GalleryObserverExternal() {
            super(null);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            if (refreshGalleryRunnable != null) {
                AndroidUtilities.cancelRunOnUIThread(refreshGalleryRunnable);
            }
            AndroidUtilities.runOnUIThread(refreshGalleryRunnable = new Runnable() {
                @Override
                public void run() {
                    refreshGalleryRunnable = null;
                    loadGalleryPhotosAlbums(0, null);
                }
            }, 2000);
        }
    }

    private ExternalObserver externalObserver = null;
    private InternalObserver internalObserver = null;
    private int startObserverToken = 0;

    private final class StopMediaObserverRunnable implements Runnable {
        public int currentObserverToken = 0;

        @Override
        public void run() {
            if (currentObserverToken == startObserverToken) {
                try {
                    if (internalObserver != null) {
                        Gallery.applicationContext.getContentResolver()
                                .unregisterContentObserver(internalObserver);
                        internalObserver = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                try {
                    if (externalObserver != null) {
                        Gallery.applicationContext.getContentResolver()
                                .unregisterContentObserver(externalObserver);
                        externalObserver = null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String[] mediaProjections = null;

    private static volatile MediaController Instance = null;

    public static MediaController getInstance() {
        MediaController localInstance = Instance;
        if (localInstance == null) {
            synchronized (MediaController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MediaController();
                }
            }
        }
        return localInstance;
    }

    public MediaController() {
        try {
            recordBufferSize = AudioRecord.getMinBufferSize(16000, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (recordBufferSize <= 0) {
                recordBufferSize = 1280;
            }
            playerBufferSize = AudioTrack.getMinBufferSize(48000, AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            if (playerBufferSize <= 0) {
                playerBufferSize = 3840;
            }
            for (int a = 0; a < 5; a++) {
                ByteBuffer buffer = ByteBuffer.allocateDirect(4096);
                buffer.order(ByteOrder.nativeOrder());
                recordBuffers.add(buffer);
            }
            for (int a = 0; a < 3; a++) {
                freePlayerBuffers.add(new AudioBuffer(playerBufferSize));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        SharedPreferences preferences = Gallery.applicationContext
                .getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        mobileDataDownloadMask = preferences.getInt("mobileDataDownloadMask",
                AUTODOWNLOAD_MASK_PHOTO | AUTODOWNLOAD_MASK_AUDIO | AUTODOWNLOAD_MASK_MUSIC
                        | AUTODOWNLOAD_MASK_GIF);
        wifiDownloadMask = preferences.getInt("wifiDownloadMask", AUTODOWNLOAD_MASK_PHOTO
                | AUTODOWNLOAD_MASK_AUDIO | AUTODOWNLOAD_MASK_MUSIC | AUTODOWNLOAD_MASK_GIF);
        roamingDownloadMask = preferences.getInt("roamingDownloadMask", 0);

        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance().addObserver(MediaController.this,
                        NotificationCenter.FileDidFailedLoad);
                NotificationCenter.getInstance().addObserver(MediaController.this,
                        NotificationCenter.didReceivedNewMessages);
                NotificationCenter.getInstance().addObserver(MediaController.this,
                        NotificationCenter.messagesDeleted);
                NotificationCenter.getInstance().addObserver(MediaController.this,
                        NotificationCenter.FileDidLoaded);
                NotificationCenter.getInstance().addObserver(MediaController.this,
                        NotificationCenter.FileLoadProgressChanged);
                NotificationCenter.getInstance().addObserver(MediaController.this,
                        NotificationCenter.FileUploadProgressChanged);
                NotificationCenter.getInstance().addObserver(MediaController.this,
                        NotificationCenter.removeAllMessagesFromDialog);
                NotificationCenter.getInstance().addObserver(MediaController.this,
                        NotificationCenter.musicDidLoaded);
            }
        });

        if (Build.VERSION.SDK_INT >= 16) {
            mediaProjections = new String[] {
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.TITLE,
                    MediaStore.Images.ImageColumns.WIDTH,
                    MediaStore.Images.ImageColumns.HEIGHT
            };
        } else {
            mediaProjections = new String[] {
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.TITLE
            };
        }

        try {
            Gallery.applicationContext.getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, false,
                    new GalleryObserverExternal());
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            Gallery.applicationContext.getContentResolver().registerContentObserver(
                    MediaStore.Images.Media.INTERNAL_CONTENT_URI, false,
                    new GalleryObserverInternal());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cleanup() {
        typingTimes.clear();
    }

    public void processMediaObserver(Uri uri) {
        try {
            Point size = AndroidUtilities.getRealScreenSize();

            Cursor cursor = Gallery.applicationContext.getContentResolver().query(uri,
                    mediaProjections, null, null, "date_added DESC LIMIT 1");
            final ArrayList<Long> screenshotDates = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String val = "";
                    String data = cursor.getString(0);
                    String display_name = cursor.getString(1);
                    String album_name = cursor.getString(2);
                    long date = cursor.getLong(3);
                    String title = cursor.getString(4);
                    int photoW = 0;
                    int photoH = 0;
                    if (Build.VERSION.SDK_INT >= 16) {
                        photoW = cursor.getInt(5);
                        photoH = cursor.getInt(6);
                    }
                    if (data != null && data.toLowerCase().contains("screenshot") ||
                            display_name != null
                                    && display_name.toLowerCase().contains("screenshot")
                            ||
                            album_name != null && album_name.toLowerCase().contains("screenshot") ||
                            title != null && title.toLowerCase().contains("screenshot")) {
                        try {
                            if (photoW == 0 || photoH == 0) {
                                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                                bmOptions.inJustDecodeBounds = true;
                                BitmapFactory.decodeFile(data, bmOptions);
                                photoW = bmOptions.outWidth;
                                photoH = bmOptions.outHeight;
                            }
                            if (photoW <= 0 || photoH <= 0 || (photoW == size.x && photoH == size.y
                                    || photoH == size.x && photoW == size.y)) {
                                screenshotDates.add(date);
                            }
                        } catch (Exception e) {
                            screenshotDates.add(date);
                        }
                    }
                }
                cursor.close();
            }
            if (!screenshotDates.isEmpty()) {
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance()
                                .postNotificationName(NotificationCenter.screenshotTook);
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void removeLoadingFileObserver(FileDownloadProgressListener observer) {
        if (listenerInProgress) {
            deleteLaterArray.add(observer);
            return;
        }
        String fileName = observersByTag.get(observer.getObserverTag());
        if (fileName != null) {
            ArrayList<WeakReference<FileDownloadProgressListener>> arrayList = loadingFileObservers
                    .get(fileName);
            if (arrayList != null) {
                for (int a = 0; a < arrayList.size(); a++) {
                    WeakReference<FileDownloadProgressListener> reference = arrayList.get(a);
                    if (reference.get() == null || reference.get() == observer) {
                        arrayList.remove(a);
                        a--;
                    }
                }
                if (arrayList.isEmpty()) {
                    loadingFileObservers.remove(fileName);
                }
            }
            observersByTag.remove(observer.getObserverTag());
        }
    }

    private void processLaterArrays() {
        addLaterArray.clear();
        for (FileDownloadProgressListener listener : deleteLaterArray) {
            removeLoadingFileObserver(listener);
        }
        deleteLaterArray.clear();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == NotificationCenter.FileDidFailedLoad) {
            listenerInProgress = true;
            String fileName = (String) args[0];
            ArrayList<WeakReference<FileDownloadProgressListener>> arrayList = loadingFileObservers
                    .get(fileName);
            if (arrayList != null) {
                for (int a = 0; a < arrayList.size(); a++) {
                    WeakReference<FileDownloadProgressListener> reference = arrayList.get(a);
                    if (reference.get() != null) {
                        reference.get().onFailedDownload(fileName);
                        observersByTag.remove(reference.get().getObserverTag());
                    }
                }
                loadingFileObservers.remove(fileName);
            }
            listenerInProgress = false;
            processLaterArrays();
        } else if (id == NotificationCenter.FileDidLoaded) {
            listenerInProgress = true;
            String fileName = (String) args[0];
            ArrayList<WeakReference<FileDownloadProgressListener>> arrayList = loadingFileObservers
                    .get(fileName);
            if (arrayList != null) {
                for (int a = 0; a < arrayList.size(); a++) {
                    WeakReference<FileDownloadProgressListener> reference = arrayList.get(a);
                    if (reference.get() != null) {
                        reference.get().onSuccessDownload(fileName);
                        observersByTag.remove(reference.get().getObserverTag());
                    }
                }
                loadingFileObservers.remove(fileName);
            }
            listenerInProgress = false;
            processLaterArrays();
        } else if (id == NotificationCenter.FileLoadProgressChanged) {
            listenerInProgress = true;
            String fileName = (String) args[0];
            ArrayList<WeakReference<FileDownloadProgressListener>> arrayList = loadingFileObservers
                    .get(fileName);
            if (arrayList != null) {
                Float progress = (Float) args[1];
                for (WeakReference<FileDownloadProgressListener> reference : arrayList) {
                    if (reference.get() != null) {
                        reference.get().onProgressDownload(fileName, progress);
                    }
                }
            }
            listenerInProgress = false;
            processLaterArrays();
        }
    }

    public static void loadGalleryPhotosAlbums(final int guid, final String[] filterMimiType) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<AlbumEntry> albumsSorted = new ArrayList<>();
                final ArrayList<AlbumEntry> videoAlbumsSorted = new ArrayList<>();
                HashMap<Integer, AlbumEntry> albums = new HashMap<>();
                AlbumEntry allPhotosAlbum = null;
                String cameraFolder = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        .getAbsolutePath() + "/" + "Camera/";

                // 相当于我们常用sql where 后面的写法
                StringBuilder selectionBuilder = null;
                if (filterMimiType != null && filterMimiType.length > 0) {
                    selectionBuilder = new StringBuilder();
                    int length = filterMimiType.length;
                    for (int i = 0; i < length; i++) {
                        String mimeType = MediaStore.Files.FileColumns.MIME_TYPE;
                        if (0 == i) {
                            selectionBuilder.append(mimeType).append(" !=?");
                        } else {
                            selectionBuilder.append(" and ").append(mimeType).append(" !=?");
                        }
                    }
                }
                String selection = selectionBuilder == null ? null : selectionBuilder.toString();

                Integer cameraAlbumId = null;
                Integer cameraAlbumVideoId = null;

                Cursor cursor = null;
                try {
                    cursor = MediaStore.Images.Media.query(
                            Gallery.applicationContext.getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projectionPhotos,
                            selection, filterMimiType,
                            MediaStore.Images.Media.DATE_TAKEN + " DESC");
                    if (cursor != null) {
                        int imageIdColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                        int bucketIdColumn = cursor
                                .getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
                        int bucketNameColumn = cursor
                                .getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                        int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                        int dateColumn = cursor
                                .getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                        int orientationColumn = cursor
                                .getColumnIndex(MediaStore.Images.Media.ORIENTATION);
//                         int mimeTypeColumn = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE);

                        while (cursor.moveToNext()) {
                            int imageId = cursor.getInt(imageIdColumn);
                            int bucketId = cursor.getInt(bucketIdColumn);
                            String bucketName = cursor.getString(bucketNameColumn);
                            String path = cursor.getString(dataColumn);
                            long dateTaken = cursor.getLong(dateColumn);
                            int orientation = cursor.getInt(orientationColumn);
//                             String mimeType = cursor.getString(mimeTypeColumn);

                            if (path == null || path.length() == 0) {
                                continue;
                            }

                            PhotoEntry photoEntry = new PhotoEntry(bucketId, imageId, dateTaken,
                                    path, orientation, false);

                            if (allPhotosAlbum == null) {
                                allPhotosAlbum = new AlbumEntry(0, LocaleController.getString(
                                        "AllPhotos", R.string.AllPhotos), photoEntry, false);
                                albumsSorted.add(0, allPhotosAlbum);
                            }
                            if (allPhotosAlbum != null) {
                                allPhotosAlbum.addPhoto(photoEntry);
                            }

                            AlbumEntry albumEntry = albums.get(bucketId);
                            if (albumEntry == null) {
                                albumEntry = new AlbumEntry(bucketId, bucketName, photoEntry,
                                        false);
                                albums.put(bucketId, albumEntry);
                                if (cameraAlbumId == null && path.startsWith(cameraFolder)) {
                                    if (albumsSorted.size() >= 2) {
                                        albumsSorted.add(1, albumEntry);
                                    } else {
                                        albumsSorted.add(albumEntry);
                                    }
                                    cameraAlbumId = bucketId;
                                } else {
                                    albumsSorted.add(albumEntry);
                                }
                            }

                            albumEntry.addPhoto(photoEntry);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                try {
                    albums.clear();
                    cursor = null;
                    AlbumEntry allVideosAlbum = null;
                     cursor = MediaStore.Images.Media.query(
                     Gallery.applicationContext.getContentResolver(),
                     MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projectionVideo, null,
                     null, MediaStore.Video.Media.DATE_TAKEN + " DESC");
                    if (cursor != null) {
                        int imageIdColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                        int bucketIdColumn = cursor
                                .getColumnIndex(MediaStore.Video.Media.BUCKET_ID);
                        int bucketNameColumn = cursor
                                .getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                        int dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
                        int dateColumn = cursor
                                .getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);

                        while (cursor.moveToNext()) {
                            int imageId = cursor.getInt(imageIdColumn);
                            int bucketId = cursor.getInt(bucketIdColumn);
                            String bucketName = cursor.getString(bucketNameColumn);
                            String path = cursor.getString(dataColumn);
                            long dateTaken = cursor.getLong(dateColumn);

                            if (path == null || path.length() == 0) {
                                continue;
                            }

                            PhotoEntry photoEntry = new PhotoEntry(bucketId, imageId, dateTaken,
                                    path, 0, true);

                            if (allVideosAlbum == null) {
                                allVideosAlbum = new AlbumEntry(0, LocaleController.getString(
                                        "AllVideo", R.string.AllVideo), photoEntry, true);
                                videoAlbumsSorted.add(0, allVideosAlbum);
                            }
                            if (allVideosAlbum != null) {
                                allVideosAlbum.addPhoto(photoEntry);
                            }

                            AlbumEntry albumEntry = albums.get(bucketId);
                            if (albumEntry == null) {
                                albumEntry = new AlbumEntry(bucketId, bucketName, photoEntry,
                                        true);
                                albums.put(bucketId, albumEntry);
                                if (cameraAlbumVideoId == null && cameraFolder != null
                                        && path != null && path.startsWith(cameraFolder)) {
                                    videoAlbumsSorted.add(0, albumEntry);
                                    cameraAlbumVideoId = bucketId;
                                } else {
                                    videoAlbumsSorted.add(albumEntry);
                                }
                            }

                            albumEntry.addPhoto(photoEntry);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    if (cursor != null) {
                        try {
                            cursor.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

                final Integer cameraAlbumIdFinal = cameraAlbumId;
                final Integer cameraAlbumVideoIdFinal = cameraAlbumVideoId;
                final AlbumEntry allPhotosAlbumFinal = allPhotosAlbum;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        allPhotosAlbumEntry = allPhotosAlbumFinal;
                        NotificationCenter.getInstance().postNotificationName(
                                NotificationCenter.albumsDidLoaded, guid, albumsSorted,
                                cameraAlbumIdFinal, videoAlbumsSorted, cameraAlbumVideoIdFinal);
                    }
                });
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    @SuppressLint("NewApi")
    public static MediaCodecInfo selectCodec(String mimeType) {
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo lastCodecInfo = null;
        for (int i = 0; i < numCodecs; i++) {
            MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {
                continue;
            }
            String[] types = codecInfo.getSupportedTypes();
            for (String type : types) {
                if (type.equalsIgnoreCase(mimeType)) {
                    lastCodecInfo = codecInfo;
                    if (!lastCodecInfo.getName().equals("OMX.SEC.avc.enc")) {
                        return lastCodecInfo;
                    } else if (lastCodecInfo.getName().equals("OMX.SEC.AVC.Encoder")) {
                        return lastCodecInfo;
                    }
                }
            }
        }
        return lastCodecInfo;
    }

    public boolean canSaveToGallery() {
        return saveToGallery;
    }

    public void checkSaveToGalleryFiles() {
        try {
            File telegramPath = new File(Environment.getExternalStorageDirectory(), "Telegram");
            File imagePath = new File(telegramPath, "Telegram Images");
            imagePath.mkdir();
            File videoPath = new File(telegramPath, "Telegram Video");
            videoPath.mkdir();

            if (saveToGallery) {
                if (imagePath.isDirectory()) {
                    new File(imagePath, ".nomedia").delete();
                }
                if (videoPath.isDirectory()) {
                    new File(videoPath, ".nomedia").delete();
                }
            } else {
                if (imagePath.isDirectory()) {
                    new File(imagePath, ".nomedia").createNewFile();
                }
                if (videoPath.isDirectory()) {
                    new File(videoPath, ".nomedia").createNewFile();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
