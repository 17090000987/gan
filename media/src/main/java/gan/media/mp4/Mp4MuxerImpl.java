package gan.media.mp4;

import gan.media.MediaConfig;

import java.util.concurrent.atomic.AtomicBoolean;

public class Mp4MuxerImpl implements Mp4Muxer, Mp4DataCallBack {

    Fmp4 mMp4;
    protected OnMuxerCallBack mOnMuxerCallBack;
    private AtomicBoolean mIsRuning = new AtomicBoolean(false);

    @Override
    public Mp4Muxer setOnMuxerCallBack(OnMuxerCallBack onMuxerCallBack) {
        this.mOnMuxerCallBack = onMuxerCallBack;
        return this;
    }

    @Override
    public void init(MediaConfig config) {
        mMp4 = new Fmp4(config);
        mMp4.setMp4DataCallBack(this);
        mMp4.create(config.outputFile);
        mIsRuning.set(true);
    }

    @Override
    public int addVideoTrack(byte[] buffer, int offset, int length, long timesample) {
        return mMp4.addVideoTrack(buffer, offset, length, timesample);
    }

    @Override
    public int addAudioTrack(byte[] adts_aac, int offset, int length, long timesample) {
        return mMp4.addAudioTrack(adts_aac, offset, length, timesample);
    }

    @Override
    public int writeSample(int track, byte[] sample, int offset, int length, long timesample, int iframe) {
        return mMp4.writeSample(track, sample, offset, length, timesample, iframe);
    }

    public boolean isRuning() {
        return mMp4!=null&&mIsRuning.get();
    }

    @Override
    public void stop() {
        mIsRuning.set(false);
    }

    @Override
    public void release() {
        stop();
        if(mMp4!=null){
            mIsRuning.set(false);
            mMp4.close();
        }
    }

    @Override
    public void onMp4(byte[] data, int length) {
        if(mOnMuxerCallBack!=null){
            mOnMuxerCallBack.onMp4(data,length);
        }
    }

}
