package net.yrom.screenrecorder;

import android.media.projection.MediaProjection;


/**
 * @Description: 集成model
 * @Author: fyq
 * @CreateDate: 2020/8/5 9:40 AM
 */
public class ScreenRecorderIntegratedModel {
    private MediaProjection mediaProjection;//不可为空
    private ScreenRecorderVideoModel screenRecorderVideoModel;//不可为空
    private ScreenRecorderAudioModel screenRecorderAudioModel;//可为空


    public ScreenRecorderIntegratedModel(MediaProjection mediaProjection,
                                         ScreenRecorderVideoModel screenRecorderVideoModel,
                                         ScreenRecorderAudioModel screenRecorderAudioModel) {
        this.mediaProjection = mediaProjection;
        this.screenRecorderVideoModel = screenRecorderVideoModel;
        this.screenRecorderAudioModel = screenRecorderAudioModel;
    }

    public MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    public ScreenRecorderVideoModel getScreenRecorderVideoModel() {
        return screenRecorderVideoModel;
    }

    public ScreenRecorderAudioModel getScreenRecorderAudioModel() {
        return screenRecorderAudioModel;
    }
}
