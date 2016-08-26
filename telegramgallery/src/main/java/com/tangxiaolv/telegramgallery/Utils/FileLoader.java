package com.tangxiaolv.telegramgallery.Utils;

import com.tangxiaolv.telegramgallery.DispatchQueue;
import com.tangxiaolv.telegramgallery.TL.Document;
import com.tangxiaolv.telegramgallery.TL.DocumentAttribute;
import com.tangxiaolv.telegramgallery.TL.FileLocation;
import com.tangxiaolv.telegramgallery.TL.PhotoSize;
import com.tangxiaolv.telegramgallery.TL.TLObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class FileLoader {

    public interface FileLoaderDelegate {
        void fileDidLoaded(String location, File finalFile, int type);

        void fileDidFailedLoad(String location, int state);

        void fileLoadProgressChanged(String location, float progress);
    }

    public static final int MEDIA_DIR_IMAGE = 0;
    public static final int MEDIA_DIR_AUDIO = 1;
    public static final int MEDIA_DIR_VIDEO = 2;
    public static final int MEDIA_DIR_DOCUMENT = 3;
    public static final int MEDIA_DIR_CACHE = 4;

    private HashMap<Integer, File> mediaDirs = null;
    private volatile DispatchQueue fileLoaderQueue = new DispatchQueue("fileUploadQueue");

    private LinkedList<FileLoadOperation> loadOperationQueue = new LinkedList<>();
    private LinkedList<FileLoadOperation> audioLoadOperationQueue = new LinkedList<>();
    private LinkedList<FileLoadOperation> photoLoadOperationQueue = new LinkedList<>();
    private ConcurrentHashMap<String, FileLoadOperation> loadOperationPaths = new ConcurrentHashMap<>();
    private HashMap<String, Long> uploadSizes = new HashMap<>();

    private FileLoaderDelegate delegate = null;

    private int currentLoadOperationsCount = 0;
    private int currentAudioLoadOperationsCount = 0;
    private int currentPhotoLoadOperationsCount = 0;
    private int currentUploadOperationsCount = 0;
    private int currentUploadSmallOperationsCount = 0;

    private static volatile FileLoader Instance = null;

    public static FileLoader getInstance() {
        FileLoader localInstance = Instance;
        if (localInstance == null) {
            synchronized (FileLoader.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new FileLoader();
                }
            }
        }
        return localInstance;
    }

    public void setMediaDirs(HashMap<Integer, File> dirs) {
        mediaDirs = dirs;
    }

    public File checkDirectory(int type) {
        return mediaDirs.get(type);
    }

    public File getDirectory(int type) {
        File dir = mediaDirs.get(type);
        if (dir == null && type != MEDIA_DIR_CACHE) {
            dir = mediaDirs.get(MEDIA_DIR_CACHE);
        }
        try {
            if (!dir.isDirectory()) {
                dir.mkdirs();
            }
        } catch (Exception e) {
            // don't promt
        }
        return dir;
    }

    public void cancelLoadFile(Document document) {
        cancelLoadFile(document, null, null);
    }

    public void cancelLoadFile(PhotoSize photo) {
        cancelLoadFile(null, photo.location, null);
    }

    public void cancelLoadFile(FileLocation location, String ext) {
        cancelLoadFile(null, location, ext);
    }

    private void cancelLoadFile(final Document document, final FileLocation location,
            final String locationExt) {
        if (location == null && document == null) {
            return;
        }
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String fileName = null;
                if (location != null) {
                    fileName = getAttachFileName(location, locationExt);
                } else if (document != null) {
                    fileName = getAttachFileName(document);
                }
                if (fileName == null) {
                    return;
                }
                FileLoadOperation operation = loadOperationPaths.remove(fileName);
                if (operation != null) {
                    if (location != null) {
                        photoLoadOperationQueue.remove(operation);
                    } else {
                        loadOperationQueue.remove(operation);
                    }
                    operation.cancel();
                }
            }
        });
    }

    public boolean isLoadingFile(final String fileName) {
        final Semaphore semaphore = new Semaphore(0);
        final Boolean[] result = new Boolean[1];
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                result[0] = loadOperationPaths.containsKey(fileName);
                semaphore.release();
            }
        });
        try {
            semaphore.acquire();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result[0];
    }

    public void loadFile(PhotoSize photo, String ext, boolean cacheOnly) {
        loadFile(null, photo.location, ext, photo.size, false,
                cacheOnly || (photo != null && photo.size == 0 || photo.location.key != null));
    }

    public void loadFile(Document document, boolean force, boolean cacheOnly) {
        loadFile(document, null, null, 0, force,
                cacheOnly || document != null && document.key != null);
    }

    public void loadFile(FileLocation location, String ext, int size, boolean cacheOnly) {
        loadFile(null, location, ext, size, true,
                cacheOnly || size == 0 || (location != null && location.key != null));
    }

    private void loadFile(final Document document, final FileLocation location,
            final String locationExt, final int locationSize, final boolean force,
            final boolean cacheOnly) {
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                String fileName = null;
                if (location != null) {
                    fileName = getAttachFileName(location, locationExt);
                } else if (document != null) {
                    fileName = getAttachFileName(document);
                }
                if (fileName == null || fileName.contains("" + Integer.MIN_VALUE)) {
                    return;
                }

                FileLoadOperation operation;
                operation = loadOperationPaths.get(fileName);
                if (operation != null) {
                    if (force) {
                        LinkedList<FileLoadOperation> downloadQueue;
                        if (location != null) {
                            downloadQueue = photoLoadOperationQueue;
                        } else {
                            downloadQueue = loadOperationQueue;
                        }
                        if (downloadQueue != null) {
                            int index = downloadQueue.indexOf(operation);
                            if (index != -1) {
                                downloadQueue.remove(index);
                                downloadQueue.add(0, operation);
                                operation.setForceRequest(true);
                            }
                        }
                    }
                    return;
                }

                File tempDir = getDirectory(MEDIA_DIR_CACHE);
                File storeDir = tempDir;
                int type = MEDIA_DIR_CACHE;

                if (location != null) {
                    operation = new FileLoadOperation(location, locationExt, locationSize);
                    type = MEDIA_DIR_IMAGE;
                } else if (document != null) {
                    operation = new FileLoadOperation(document);
                }
                if (!cacheOnly) {
                    storeDir = getDirectory(type);
                }
                operation.setPaths(storeDir, tempDir);

                final String finalFileName = fileName;
                final int finalType = type;
                loadOperationPaths.put(fileName, operation);
                operation.setDelegate(new FileLoadOperation.FileLoadOperationDelegate() {
                    @Override
                    public void didFinishLoadingFile(FileLoadOperation operation, File finalFile) {
                        if (delegate != null) {
                            delegate.fileDidLoaded(finalFileName, finalFile, finalType);
                        }
                        checkDownloadQueue(document, location, finalFileName);
                    }

                    @Override
                    public void didFailedLoadingFile(FileLoadOperation operation, int canceled) {
                        checkDownloadQueue(document, location, finalFileName);
                        if (delegate != null) {
                            delegate.fileDidFailedLoad(finalFileName, canceled);
                        }
                    }

                    @Override
                    public void didChangedLoadProgress(FileLoadOperation operation,
                            float progress) {
                        if (delegate != null) {
                            delegate.fileLoadProgressChanged(finalFileName, progress);
                        }
                    }
                });
                int maxCount = force ? 3 : 1;
                if (type == MEDIA_DIR_AUDIO) {
                    if (currentAudioLoadOperationsCount < maxCount) {
                        currentAudioLoadOperationsCount++;
                        operation.start();
                    } else {
                        if (force) {
                            audioLoadOperationQueue.add(0, operation);
                        } else {
                            audioLoadOperationQueue.add(operation);
                        }
                    }
                } else if (location != null) {
                    if (currentPhotoLoadOperationsCount < maxCount) {
                        currentPhotoLoadOperationsCount++;
                        operation.start();
                    } else {
                        if (force) {
                            photoLoadOperationQueue.add(0, operation);
                        } else {
                            photoLoadOperationQueue.add(operation);
                        }
                    }
                } else {
                    if (currentLoadOperationsCount < maxCount) {
                        currentLoadOperationsCount++;
                        operation.start();
                    } else {
                        if (force) {
                            loadOperationQueue.add(0, operation);
                        } else {
                            loadOperationQueue.add(operation);
                        }
                    }
                }
            }
        });
    }

    private void checkDownloadQueue(final Document document, final FileLocation location,
            final String arg1) {
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                loadOperationPaths.remove(arg1);
                FileLoadOperation operation;
                if (location != null) {
                    currentPhotoLoadOperationsCount--;
                    if (!photoLoadOperationQueue.isEmpty()) {
                        operation = photoLoadOperationQueue.get(0);
                        int maxCount = operation.isForceRequest() ? 3 : 1;
                        if (currentPhotoLoadOperationsCount < maxCount) {
                            operation = photoLoadOperationQueue.poll();
                            if (operation != null) {
                                currentPhotoLoadOperationsCount++;
                                operation.start();
                            }
                        }
                    }
                } else {
                    currentLoadOperationsCount--;
                    if (!loadOperationQueue.isEmpty()) {
                        operation = loadOperationQueue.get(0);
                        int maxCount = operation.isForceRequest() ? 3 : 1;
                        if (currentLoadOperationsCount < maxCount) {
                            operation = loadOperationQueue.poll();
                            if (operation != null) {
                                currentLoadOperationsCount++;
                                operation.start();
                            }
                        }
                    }
                }
            }
        });
    }

    public void setDelegate(FileLoaderDelegate delegate) {
        this.delegate = delegate;
    }

    public static File getPathToAttach(TLObject attach) {
        return getPathToAttach(attach, null, false);
    }

    public static File getPathToAttach(TLObject attach, boolean forceCache) {
        return getPathToAttach(attach, null, forceCache);
    }

    public static File getPathToAttach(TLObject attach, String ext, boolean forceCache) {
        File dir = null;
        if (forceCache) {
            dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
        } else {
            if (attach instanceof Document) {
                Document document = (Document) attach;
                if (document.key != null) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_DOCUMENT);
                }
            } else if (attach instanceof PhotoSize) {
                PhotoSize photoSize = (PhotoSize) attach;
                if (photoSize.location == null || photoSize.location.key != null
                        || photoSize.location.volume_id == Integer.MIN_VALUE
                                && photoSize.location.local_id < 0
                        || photoSize.size < 0) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_IMAGE);
                }
            } else if (attach instanceof FileLocation) {
                FileLocation fileLocation = (FileLocation) attach;
                if (fileLocation.key != null || fileLocation.volume_id == Integer.MIN_VALUE
                        && fileLocation.local_id < 0) {
                    dir = getInstance().getDirectory(MEDIA_DIR_CACHE);
                } else {
                    dir = getInstance().getDirectory(MEDIA_DIR_IMAGE);
                }
            }
        }
        if (dir == null) {
            return new File("");
        }
        return new File(dir, getAttachFileName(attach, ext));
    }

    public static PhotoSize getClosestPhotoSizeWithSize(ArrayList<PhotoSize> sizes, int side) {
        return getClosestPhotoSizeWithSize(sizes, side, false);
    }

    public static PhotoSize getClosestPhotoSizeWithSize(ArrayList<PhotoSize> sizes, int side,
            boolean byMinSide) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        int lastSide = 0;
        PhotoSize closestObject = null;
        for (int a = 0; a < sizes.size(); a++) {
            PhotoSize obj = sizes.get(a);
            if (obj == null) {
                continue;
            }
            if (byMinSide) {
                int currentSide = obj.h >= obj.w ? obj.w : obj.h;
                if (closestObject == null
                        || side > 100 && closestObject.location != null
                                && closestObject.location.dc_id == Integer.MIN_VALUE
                        || obj instanceof PhotoSize.TL_photoCachedSize
                        || side > lastSide && lastSide < currentSide) {
                    closestObject = obj;
                    lastSide = currentSide;
                }
            } else {
                int currentSide = obj.w >= obj.h ? obj.w : obj.h;
                if (closestObject == null
                        || side > 100 && closestObject.location != null
                                && closestObject.location.dc_id == Integer.MIN_VALUE
                        || obj instanceof PhotoSize.TL_photoCachedSize
                        || currentSide <= side && lastSide < currentSide) {
                    closestObject = obj;
                    lastSide = currentSide;
                }
            }
        }
        return closestObject;
    }

    public static String getFileExtension(File file) {
        String name = file.getName();
        try {
            return name.substring(name.lastIndexOf('.') + 1);
        } catch (Exception e) {
            return "";
        }
    }

    public static String getDocumentFileName(Document document) {
        if (document != null) {
            if (document.file_name != null) {
                return document.file_name;
            }
            for (int a = 0; a < document.attributes.size(); a++) {
                DocumentAttribute documentAttribute = document.attributes.get(a);
                if (documentAttribute instanceof DocumentAttribute.TL_documentAttributeFilename) {
                    return documentAttribute.file_name;
                }
            }
        }
        return "";
    }

    public static String getDocumentExtension(Document document) {
        String fileName = getDocumentFileName(document);
        int idx = fileName.lastIndexOf('.');
        String ext = null;
        if (idx != -1) {
            ext = fileName.substring(idx + 1);
        }
        if (ext == null || ext.length() == 0) {
            ext = document.mime_type;
        }
        if (ext == null) {
            ext = "";
        }
        ext = ext.toUpperCase();
        return ext;
    }

    public static String getAttachFileName(TLObject attach) {
        return getAttachFileName(attach, null);
    }

    public static String getAttachFileName(TLObject attach, String ext) {
        if (attach instanceof Document) {
            Document document = (Document) attach;
            String docExt = null;
            if (docExt == null) {
                docExt = getDocumentFileName(document);
                int idx;
                if (docExt == null || (idx = docExt.lastIndexOf('.')) == -1) {
                    docExt = "";
                } else {
                    docExt = docExt.substring(idx);
                }
            }
            if (docExt.length() <= 1) {
                if (document.mime_type != null) {
                    switch (document.mime_type) {
                        case "video/mp4":
                            docExt = ".mp4";
                            break;
                        case "audio/ogg":
                            docExt = ".ogg";
                            break;
                        default:
                            docExt = "";
                            break;
                    }
                } else {
                    docExt = "";
                }
            }
            if (docExt.length() > 1) {
                return document.dc_id + "_" + document.id + docExt;
            } else {
                return document.dc_id + "_" + document.id;
            }
        } else if (attach instanceof PhotoSize) {
            PhotoSize photo = (PhotoSize) attach;
            if (photo.location == null || photo.location instanceof FileLocation.TL_fileLocationUnavailable) {
                return "";
            }
            return photo.location.volume_id + "_" + photo.location.local_id + "."
                    + (ext != null ? ext : "jpg");
        } else if (attach instanceof FileLocation) {
            if (attach instanceof FileLocation.TL_fileLocationUnavailable) {
                return "";
            }
            FileLocation location = (FileLocation) attach;
            return location.volume_id + "_" + location.local_id + "." + (ext != null ? ext : "jpg");
        }
        return "";
    }

    public void deleteFiles(final ArrayList<File> files, final int type) {
        if (files == null || files.isEmpty()) {
            return;
        }
        fileLoaderQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                for (int a = 0; a < files.size(); a++) {
                    File file = files.get(a);
                    if (file.exists()) {
                        try {
                            if (!file.delete()) {
                                file.deleteOnExit();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        File qFile = new File(file.getParentFile(), "q_" + file.getName());
                        if (qFile.exists()) {
                            if (!qFile.delete()) {
                                qFile.deleteOnExit();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (type == 2) {
                    ImageLoader.getInstance().clearMemory();
                }
            }
        });
    }
}
