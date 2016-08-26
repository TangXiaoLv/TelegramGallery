package com.tangxiaolv.telegramgallery.Utils;

import android.os.Looper;
import android.util.SparseArray;

import java.util.ArrayList;

public class NotificationCenter {

    private static int totalEvents = 1;

    public static final int didReceivedNewMessages = totalEvents++;
    public static final int dialogsNeedReload = totalEvents++;
    public static final int closeChats = totalEvents++;
    public static final int messagesDeleted = totalEvents++;
    public static final int mediaDidLoaded = totalEvents++;
    public static final int mediaCountDidLoaded = totalEvents++;
    public static final int dialogPhotosLoaded = totalEvents++;
    public static final int removeAllMessagesFromDialog = totalEvents++;
    public static final int recentImagesDidLoaded = totalEvents++;
    public static final int didReplacedPhotoInMemCache = totalEvents++;
    public static final int musicDidLoaded = totalEvents++;

    public static final int httpFileDidLoaded = totalEvents++;
    public static final int httpFileDidFailedLoad = totalEvents++;

    public static final int messageThumbGenerated = totalEvents++;

    public static final int emojiDidLoaded = totalEvents++;

    public static final int FileDidUpload = totalEvents++;
    public static final int FileDidFailUpload = totalEvents++;
    public static final int FileUploadProgressChanged = totalEvents++;
    public static final int FileLoadProgressChanged = totalEvents++;
    public static final int FileDidLoaded = totalEvents++;
    public static final int FileDidFailedLoad = totalEvents++;

    public static final int screenshotTook = totalEvents++;
    public static final int albumsDidLoaded = totalEvents++;

    private SparseArray<ArrayList<Object>> observers = new SparseArray<>();
    private SparseArray<ArrayList<Object>> removeAfterBroadcast = new SparseArray<>();
    private SparseArray<ArrayList<Object>> addAfterBroadcast = new SparseArray<>();
    private ArrayList<DelayedPost> delayedPosts = new ArrayList<>(10);

    private int broadcasting = 0;
    private int[] allowedNotifications;
    private boolean animationInProgress;

    public interface NotificationCenterDelegate {
        void didReceivedNotification(int id, Object... args);
    }

    private class DelayedPost {

        private DelayedPost(int id, Object[] args) {
            this.id = id;
            this.args = args;
        }

        private int id;
        private Object[] args;
    }

    private static volatile NotificationCenter Instance = null;

    public static NotificationCenter getInstance() {
        NotificationCenter localInstance = Instance;
        if (localInstance == null) {
            synchronized (NotificationCenter.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new NotificationCenter();
                }
            }
        }
        return localInstance;
    }

    public void setAllowedNotificationsDutingAnimation(int notifications[]) {
        allowedNotifications = notifications;
    }

    public void setAnimationInProgress(boolean value) {
        animationInProgress = value;
        if (!animationInProgress && !delayedPosts.isEmpty()) {
            for (DelayedPost delayedPost : delayedPosts) {
                postNotificationNameInternal(delayedPost.id, true, delayedPost.args);
            }
            delayedPosts.clear();
        }
    }

    public boolean isAnimationInProgress() {
        return animationInProgress;
    }

    public void postNotificationName(int id, Object... args) {
        boolean allowDuringAnimation = false;
        if (allowedNotifications != null) {
            for (int a = 0; a < allowedNotifications.length; a++) {
                if (allowedNotifications[a] == id) {
                    allowDuringAnimation = true;
                    break;
                }
            }
        }
        postNotificationNameInternal(id, allowDuringAnimation, args);
    }

    public void postNotificationNameInternal(int id, boolean allowDuringAnimation, Object... args) {
        if (Looper.getMainLooper() != Looper.myLooper()){
            throw new RuntimeException("addObserver allowed only from MAIN thread");
        }

        if (!allowDuringAnimation && animationInProgress) {
            DelayedPost delayedPost = new DelayedPost(id, args);
            delayedPosts.add(delayedPost);
            return;
        }
        broadcasting++;
        ArrayList<Object> objects = observers.get(id);
        if (objects != null && !objects.isEmpty()) {
            for (int a = 0; a < objects.size(); a++) {
                Object obj = objects.get(a);
                ((NotificationCenterDelegate) obj).didReceivedNotification(id, args);
            }
        }
        broadcasting--;
        if (broadcasting == 0) {
            if (removeAfterBroadcast.size() != 0) {
                for (int a = 0; a < removeAfterBroadcast.size(); a++) {
                    int key = removeAfterBroadcast.keyAt(a);
                    ArrayList<Object> arrayList = removeAfterBroadcast.get(key);
                    for (int b = 0; b < arrayList.size(); b++) {
                        removeObserver(arrayList.get(b), key);
                    }
                }
                removeAfterBroadcast.clear();
            }
            if (addAfterBroadcast.size() != 0) {
                for (int a = 0; a < addAfterBroadcast.size(); a++) {
                    int key = addAfterBroadcast.keyAt(a);
                    ArrayList<Object> arrayList = addAfterBroadcast.get(key);
                    for (int b = 0; b < arrayList.size(); b++) {
                        addObserver(arrayList.get(b), key);
                    }
                }
                addAfterBroadcast.clear();
            }
        }
    }

    public void addObserver(Object observer, int id) {
        if (Looper.getMainLooper() != Looper.myLooper()){
                throw new RuntimeException("addObserver allowed only from MAIN thread");
        }

        if (broadcasting != 0) {
            ArrayList<Object> arrayList = addAfterBroadcast.get(id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                addAfterBroadcast.put(id, arrayList);
            }
            arrayList.add(observer);
            return;
        }
        ArrayList<Object> objects = observers.get(id);
        if (objects == null) {
            observers.put(id, (objects = new ArrayList<>()));
        }
        if (objects.contains(observer)) {
            return;
        }
        objects.add(observer);
    }

    public void removeObserver(Object observer, int id) {
        if (Looper.getMainLooper() != Looper.myLooper()){
            throw new RuntimeException("addObserver allowed only from MAIN thread");
        }

        if (broadcasting != 0) {
            ArrayList<Object> arrayList = removeAfterBroadcast.get(id);
            if (arrayList == null) {
                arrayList = new ArrayList<>();
                removeAfterBroadcast.put(id, arrayList);
            }
            arrayList.add(observer);
            return;
        }
        ArrayList<Object> objects = observers.get(id);
        if (objects != null) {
            objects.remove(observer);
        }
    }
}
