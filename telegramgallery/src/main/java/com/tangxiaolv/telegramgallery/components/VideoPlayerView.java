package com.tangxiaolv.telegramgallery.components;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.coremedia.iso.IsoFile;
import com.coremedia.iso.boxes.Box;
import com.coremedia.iso.boxes.MediaBox;
import com.coremedia.iso.boxes.MediaHeaderBox;
import com.coremedia.iso.boxes.SampleSizeBox;
import com.coremedia.iso.boxes.TrackBox;
import com.coremedia.iso.boxes.TrackHeaderBox;
import com.googlecode.mp4parser.util.Matrix;
import com.googlecode.mp4parser.util.Path;
import com.tangxiaolv.telegramgallery.R;
import com.tangxiaolv.telegramgallery.exoplayer2.ExoPlayer;
import com.tangxiaolv.telegramgallery.exoplayer2.ui.AspectRatioFrameLayout;
import com.tangxiaolv.telegramgallery.utils.AndroidUtilities;
import com.tangxiaolv.telegramgallery.utils.FileLog;
import com.tangxiaolv.telegramgallery.utils.LayoutHelper;
import com.tangxiaolv.telegramgallery.utils.Utilities;
import com.tangxiaolv.telegramgallery.entity.VideoEditedInfo;

import java.io.File;
import java.util.List;

public class VideoPlayerView extends FrameLayout {
    private Activity parentActivity;
    private ImageView videoPlayButton;
    private VideoPlayer videoPlayer;
    private TextureView videoTextureView;
    private AspectRatioFrameLayout aspectRatioFrameLayout;
    private VideoTimelinePlayView videoTimelineView;
    private Runnable currentLoadingVideoRunnable;

    private boolean isPlaying;
    private boolean muteVideo;
    private float videoDuration;

    /*video params*/
    private int compressionsCount = -1;
    private int resultWidth;
    private int resultHeight;
    private int originalWidth;
    private int originalHeight;
    private int originalBitrate;
    private int bitrate;
    private int estimatedSize;
    private int rotationValue;

    /**
     * 0 - 240
     * 1 - 360
     * 2 - 480
     * 3 - 720
     * 4 - 1080
     */
    private int selectedCompression = 1;

    private long videoFramesSize;
    private long audioFramesSize;
    private long originalSize;
    private long startTime;
    private long endTime;
    private long estimatedDuration;
    private long cutDuration;

    private String videoPath;

