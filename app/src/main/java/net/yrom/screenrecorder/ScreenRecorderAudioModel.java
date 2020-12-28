package net.yrom.screenrecorder;

/**
 * @Description:
 * @Author: fyq
 * @CreateDate: 2020/8/5 9:53 AM
 */
public class ScreenRecorderAudioModel {
    private int audioChannelCount;
    private int audioSampleRate;
    private int audioBitRate;
    private int audioAACProfile;

    public ScreenRecorderAudioModel(int audioChannelCount, int audioSampleRate, int audioBitRate, int audioAACProfile) {
        this.audioChannelCount = audioChannelCount;
        this.audioSampleRate = audioSampleRate;
        this.audioBitRate = audioBitRate;
        this.audioAACProfile = audioAACProfile;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public int getAudioBitRate() {
        return audioBitRate;
    }

    public int getAudioChannelCount() {
        return audioChannelCount;
    }

    public int getAudioAACProfile() {
        return audioAACProfile;
    }
}
