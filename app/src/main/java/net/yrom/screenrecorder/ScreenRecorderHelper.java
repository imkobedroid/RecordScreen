package net.yrom.screenrecorder;

import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodecInfo;
import android.media.projection.MediaProjection;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import static net.yrom.screenrecorder.ScreenRecorder.AUDIO_AAC;
import static net.yrom.screenrecorder.ScreenRecorder.VIDEO_AVC;

/**
 * @Description: 录屏控制
 * @Author: fyq
 * @CreateDate: 2020/8/4 2:52 PM
 */
public class ScreenRecorderHelper {

    private static volatile ScreenRecorderHelper instance;

    private MediaCodecInfo[] mAvcCodecInfos; // avc codecs
    private MediaCodecInfo[] mAacCodecInfos; // aac codecs
    private ScreenRecorder mRecorder;
    private VirtualDisplay mVirtualDisplay;

    public static ScreenRecorderHelper me() {
        if (null == instance) {
            synchronized (ScreenRecorderHelper.class) {
                if (null == instance) {
                    instance = new ScreenRecorderHelper();
                }
            }
        }
        return instance;
    }

    public void init() {
        //初始化做的事
        setAvcCodeInfos();
        setAacCodecInfos();
    }


    /**
     * @method  startCapturing
     * @description 开始录屏
     * @date: 2020/8/5
     * @author: fyq
     * @param
     * @return void
     */
    public void startCapturing(ScreenRecorderIntegratedModel screenRecorderIntegratedModel) {
        ScreenRecorderVideoModel screenRecorderVideoModel = screenRecorderIntegratedModel.getScreenRecorderVideoModel();
        ScreenRecorderAudioModel screenRecorderAudioModel = screenRecorderIntegratedModel.getScreenRecorderAudioModel();
        MediaProjection mediaProjection = screenRecorderIntegratedModel.getMediaProjection();
        if (screenRecorderVideoModel == null || mediaProjection == null) {
            return;
        }

        VideoEncodeConfig video = createVideoConfig(screenRecorderVideoModel.getScreenWidth(),
                screenRecorderVideoModel.getScreenHeight(),
                screenRecorderVideoModel.getVideoBitRate(),
                screenRecorderVideoModel.getVideoFrameRate(),
                screenRecorderVideoModel.getVideoIFrame());
        AudioEncodeConfig audio;

        if (screenRecorderAudioModel == null) {
            audio = null;
        } else {
            audio = createAudioConfig(screenRecorderAudioModel.getAudioBitRate(),
                    screenRecorderAudioModel.getAudioSampleRate(),
                    screenRecorderAudioModel.getAudioChannelCount(),
                    screenRecorderAudioModel.getAudioAACProfile());
        }

        File dir = getSavingDir();
        if (!dir.exists() && !dir.mkdirs()) {
            cancelRecorder();
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);
        final File file = new File(dir, "Screenshots-" + format.format(new Date())
                + "-" + "1920x1080"+ ".mp4");
        Log.d("@@", "Create recorder with :" + video + " \n " + audio + "\n " + file);
        mRecorder = newRecorder(mediaProjection, video, audio, file);
        mRecorder.start();
    }

    private VideoEncodeConfig createVideoConfig(int width, int height, int bitRate, int frameRate, int iFrame) {
        if (mAvcCodecInfos.length > 0) {
            final String codec = mAvcCodecInfos[0].getName();
            MediaCodecInfo.CodecProfileLevel profileLevel = null;
            return new VideoEncodeConfig(width, height, bitRate,
                    frameRate, iFrame, codec, VIDEO_AVC, profileLevel);
        } else {
            return null;
        }

    }

    private AudioEncodeConfig createAudioConfig(int bitRate, int sampleRate, int channelCount, int profile) {
        if (mAacCodecInfos.length > 0) {
            String codec = mAacCodecInfos[0].getName();
            return new AudioEncodeConfig(codec, AUDIO_AAC, bitRate, sampleRate, channelCount, profile);
        } else {
            return null;
        }

    }

    
    private void setAvcCodeInfos() {
        Utils.findEncodersByTypeAsync(VIDEO_AVC, infos -> {
            logCodecInfos(infos, VIDEO_AVC);
            mAvcCodecInfos = infos;

        });
    }
    
