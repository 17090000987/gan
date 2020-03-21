package gan.media.mp4;

import gan.log.DebugLog;

public class MP4 {

    final static String Tag = MP4.class.getName();

    static{
        System.loadLibrary("mp4");
    }

    byte[] h264;
    byte[] aac;

    int h264Track=-1,aacTrack=-1;
    Mp4DataCallBack mp4DataCallBack;

    public void setMp4DataCallBack(Mp4DataCallBack mp4DataCallBack) {
        this.mp4DataCallBack = mp4DataCallBack;
    }

    public native int open();

    public native int packet2Mp4(String in_file,String out_file);

    public native int openInput(String in_file);

    /**
     * @param out_file 文件路径，可以为null，数据回调Mp4DataCallBack
     * @return
     */
    public native int openOutput(String out_file);

    public int addH264Track(byte[] h264,int offset,int length){
        this.h264 = new byte[length];
        System.arraycopy(h264,offset,this.h264,0,length);
        return h264Track=0;
    }

    public int addAacTrack(byte[] aac,int offset,int length){
        this.aac = new byte[length];
        System.arraycopy(aac,offset,this.aac,0,length);
        return aacTrack=1;
    }

    public native int h264AndAAc2Mp4(String out_file);

    public int writeH264AndAACHeader(){
        return writeH264AndAACHeader(h264,aac);
    }

    public native int writeH264AndAACHeader(byte[] h264,byte[] aac);

    public native int writeHeader1(byte[] sdp,byte[] sps);

    public native int writeHeader(byte[] sdp,int videoWidth,int videoHeight);

    /**
     *编码数据帧 写入MP4
     * @param stream_index video 0 audio 1
     * @param frame
     * @param frameLen
     * @param time
     * @return
     */
    public native int writeFrame(int stream_index,byte[] frame,int frameLen,long time);

    public native int closeInput();

    public native int closeOutput();

    public native int close();

    public void onMp4(byte[] data,int length){
        DebugLog.info("onMp4 length:"+length);
        if(mp4DataCallBack!=null){
            mp4DataCallBack.onMp4(data,length);
        }
    }
}
