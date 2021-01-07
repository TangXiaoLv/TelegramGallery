package com.tangxiaolv.telegramgallery.utils;

import android.app.Activity;
import android.content.SharedPreferences;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.ArrayMap;

import com.tangxiaolv.telegramgallery.Gallery;
import com.tangxiaolv.telegramgallery.R;

import java.io.Closeable;
import java.io.File;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

import static com.tangxiaolv.telegramgallery.GalleryActivity.getConfig;

public class MediaController implements NotificationCenter.NotificationCenterDelegate {
    private boolean saveToGallery = true;

    public interface FileDownloadProgressListener {
        void onFailedDownload(String fileName);

        void onSuccessDownload(String fileName);

        void onProgressDownload(String fileName, float progress);

        int getObserverTag();
    }

    private static final String[] projectionPhotos = {
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.ORIENTATION,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.MIME_TYPE,
    };

    private static final String[] projectionVideo = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DATE_TAKEN,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
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

    public static class PhotoEntry implements Comparator<PhotoEntry>, Serializable {
        public int bucketId;
        public int imageId;
        public long dateTaken;
        public long size;
        public String path;
        public String mimeType;
        public int orientation;
        public String thumbPath;
        public String imagePath;
        public boolean isVideo;
        public CharSequence caption;
        public int sortindex;
        public int duration;//sec
        public String title;

        public PhotoEntry(int bucketId,
                          int imageId,
                          long dateTaken,
                          long size,
                          String path,
                          String mimeType,
                          int orientation,
                          boolean isVideo,
                          String title
        ) {
            this.bucketId = bucketId;
            this.imageId = imageId;
            this.dateTaken = dateTaken;
            this.size = size;
            this.path = path;
            this.mimeType = mimeType;
            this.title = title;
            if (isVideo) {
                this.duration = orientation;
            } else {
                this.orientation = 0;
            }
            this.isVideo = isVideo;
        }

        @Override
        public int compare(PhotoEntry o1, PhotoEntry o2) {
            if (o1.sortindex > o2.sortindex) {
                return 1;
            } else if (o1.sortindex == o2.sortindex) {
                return 0;
            } else {
                return -1;
            }
        }
    }

    public static final int AUTODOWNLOAD_MASK_PHOTO = 1;
    public static final int AUTODOWNLOAD_MASK_AUDIO = 2;
    public static final int AUTODOWNLOAD_MASK_MUSIC = 16;
    public static final int AUTODOWNLOAD_MASK_GIF = 32;
    public int mobileDataDownloadMask = 0;
    public int wifiDownloadMask = 0;
    public int roamingDownloadMask = 0;

    private Runnable refreshGalleryRunnable;

    private HashMap<String, ArrayList<WeakReference<FileDownloadProgressListener>>> loadingFileObservers = new HashMap<>();
    private HashMap<Integer, String> observersByTag = new HashMap<>();
    private boolean listenerInProgress = false;
    private ArrayList<FileDownloadProgressListener> deleteLaterArray = new ArrayList<>();

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
        SharedPreferences preferences = Gallery.applicationContext
                .getSharedPreferences("mainconfig", Activity.MODE_PRIVATE);
        mobileDataDownloadMask = preferences.getInt("mobileDataDownloadMask",
                AUTODOWNLOAD_MASK_PHOTO | AUTODOWNLOAD_MASK_AUDIO | AUTODOWNLOAD_MASK_MUSIC
                        | AUTODOWNLOAD_MASK_GIF);
        wifiDownloadMask = preferences.getInt("wifiDownloadMask", AUTODOWNLOAD_MASK_PHOTO
                | AUTODOWNLOAD_MASK_AUDIO | AUTODOWNLOAD_MASK_MUSIC | AUTODOWNLOAD_MASK_GIF);
        roamingDownloadMask = preferences.getInt("roamingDownloadMask", 0);

