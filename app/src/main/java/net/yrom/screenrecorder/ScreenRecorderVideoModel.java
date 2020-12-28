package net.yrom.screenrecorder;

/**
 * @Description:
 * @Author: fyq
 * @CreateDate: 2020/8/5 9:54 AM
 */
public class ScreenRecorderVideoModel {
    private int videoBitRate;
    private int videoFrameRate;
    private int videoIFrame;
    private int screenHeight;
    private int screenWidth;


    public ScreenRecorderVideoModel(int videoBitRate, int videoFrameRate, int videoIFrame,int screenWidth, int screenHeight) {
        this.videoBitRate = videoBitRate;
        this.videoFrameRate = videoFrameRate;
        this.videoIFrame = videoIFrame;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public int getVideoBitRate() {
        return videoBitRate;
    }

    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    public int getVideoIFrame() {
        return videoIFrame;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public int getScreenWidth() {
        return screenWidth;
    }
}