    public VideoPlayerView(@NonNull Context context) {
        this(context, null);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VideoPlayerView(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        if (!(context instanceof Activity)) return;
        parentActivity = (Activity) context;
        initView();
    }

    private void initView() {
        /*VideoPlayer*/
        FrameLayout playclt = new FrameLayout(getContext());
        playclt.setBackgroundResource(R.drawable.circle_big);
        videoPlayButton = new ImageView(getContext());
        videoPlayButton.setBackgroundResource(R.drawable.ic_video_play);
        playclt.addView(videoPlayButton, LayoutHelper.createFrame(24, 24, Gravity.CENTER));
        addView(playclt, LayoutHelper.createFrame(64, 64, Gravity.CENTER));
        playclt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (videoPlayer == null) {
                    return;
                }
                if (isPlaying) {
                    videoPlayer.pause();
                } else {
                    if (videoPlayer.getCurrentPosition() == videoPlayer.getDuration()) {
                        videoPlayer.seekTo(0);
                    }
                    videoPlayer.play();
                }
            }
        });

        videoTimelineView = new VideoTimelinePlayView(getContext());
        videoTimelineView.setVisibility(GONE);
        videoTimelineView.setDelegate(new VideoTimelinePlayView.VideoTimelineViewDelegate() {
            @Override
            public void onLeftProgressChanged(float progress) {
                if (videoPlayer == null) {
                    return;
                }
                if (videoPlayer.isPlaying()) {
                    videoPlayer.pause();
                }
                videoPlayer.seekTo((int) (videoPlayer.getDuration() * progress));
                videoTimelineView.setProgress(0);
                updateVideoInfo();
            }

            @Override
            public void onRightProgressChanged(float progress) {
                if (videoPlayer == null) {
                    return;
                }
                if (videoPlayer.isPlaying()) {
                    videoPlayer.pause();
                }
                videoPlayer.seekTo((int) (videoPlayer.getDuration() * progress));
                videoTimelineView.setProgress(0);
                updateVideoInfo();
            }

            @Override
            public void onPlayProgressChanged(float progress) {
                videoPlayer.seekTo((int) (videoPlayer.getDuration() * progress));
            }

            @Override
            public void didStartDragging() {

            }

            @Override
            public void didStopDragging() {

            }
        });

        addView(videoTimelineView, LayoutHelper.createFrame(
                LayoutHelper.MATCH_PARENT, 58, Gravity.BOTTOM, 0, 8, 0, 88));
    }

    private void preparePlayer(File file, boolean playWhenReady) {
        releasePlayer();
        if (videoTextureView == null) {
            aspectRatioFrameLayout = new AspectRatioFrameLayout(getContext());
            aspectRatioFrameLayout.setVisibility(View.INVISIBLE);
            addView(aspectRatioFrameLayout, 0, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));

            videoTextureView = new TextureView(getContext());
            videoTextureView.setOpaque(false);
            aspectRatioFrameLayout.addView(videoTextureView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        }

        if (videoPlayer == null) {
            videoPlayer = new VideoPlayer();
            videoPlayer.setTextureView(videoTextureView);
            videoPlayer.setDelegate(new VideoPlayer.VideoPlayerDelegate() {
                @Override
                public void onStateChanged(boolean playWhenReady, int playbackState) {
                    if (videoPlayer == null) {
                        return;
                    }
                    if (playbackState != ExoPlayer.STATE_ENDED && playbackState != ExoPlayer.STATE_IDLE) {
                        try {
                            parentActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            parentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    if (playbackState == ExoPlayer.STATE_READY && aspectRatioFrameLayout.getVisibility() != View.VISIBLE) {
                        aspectRatioFrameLayout.setVisibility(View.VISIBLE);
                    }
                    if (videoPlayer.isPlaying() && playbackState != ExoPlayer.STATE_ENDED) {
                        isPlaying = true;
                        videoPlayButton.setBackgroundResource(R.drawable.ic_video_pause);
                        AndroidUtilities.runOnUIThread(updateProgressRunnable);
                    } else if (isPlaying) {
                        isPlaying = false;
                        videoPlayButton.setBackgroundResource(R.drawable.ic_video_play);
                        AndroidUtilities.cancelRunOnUIThread(updateProgressRunnable);
                        if (playbackState == ExoPlayer.STATE_ENDED) {
                            videoPlayer.pause();
                        }
                    }
                    if (playbackState == ExoPlayer.STATE_ENDED) {
                        videoPlayer.seekTo(0);
                    }
                }

                @Override
                public void onError(Exception e) {
                    e.printStackTrace();
                }

                @Override
                public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                    if (aspectRatioFrameLayout != null) {
                        if (unappliedRotationDegrees == 90 || unappliedRotationDegrees == 270) {
                            int temp = width;
                            width = height;
                            height = temp;
                        }
                        aspectRatioFrameLayout.setAspectRatio(height == 0 ? 1 : (width * pixelWidthHeightRatio) / height, unappliedRotationDegrees);
                    }
                }

                @Override
                public void onRenderedFirstFrame() {
                }

                @Override
                public boolean onSurfaceDestroyed(SurfaceTexture surfaceTexture) {
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

                }
            });
        }
        videoPlayer.preparePlayer(Uri.fromFile(file), "other");
        videoPlayer.setPlayWhenReady(playWhenReady);
    }

    private void releasePlayer() {
        if (videoPlayer != null) {
            videoPlayer.pause();
            videoPlayer.releasePlayer();
            videoPlayer = null;
        }
        try {
            parentActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (aspectRatioFrameLayout != null) {
            removeView(aspectRatioFrameLayout);
            aspectRatioFrameLayout = null;
        }
        if (videoTextureView != null) {
            videoTextureView = null;
        }

        if (isPlaying) {
            isPlaying = false;
            videoPlayButton.setImageResource(R.drawable.ic_video_play);
            AndroidUtilities.cancelRunOnUIThread(updateProgressRunnable);
        }
    }

    private void processOpenVideo(final String videoPath) {
        if (currentLoadingVideoRunnable != null) {
            Utilities.globalQueue.cancelRunnable(currentLoadingVideoRunnable);
            currentLoadingVideoRunnable = null;
        }
        muteVideo = false;
        compressionsCount = -1;
        rotationValue = 0;
        File file = new File(videoPath);
        originalSize = file.length();

        Utilities.globalQueue.postRunnable(currentLoadingVideoRunnable = new Runnable() {
            @Override
            public void run() {
                if (currentLoadingVideoRunnable != this) {
                    return;
                }
                TrackHeaderBox trackHeaderBox = null;
                boolean isAvc = true;
                try {
                    IsoFile isoFile = new IsoFile(videoPath);
                    List<Box> boxes = Path.getPaths(isoFile, "/moov/trak/");
                    boolean isMp4A = true;

                    Box boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/mp4a/");
                    if (boxTest == null) {
                        isMp4A = false;
                    }

                    if (!isMp4A) {
                        return;
                    }

                    boxTest = Path.getPath(isoFile, "/moov/trak/mdia/minf/stbl/stsd/avc1/");
                    if (boxTest == null) {
                        isAvc = false;
                    }

                    for (int b = 0; b < boxes.size(); b++) {
                        if (currentLoadingVideoRunnable != this) {
                            return;
                        }
                        Box box = boxes.get(b);
                        TrackBox trackBox = (TrackBox) box;
                        long sampleSizes = 0;
                        long trackBitrate = 0;
                        try {
                            MediaBox mediaBox = trackBox.getMediaBox();
                            MediaHeaderBox mediaHeaderBox = mediaBox.getMediaHeaderBox();
                            SampleSizeBox sampleSizeBox = mediaBox.getMediaInformationBox().getSampleTableBox().getSampleSizeBox();
                            long[] sizes = sampleSizeBox.getSampleSizes();
                            for (int a = 0; a < sizes.length; a++) {
                                if (currentLoadingVideoRunnable != this) {
                                    return;
                                }
                                sampleSizes += sizes[a];
                            }
                            videoDuration = (float) mediaHeaderBox.getDuration() / (float) mediaHeaderBox.getTimescale();
                            trackBitrate = (int) (sampleSizes * 8 / videoDuration);
                        } catch (Exception e) {
                            FileLog.e(e);
                        }
                        if (currentLoadingVideoRunnable != this) {
                            return;
                        }
                        TrackHeaderBox headerBox = trackBox.getTrackHeaderBox();
                        if (headerBox.getWidth() != 0 && headerBox.getHeight() != 0) {
                            trackHeaderBox = headerBox;
                            originalBitrate = bitrate = (int) (trackBitrate / 100000 * 100000);
                            if (bitrate > 900000) {
                                bitrate = 900000;
                            }
                            videoFramesSize += sampleSizes;
                        } else {
                            audioFramesSize += sampleSizes;
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                    return;
                }
                if (trackHeaderBox == null) {
                    return;
                }
                final boolean isAvcFinal = isAvc;
                final TrackHeaderBox trackHeaderBoxFinal = trackHeaderBox;
                if (currentLoadingVideoRunnable != this) {
                    return;
                }
                currentLoadingVideoRunnable = null;
                AndroidUtilities.runOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        Matrix matrix = trackHeaderBoxFinal.getMatrix();
                        if (matrix.equals(Matrix.ROTATE_90)) {
                            rotationValue = 90;
                        } else if (matrix.equals(Matrix.ROTATE_180)) {
                            rotationValue = 180;
                        } else if (matrix.equals(Matrix.ROTATE_270)) {
                            rotationValue = 270;
                        } else {
                            rotationValue = 0;
                        }
                        resultWidth = originalWidth = (int) trackHeaderBoxFinal.getWidth();
                        resultHeight = originalHeight = (int) trackHeaderBoxFinal.getHeight();

                        if (!isAvcFinal && (resultWidth == originalWidth || resultHeight == originalHeight)) {
                            return;
                        }

                        videoDuration *= 1000;

                        if (originalWidth > 1280 || originalHeight > 1280) {
                            compressionsCount = 5;
                        } else if (originalWidth > 848 || originalHeight > 848) {
                            compressionsCount = 4;
                        } else if (originalWidth > 640 || originalHeight > 640) {
                            compressionsCount = 3;
                        } else if (originalWidth > 480 || originalHeight > 480) {
                            compressionsCount = 2;
                        } else {
                            compressionsCount = 1;
                        }
                        updateWidthHeightBitrateForCompression();
                        updateVideoInfo();
                    }
                });
            }
        });
    }

    private void updateVideoInfo() {
        estimatedDuration = (long) Math.ceil((videoTimelineView.getRightProgress()
                - videoTimelineView.getLeftProgress()) * videoPlayer.getDuration());

        if (selectedCompression == compressionsCount - 1) {
            estimatedSize = (int) (originalSize * ((float) estimatedDuration / videoDuration));
        } else {
            estimatedSize = (int) ((audioFramesSize + videoFramesSize) * ((float) estimatedDuration / videoDuration));
            estimatedSize += estimatedSize / (32 * 1024) * 16;
        }

        if (videoTimelineView.getLeftProgress() == 0) {
            startTime = -1;
        } else {
            startTime = (long) (videoTimelineView.getLeftProgress() * videoPlayer.getDuration()) * 1000;
        }
        if (videoTimelineView.getRightProgress() == 1) {
            endTime = -1;
        } else {
            endTime = (long) (videoTimelineView.getRightProgress() * videoPlayer.getDuration()) * 1000;
        }
    }

    private Runnable updateProgressRunnable = new Runnable() {
        @Override
        public void run() {
            if (videoPlayer == null) {
                return;
            }
            if (!videoTimelineView.isDragging()) {
                float progress = videoPlayer.getCurrentPosition() / (float) videoPlayer.getDuration();
                if (videoTimelineView.getVisibility() == View.VISIBLE) {
                    if (progress >= videoTimelineView.getRightProgress()) {
                        videoPlayer.pause();
                        videoTimelineView.setProgress(0);
                        videoPlayer.seekTo((int) (videoTimelineView.getLeftProgress() * videoPlayer.getDuration()));
                    } else {
                        progress -= videoTimelineView.getLeftProgress();
                        if (progress < 0) {
                            progress = 0;
                        }
                        progress /= (videoTimelineView.getRightProgress() - videoTimelineView.getLeftProgress());
                        if (progress > 1) {
                            progress = 1;
                        }
                        videoTimelineView.setProgress(progress);
                    }
                } else {
                    videoTimelineView.setProgress(progress);
                }
            }

            if (isPlaying) {
                AndroidUtilities.runOnUIThread(updateProgressRunnable);
            }
        }
    };

    public VideoEditedInfo getCurrentVideoEditedInfo() {
        long cutStartTime = startTime == -1 ? 0 : startTime;
        long cutEndTime = endTime == -1 ? videoPlayer.getDuration() * 1000 : endTime;
        cutDuration = (cutEndTime - cutStartTime) / 1000;

        VideoEditedInfo videoEditedInfo = new VideoEditedInfo();
        videoEditedInfo.startTime = startTime;
        videoEditedInfo.endTime = endTime;
        videoEditedInfo.rotationValue = rotationValue;
        videoEditedInfo.originalWidth = originalWidth;
        videoEditedInfo.originalHeight = originalHeight;
        videoEditedInfo.bitrate = bitrate;
        videoEditedInfo.originalPath = videoPath;
//        videoEditedInfo.estimatedSize = estimatedSize;
//        videoEditedInfo.estimatedDuration = estimatedDuration;
        videoEditedInfo.estimatedSize = 0;
        videoEditedInfo.estimatedDuration = 0;

        if (!muteVideo && selectedCompression == compressionsCount - 1) {
            videoEditedInfo.resultWidth = originalWidth;
            videoEditedInfo.resultHeight = originalHeight;
            videoEditedInfo.bitrate = muteVideo ? -1 : originalBitrate;
            videoEditedInfo.muted = muteVideo;
        } else {
            if (muteVideo) {
                selectedCompression = 1;
                updateWidthHeightBitrateForCompression();
            }
            videoEditedInfo.resultWidth = resultWidth;
            videoEditedInfo.resultHeight = resultHeight;
            videoEditedInfo.bitrate = muteVideo ? -1 : bitrate;
            videoEditedInfo.muted = muteVideo;
        }
        return videoEditedInfo;
    }

    private void updateWidthHeightBitrateForCompression() {
        if (selectedCompression >= compressionsCount) {
            selectedCompression = compressionsCount - 1;
        }
        if (selectedCompression != compressionsCount - 1) {
            float maxSize;
            int targetBitrate;
            switch (selectedCompression) {
                case 0:
                    maxSize = 432.0f;
                    targetBitrate = 400000;
                    break;
                case 1:
                    maxSize = 640.0f;
                    targetBitrate = 900000;
                    break;
                case 2:
                    maxSize = 848.0f;
                    targetBitrate = 1100000;
                    break;
                case 3:
                default:
                    targetBitrate = 1600000;
                    maxSize = 1280.0f;
                    break;
            }
            float scale = originalWidth > originalHeight ? maxSize / originalWidth : maxSize / originalHeight;
            resultWidth = Math.round(originalWidth * scale / 2) * 2;
            resultHeight = Math.round(originalHeight * scale / 2) * 2;
            if (bitrate != 0) {
                bitrate = Math.min(targetBitrate, (int) (originalBitrate / scale));
                videoFramesSize = (long) (bitrate / 8 * videoDuration / 1000);
            }
        }
    }

    public void prepareVideoPlayer(String path, boolean canEdit, boolean playWhenReady) {
        videoPath = path;
        File videoF = new File(path);
        if (!videoF.exists()) return;
        preparePlayer(videoF, playWhenReady);
        processOpenVideo(path);

        if (canEdit) {
            showEditFrame();
        }
    }

    public void showEditFrame() {
        videoTimelineView.setVideoPath(videoPath);
        videoTimelineView.setProgress(0);
        videoTimelineView.setVisibility(VISIBLE);
    }

    public void release() {
        releasePlayer();
        videoTimelineView.destroy();
    }

    public long getCutDuration() {
        return cutDuration;
    }
}