    private void setAacCodecInfos() {
        Utils.findEncodersByTypeAsync(AUDIO_AAC, infos -> {
            logCodecInfos(infos, AUDIO_AAC);
            mAacCodecInfos = infos;
        });
    }

    
    private static void logCodecInfos(MediaCodecInfo[] codecInfos, String mimeType) {
        for (MediaCodecInfo info : codecInfos) {
            StringBuilder builder = new StringBuilder(512);
            MediaCodecInfo.CodecCapabilities caps = info.getCapabilitiesForType(mimeType);
            builder.append("Encoder '").append(info.getName()).append('\'')
                    .append("\n  supported : ")
                    .append(Arrays.toString(info.getSupportedTypes()));
            MediaCodecInfo.VideoCapabilities videoCaps = caps.getVideoCapabilities();
            if (videoCaps != null) {
                builder.append("\n  Video capabilities:")
                        .append("\n  Widths: ").append(videoCaps.getSupportedWidths())
                        .append("\n  Heights: ").append(videoCaps.getSupportedHeights())
                        .append("\n  Frame Rates: ").append(videoCaps.getSupportedFrameRates())
                        .append("\n  Bitrate: ").append(videoCaps.getBitrateRange());
                if (VIDEO_AVC.equals(mimeType)) {
                    MediaCodecInfo.CodecProfileLevel[] levels = caps.profileLevels;

                    builder.append("\n  Profile-levels: ");
                    for (MediaCodecInfo.CodecProfileLevel level : levels) {
                        builder.append("\n  ").append(Utils.avcProfileLevelToString(level));
                    }
                }
                builder.append("\n  Color-formats: ");
                for (int c : caps.colorFormats) {
                    builder.append("\n  ").append(Utils.toHumanReadable(c));
                }
            }
            MediaCodecInfo.AudioCapabilities audioCaps = caps.getAudioCapabilities();
            if (audioCaps != null) {
                builder.append("\n Audio capabilities:")
                        .append("\n Sample Rates: ").append(Arrays.toString(audioCaps.getSupportedSampleRates()))
                        .append("\n Bit Rates: ").append(audioCaps.getBitrateRange())
                        .append("\n Max channels: ").append(audioCaps.getMaxInputChannelCount());
            }
            Log.i("@@@", builder.toString());
        }
    }

    private static File getSavingDir() {
        return new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES),
                "Screenshots");
    }

    private void cancelRecorder() {
        if (mRecorder == null) return;
        mRecorder.quit();
    }

    
    private ScreenRecorder newRecorder(MediaProjection mediaProjection, VideoEncodeConfig video,
                                       AudioEncodeConfig audio, File output) {
        final VirtualDisplay display = getOrCreateVirtualDisplay(mediaProjection, video);
        ScreenRecorder r = new ScreenRecorder(video, audio, display, output.getAbsolutePath());
        r.setCallback(new ScreenRecorder.Callback() {
            long startTime = 0;

            @Override
            public void onStop(Throwable error) {
            }

            @Override
            public void onStart() {
            }

            @Override
            public void onRecording(long presentationTimeUs) {
            }
        });
        return r;
    }

    private VirtualDisplay getOrCreateVirtualDisplay(MediaProjection mediaProjection, VideoEncodeConfig config) {
        if (mVirtualDisplay == null) {
            mVirtualDisplay = mediaProjection.createVirtualDisplay("ScreenRecorder-display0",
                    config.width, config.height, 1 /*dpi*/,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                    null /*surface*/, null, null);
        } else {
            // resize if size not matched
            Point size = new Point();
            mVirtualDisplay.getDisplay().getSize(size);
            if (size.x != config.width || size.y != config.height) {
                mVirtualDisplay.resize(config.width, config.height, 1);
            }
        }
        return mVirtualDisplay;
    }

    //停止录制做的事===
    public void quitScreen() {
        if (mRecorder != null) {
            mRecorder.quit();
        }
        if (mVirtualDisplay != null) {
            mVirtualDisplay.setSurface(null);
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }


}
