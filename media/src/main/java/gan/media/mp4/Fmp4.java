package gan.media.mp4;

import gan.log.DebugLog;
import gan.media.MediaConfig;

public class Fmp4 {

    final static String Tag = Fmp4.class.getName();

    static{
        System.loadLibrary("fmp4");
    }

    private long handle;
    MediaConfig mMp4Config;
    Mp4DataCallBack mp4DataCallBack;

    public Fmp4(){
        this(MediaConfig.defaultConfig());
    }

    public Fmp4(MediaConfig mp4Config){
        mMp4Config = mp4Config;
    }

    @Override
    protected void finalize() throws Throwable {
        try{
            close();
        }finally {
            super.finalize();
        }
    }

    public final void create(String filePath){
        DebugLog.debug( "create in thread:"+Thread.currentThread().getId());
        handle = nativeCreate(filePath);
    }

    public final void close(){
        if(handle>0){
            close(handle);
            handle = 0;
        }
    }

    public void setMp4DataCallBack(Mp4DataCallBack mp4DataCallBack) {
        this.mp4DataCallBack = mp4DataCallBack;
    }

    public int addVideoTrack(byte[] sps_pps_frame,long timesample){
        return addVideoTrack(handle, sps_pps_frame,0, sps_pps_frame.length, timesample,mMp4Config.videoCodec);
    }

    public int addVideoTrack(byte[] sps_pps_frame,int offset,int length,long timesample){
        DebugLog.debug( "addVideoTrack in thread:"+Thread.currentThread().getId());
        return addVideoTrack(handle, sps_pps_frame,offset, length, timesample, mMp4Config.videoCodec);
    }

    public int addAudioTrack(byte[] adts_aac, long timesample){
        return addAudioTrack(handle, adts_aac,0, adts_aac.length, timesample, mMp4Config.audioCodec);
    }

    public int addAudioTrack(byte[] adts_aac,int offset,int length, long timesample){
        DebugLog.debug( "addAudioTrack in thread:"+Thread.currentThread().getId());
        return addAudioTrack(handle, adts_aac, offset, length, timesample, mMp4Config.audioCodec);
    }

    public int writeSample(int track,byte[] sample,long timesample,int iframe){
        DebugLog.debug( "writeSample in thread:"+Thread.currentThread().getId());
        return writeSample(handle, track, sample,0, sample.length, timesample, iframe);
    }

    public int writeSample(int track,byte[] sample, int offset, int length,long timesample,int iframe){
        DebugLog.debug( "writeSample in thread:"+Thread.currentThread().getId());
        return writeSample(handle,track, sample, offset, length, timesample, iframe);
    }

    public native int rtsp2mp4(String rtsp,String filePath);

    private native long nativeCreate(String filePath);

    private native int addVideoTrack(long p,byte[] sps_pps_frame,int offset,int length,long timesample,String codec);

    private native int addAudioTrack(long p,byte[] adts_aac,int offset,int length, long timesample, String codec);

    /**
     * (1)h264流中的NAL，头四个字节是0x00000001;
     * (2)mp4中的h264track，头四个字节要求是NAL的长度，并且是大端顺序；
     * @param track
     * @param sample
     * @param length
     * @param timesample
     * @param iframe IFrame?1:0
     */
    private native int writeSample(long p,int track,byte[] sample, int offset, int length,long timesample,int iframe);

    private native void close(long p);

    public void onMp4(byte[] data,int length){
        DebugLog.debug( "onMp4 in thread:"+Thread.currentThread().getId());
        if(mp4DataCallBack!=null){
            mp4DataCallBack.onMp4(data,length);
        }
    }
}
