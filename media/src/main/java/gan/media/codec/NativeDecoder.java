package gan.media.codec;

public class NativeDecoder implements Decoder{

    static{
        System.loadLibrary("codec");
    }

    long p;
    int track;
    RawDataCallBack mRawDataCallBack;

    @Override
    protected void finalize() throws Throwable {
        try{
            release();
        }finally {
            super.finalize();
        }
    }

    @Override
    public void init(Object... params) {
        p=init();
    }

    public int openDecoder(byte[] sps_pps_frame,int offset,int length,String codec){
        track = openDecoder(p,sps_pps_frame,offset,length,codec);
        if(track<0){
            throw new IllegalStateException(String.format("open decoder fail:%s", track));
        }
        return track;
    }

    public void decode(byte[] buffer, int offset, int len,long timesample,int iframe){
        decode(track,buffer,offset,len,timesample,iframe);
    }

    @Override
    public void decode(int track,byte[] buffer, int offset, int len,long timesample,int iframe) {
        int i = decode(p,track,buffer,offset, len,timesample,iframe);
//        if(i<0){
//            throw new IllegalStateException(String.format("decode fail:%s",i));
//        }
    }

    @Override
    public void release() {
        if(p>0){
            release(p);
            p=0;
        }
    }

    private native int init();
    private native int openDecoder(long p, byte[] sps_pps_frame,int offset,int length,String codec);
    private native int decode(long p, int track,byte[] buffer, int offset, int len,long timesample,int iframe);
    private native void release(long p);

    public NativeDecoder setRawDataCallBack(RawDataCallBack rawDataCallBack) {
        this.mRawDataCallBack = rawDataCallBack;
        return this;
    }

    public void onRawCallBack(byte[] buf, int size, int timestamp, int width, int height){
        if(mRawDataCallBack!=null){
            mRawDataCallBack.onRawFrame(buf, size, timestamp, width, height);
        }
    }

}