        if (Build.VERSION.SDK_INT >= 16) {
            mediaProjections = new String[]{
                    MediaStore.Images.ImageColumns.DATA,
                    MediaStore.Images.ImageColumns.DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.ImageColumns.DATE_TAKEN,
                    MediaStore.Images.ImageColumns.TITLE,
                    MediaStore.Images.ImageColumns.WIDTH,
                    MediaStore.Images.ImageColumns.HEIGHT
            };
        } else {
            mediaProjections = new String[]{
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

    public void cleanUp() {
    }

    public void processMediaObserver(Uri uri) {
        try {
            Point size = AndroidUtilities.getRealScreenSize();

            Cursor cursor = Gallery.applicationContext.getContentResolver().query(uri,
                    mediaProjections, null, null, "date_added DESC LIMIT 1");
            final ArrayList<Long> screenshotDates = new ArrayList<>();
            if (cursor != null) {
                while (cursor.moveToNext()) {
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

    public static void loadGalleryPhotosAlbums(final int guid, final String[] filterMimiTypes) {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                final ArrayList<AlbumEntry> mediaAlbumsSorted = new ArrayList<>();
                HashMap<Integer, AlbumEntry> albums = new HashMap<>();
                AlbumEntry allPhotoAlbum = null;
                AlbumEntry allVideosAlbum = null;
                String cameraFolder = Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
                        .getAbsolutePath() + "/" + "Camera/";

                // 相当于我们常用sql where 后面的写法
                String selection = "";
                int length = 0;
                StringBuilder selectionBuilder = new StringBuilder();
                if (filterMimiTypes != null && (length = filterMimiTypes.length) > 0) {
                    selectionBuilder.append(MediaStore.Files.FileColumns.MIME_TYPE);
                    selectionBuilder.append(" in (");
                    for (int i = 0; i < length; i++) {
                        String mimeType = filterMimiTypes[i];
                        if (mimeType.contains("image")) {
                            selectionBuilder.append("'");
                            selectionBuilder.append(mimeType);
                            selectionBuilder.append("'").append(",");
                        }
                    }

                    selectionBuilder.append(")");
                    int index = selectionBuilder.lastIndexOf(",");
                    if (index != -1) {
                        selection = selectionBuilder.deleteCharAt(index).toString();
                    }
                }

                Integer cameraAlbumId = null;

                //加载图片

                Cursor cursor = null;
                try {
                    cursor = MediaStore.Images.Media.query(
                            Gallery.applicationContext.getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projectionPhotos,
                            selection,
                            MediaStore.Images.Media.DATE_TAKEN + " DESC");
                    if (cursor != null) {
                        int imageIdColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                        int bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID);
                        int bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                        int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                        int dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                        int orientationColumn = cursor.getColumnIndex(MediaStore.Images.Media.ORIENTATION);
                        int imageSize = cursor.getColumnIndex(MediaStore.Images.Media.SIZE);
                        int mimeType = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE);

                        while (cursor.moveToNext()) {
                            int imageId = cursor.getInt(imageIdColumn);
                            int bucketId = cursor.getInt(bucketIdColumn);
                            String bucketName = cursor.getString(bucketNameColumn);
                            String path = cursor.getString(dataColumn);
                            long dateTaken = cursor.getLong(dateColumn);
                            int orientation = cursor.getInt(orientationColumn);
                            int size = cursor.getInt(imageSize);
                            String type = cursor.getString(mimeType);

                            if (path == null || size == 0 || !new File(path).exists()) {
                                continue;
                            }

                            PhotoEntry photoEntry = new PhotoEntry(bucketId, imageId, dateTaken,
                                    size, path, type, orientation, false, "");

                            if (allPhotoAlbum == null) {
                                allPhotoAlbum = new AlbumEntry(0, LocaleController.getString(
                                        "AllPhotos", R.string.AllPhotos), photoEntry, false);
                                mediaAlbumsSorted.add(0, allPhotoAlbum);
                            }
                            allPhotoAlbum.addPhoto(photoEntry);

                            AlbumEntry albumEntry = albums.get(bucketName.hashCode());
                            if (albumEntry == null) {
                                albumEntry = new AlbumEntry(bucketId, bucketName, photoEntry, false);
                                albums.put(bucketName.hashCode(), albumEntry);
                                if (cameraAlbumId == null && path.startsWith(cameraFolder)) {
                                    if (mediaAlbumsSorted.size() >= 2) {
                                        mediaAlbumsSorted.add(1, albumEntry);
                                    } else {
                                        mediaAlbumsSorted.add(albumEntry);
                                    }
                                    cameraAlbumId = bucketId;
                                } else {
                                    mediaAlbumsSorted.add(albumEntry);
                                }
                            }

                            albumEntry.addPhoto(photoEntry);
                        }
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                } finally {
                    close(cursor);
                }

                //加载video
                try {
                    if (!getConfig().hasVideo()) {
                        throw new Exception("igone");
                    }

                    // 相当于我们常用sql where 后面的写法
                    selection = "";
                    selectionBuilder = new StringBuilder();
                    if (length > 0) {
                        selectionBuilder.append(MediaStore.Files.FileColumns.MIME_TYPE);
                        selectionBuilder.append(" in (");
                        for (int i = 0; i < length; i++) {
                            String mimeType = filterMimiTypes[i];
                            if (mimeType.contains("video")) {
                                selectionBuilder.append("'");
                                selectionBuilder.append(mimeType);
                                selectionBuilder.append("'").append(",");
                            }
                        }

                        selectionBuilder.append(")");
                        int index = selectionBuilder.lastIndexOf(",");
                        if (index != -1) {
                            selection = selectionBuilder.deleteCharAt(index).toString();
                        }
                    }

                    cursor = MediaStore.Images.Media.query(
                            Gallery.applicationContext.getContentResolver(),
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            projectionVideo,
                            selection,
                            MediaStore.Video.Media.DATE_TAKEN + " DESC");
                    if (cursor != null) {
                        int imageIdColumn = cursor.getColumnIndex(MediaStore.Video.Media._ID);
                        int bucketIdColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID);
                        int bucketNameColumn = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME);
                        int dataColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATA);
                        int dateColumn = cursor.getColumnIndex(MediaStore.Video.Media.DATE_TAKEN);
                        int durationColumn = cursor.getColumnIndex(MediaStore.Video.Media.DURATION);
                        int mimeTypeColumn = cursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE);
                        int titleColumn = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME);
                        int sizeColumn = cursor.getColumnIndex(MediaStore.Video.Media.SIZE);

                        while (cursor.moveToNext()) {
                            int imageId = cursor.getInt(imageIdColumn);
                            int bucketId = cursor.getInt(bucketIdColumn);
                            String bucketName = cursor.getString(bucketNameColumn);
                            String path = cursor.getString(dataColumn);
                            long dateTaken = cursor.getLong(dateColumn);
                            long duration = cursor.getLong(durationColumn);
                            String type = cursor.getString(mimeTypeColumn);
                            String title = cursor.getString(titleColumn);
                            long size = cursor.getLong(sizeColumn);

                            if (path == null || path.length() == 0 || duration == 0) {
                                continue;
                            }

                            PhotoEntry photoEntry = new PhotoEntry(bucketId, imageId, dateTaken,
                                    size, path, type, (int)(Math.ceil(duration / 1000d)), true, title);

                            if (allVideosAlbum == null) {
                                allVideosAlbum = new AlbumEntry(1, LocaleController.getString(
                                        "AllVideo", R.string.AllVideo), photoEntry, false);
                                mediaAlbumsSorted.add(1, allVideosAlbum);
                            }

                            allVideosAlbum.addPhoto(photoEntry);
                            /*AlbumEntry albumEntry = albums.get(bucketName.hashCode());
                            if (albumEntry == null) {
                                albumEntry = new AlbumEntry(bucketId, bucketName, photoEntry, false);
                                albums.put(bucketName.hashCode(), albumEntry);
                                if (cameraAlbumId == null && path.startsWith(cameraFolder)) {
                                    if (mediaAlbumsSorted.size() >= 2) {
                                        mediaAlbumsSorted.add(2, albumEntry);
                                    } else {
                                        mediaAlbumsSorted.add(albumEntry);
                                    }
                                    cameraAlbumId = bucketId;
                                } else {
                                    mediaAlbumsSorted.add(albumEntry);
                                }
                            }

                            albumEntry.addPhoto(photoEntry);*/
                        }
                    }
                } catch (Throwable e) {
                    //igone
                    e.getStackTrace();
                } finally {
                    close(cursor);
                }

                final Integer cameraAlbumIdFinal = cameraAlbumId;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(
                                NotificationCenter.albumsDidLoaded, guid, mediaAlbumsSorted,
                                cameraAlbumIdFinal, null, cameraAlbumIdFinal);
                    }
                });
            }
        });
        thread.setPriority(Thread.MIN_PRIORITY);
        thread.start();
    }

    private static void close(Cursor closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean canSaveToGallery() {
        return saveToGallery;
    }

    public void checkSaveToGalleryFiles() {
        try {
            File telegramPath = new File(Environment.getExternalStorageDirectory(), "toon");
            File imagePath = new File(telegramPath, "toon Images");
            imagePath.mkdir();
            File videoPath = new File(telegramPath, "toon Video");
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
