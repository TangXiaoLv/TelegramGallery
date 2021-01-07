package com.tangxiaolv.telegramgallery.utils;

import com.tangxiaolv.telegramgallery.tl.Document;
import com.tangxiaolv.telegramgallery.tl.FileLocation;
import com.tangxiaolv.telegramgallery.tl.InputFileLocation;
import com.tangxiaolv.telegramgallery.tl.TL_upload_file;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class FileLoadOperation {

    private static class RequestInfo {
        private int requestToken;
        private int offset;
        private TL_upload_file response;
    }

    private final static int stateIdle = 0;
    private final static int stateDownloading = 1;
    private final static int stateFailed = 2;
    private final static int stateFinished = 3;

    private final static int downloadChunkSize = 1024 * 32;
    private final static int downloadChunkSizeBig = 1024 * 128;
    private final static int maxDownloadRequests = 4;
    private final static int maxDownloadRequestsBig = 2;
    private final static int bigFileSizeFrom = 1024 * 1024;

    private int datacenter_id;
    private InputFileLocation location;
    private volatile int state = stateIdle;
    private int downloadedBytes;
    private int totalBytesCount;
    private int bytesCountPadding;
    private FileLoadOperationDelegate delegate;
    private byte[] key;
    private byte[] iv;
    private int currentDownloadChunkSize;
    private int currentMaxDownloadRequests;
    private int requestsCount;
    private int renameRetryCount;

    private int nextDownloadOffset;
    private ArrayList<RequestInfo> requestInfos;
    private ArrayList<RequestInfo> delayedRequestInfos;

    private File cacheFileTemp;
    private File cacheFileFinal;
    private File cacheIvTemp;

    private String ext;
    private RandomAccessFile fileOutputStream;
    private RandomAccessFile fiv;
    private File storePath;
    private File tempPath;
    private boolean isForceRequest;
    private boolean started;

    public interface FileLoadOperationDelegate {
        void didFinishLoadingFile(FileLoadOperation operation, File finalFile);

        void didFailedLoadingFile(FileLoadOperation operation, int state);

        void didChangedLoadProgress(FileLoadOperation operation, float progress);
    }

    public FileLoadOperation(FileLocation photoLocation, String extension, int size) {
        if (photoLocation instanceof FileLocation.TL_fileEncryptedLocation) {
            location = new InputFileLocation.TL_inputEncryptedFileLocation();
            location.id = photoLocation.volume_id;
            location.volume_id = photoLocation.volume_id;
            location.access_hash = photoLocation.secret;
            location.local_id = photoLocation.local_id;
            iv = new byte[32];
            System.arraycopy(photoLocation.iv, 0, iv, 0, iv.length);
            key = photoLocation.key;
            datacenter_id = photoLocation.dc_id;
        } else if (photoLocation instanceof FileLocation.TL_fileLocation) {
            location = new InputFileLocation.TL_inputFileLocation();
            location.volume_id = photoLocation.volume_id;
            location.secret = photoLocation.secret;
            location.local_id = photoLocation.local_id;
            datacenter_id = photoLocation.dc_id;
        }
        totalBytesCount = size;
        ext = extension != null ? extension : "jpg";
    }

    public FileLoadOperation(Document documentLocation) {
        try {
            if (documentLocation instanceof Document.TL_documentEncrypted) {
                location = new InputFileLocation.TL_inputEncryptedFileLocation();
                location.id = documentLocation.id;
                location.access_hash = documentLocation.access_hash;
                datacenter_id = documentLocation.dc_id;
                iv = new byte[32];
                System.arraycopy(documentLocation.iv, 0, iv, 0, iv.length);
                key = documentLocation.key;
            } else if (documentLocation instanceof Document.TL_document) {
                location = new InputFileLocation.TL_inputDocumentFileLocation();
                location.id = documentLocation.id;
                location.access_hash = documentLocation.access_hash;
                datacenter_id = documentLocation.dc_id;
            }
            totalBytesCount = documentLocation.size;
            if (key != null) {
                int toAdd = 0;
                if (totalBytesCount % 16 != 0) {
                    bytesCountPadding = 16 - totalBytesCount % 16;
                    totalBytesCount += bytesCountPadding;
                }
            }
            ext = FileLoader.getDocumentFileName(documentLocation);
            int idx;
            if (ext == null || (idx = ext.lastIndexOf('.')) == -1) {
                ext = "";
            } else {
                ext = ext.substring(idx);
            }
            if (ext.length() <= 1) {
                if (documentLocation.mime_type != null) {
                    switch (documentLocation.mime_type) {
                        case "video/mp4":
                            ext = ".mp4";
                            break;
                        case "audio/ogg":
                            ext = ".ogg";
                            break;
                        default:
                            ext = "";
                            break;
                    }
                } else {
                    ext = "";
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            state = stateFailed;
            cleanup();
            Utilities.stageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    delegate.didFailedLoadingFile(FileLoadOperation.this, 0);
                }
            });
        }
    }

    public void setForceRequest(boolean forceRequest) {
        isForceRequest = forceRequest;
    }

    public boolean isForceRequest() {
        return isForceRequest;
    }

    public void setPaths(File store, File temp) {
        storePath = store;
        tempPath = temp;
    }

    public boolean wasStarted() {
        return started;
    }

    public boolean start() {
        if (state != stateIdle) {
            return false;
        }
        if (location == null) {
            onFail(true, 0);
            return false;
        }

        String fileNameFinal;
        String fileNameTemp;
        String fileNameIv = null;
        if (location.volume_id != 0 && location.local_id != 0) {
            if (datacenter_id == Integer.MIN_VALUE || location.volume_id == Integer.MIN_VALUE || datacenter_id == 0) {
                onFail(true, 0);
                return false;
            }

            fileNameTemp = location.volume_id + "_" + location.local_id + ".temp";
            fileNameFinal = location.volume_id + "_" + location.local_id + "." + ext;
            if (key != null) {
                fileNameIv = location.volume_id + "_" + location.local_id + ".iv";
            }
        } else {
            if (datacenter_id == 0 || location.id == 0) {
                onFail(true, 0);
                return false;
            }
            fileNameTemp = datacenter_id + "_" + location.id + ".temp";
            fileNameFinal = datacenter_id + "_" + location.id + ext;
            if (key != null) {
                fileNameIv = datacenter_id + "_" + location.id + ".iv";
            }
        }
        currentDownloadChunkSize = totalBytesCount >= bigFileSizeFrom ? downloadChunkSizeBig : downloadChunkSize;
        currentMaxDownloadRequests = totalBytesCount >= bigFileSizeFrom ? maxDownloadRequestsBig : maxDownloadRequests;
        requestInfos = new ArrayList<>(currentMaxDownloadRequests);
        delayedRequestInfos = new ArrayList<>(currentMaxDownloadRequests - 1);
        state = stateDownloading;

        cacheFileFinal = new File(storePath, fileNameFinal);
        boolean exist = cacheFileFinal.exists();
        if (exist && totalBytesCount != 0 && totalBytesCount != cacheFileFinal.length()) {
            cacheFileFinal.delete();
        }

        if (!cacheFileFinal.exists()) {
            cacheFileTemp = new File(tempPath, fileNameTemp);
            boolean newKeyGenerated = false;

            if (cacheFileTemp.exists()) {
                if (newKeyGenerated) {
                    cacheFileTemp.delete();
                } else {
                    downloadedBytes = (int) cacheFileTemp.length();
                    nextDownloadOffset = downloadedBytes = downloadedBytes / currentDownloadChunkSize * currentDownloadChunkSize;
                }
            }

            if (fileNameIv != null) {
                cacheIvTemp = new File(tempPath, fileNameIv);
                try {
                    fiv = new RandomAccessFile(cacheIvTemp, "rws");
                    if (!newKeyGenerated) {
                        long len = cacheIvTemp.length();
                        if (len > 0 && len % 32 == 0) {
                            fiv.read(iv, 0, 32);
                        } else {
                            downloadedBytes = 0;
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                    downloadedBytes = 0;
                }
            }
            try {
                fileOutputStream = new RandomAccessFile(cacheFileTemp, "rws");
                if (downloadedBytes != 0) {
                    fileOutputStream.seek(downloadedBytes);
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            if (fileOutputStream == null) {
                onFail(true, 0);
                return false;
            }
            started = true;
            Utilities.stageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    if (totalBytesCount != 0 && downloadedBytes == totalBytesCount) {
                        try {
                            onFinishLoadingFile(false);
                        } catch (Exception e) {
                            onFail(true, 0);
                        }
                    }
                }
            });
        } else {
            started = true;
            try {
                onFinishLoadingFile(false);
            } catch (Exception e) {
                onFail(true, 0);
            }
        }
        return true;
    }

    private void onFail(boolean thread, final int reason) {
        cleanup();
        state = stateFailed;
        if (thread) {
            Utilities.stageQueue.postRunnable(new Runnable() {
                @Override
                public void run() {
                    delegate.didFailedLoadingFile(FileLoadOperation.this, reason);
                }
            });
        } else {
            delegate.didFailedLoadingFile(FileLoadOperation.this, reason);
        }
    }

    public void cancel() {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (state == stateFinished || state == stateFailed) {
                    return;
                }
                state = stateFailed;
                cleanup();
                if (requestInfos != null) {
                    for (int a = 0; a < requestInfos.size(); a++) {
                        RequestInfo requestInfo = requestInfos.get(a);
                    }
                }
                delegate.didFailedLoadingFile(FileLoadOperation.this, 1);
            }
        });
    }

    private void cleanup() {
        try {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.getChannel().close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                fileOutputStream.close();
                fileOutputStream = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (fiv != null) {
                fiv.close();
                fiv = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (delayedRequestInfos != null) {
            for (int a = 0; a < delayedRequestInfos.size(); a++) {
                RequestInfo requestInfo = delayedRequestInfos.get(a);
                if (requestInfo.response != null) {
                    requestInfo.response.disableFree = false;
                    requestInfo.response.freeResources();
                }
            }
            delayedRequestInfos.clear();
        }
    }

    private void onFinishLoadingFile(final boolean increment) throws Exception {
        if (state != stateDownloading) {
            return;
        }
        state = stateFinished;
        cleanup();
        if (cacheIvTemp != null) {
            cacheIvTemp.delete();
            cacheIvTemp = null;
        }
        if (cacheFileTemp != null) {
            boolean renameResult = cacheFileTemp.renameTo(cacheFileFinal);
            if (!renameResult) {
                renameRetryCount++;
                if (renameRetryCount < 3) {
                    state = stateDownloading;
                    Utilities.stageQueue.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                onFinishLoadingFile(increment);
                            } catch (Exception e) {
                                onFail(false, 0);
                            }
                        }
                    }, 200);
                    return;
                }
                cacheFileFinal = cacheFileTemp;
            }
        }
        delegate.didFinishLoadingFile(FileLoadOperation.this, cacheFileFinal);
    }

    public void setDelegate(FileLoadOperationDelegate delegate) {
        this.delegate = delegate;
    }
}
