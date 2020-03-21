package gan.media.mp4;

import gan.media.MediaConfig;

public interface Mp4Muxer {

    public final static int Event_TimeError = 1;
    public final static int Event_SpsChanged = 2;
    public final static int Event_EncodeChanged = 3;
    public final static int Event_VideoSize = 4;

    public void init(MediaConfig config);
    public int addVideoTrack(byte[] buffer,int offset,int length,long timesample);
    public int addAudioTrack(byte[] adts_aac,int offset,int length, long timesample);
    public int writeSample(int track,byte[] sample, int offset, int length,long timesample,int iframe);
    public void stop();
    public void release();

    public Mp4Muxer setOnMuxerCallBack(OnMuxerCallBack onMuxerCallBack);

    public static interface OnMuxerCallBack{
        public void onEvent(int code,String message,Object... params);
        public void onMp4(byte[] data,int length);
    }
}
