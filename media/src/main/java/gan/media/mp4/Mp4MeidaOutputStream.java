package gan.media.mp4;

import gan.log.DebugLog;
import gan.media.BufferInfo;
import gan.media.Media;
import gan.media.MediaConfig;
import gan.media.MediaOutputStream;
import gan.media.h264.H264SPS;
import gan.media.h264.H264StreamMp4Muxer;
import gan.media.h265.H265StreamMp4Muxer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Mp4MeidaOutputStream implements MediaOutputStream,Mp4Muxer.OnMuxerCallBack {

    String mUrl;
    MediaConfig mMP4Config;
    MP4StreamMuxer mMP4StreamMuxer;

    public Mp4MeidaOutputStream(String url,String filePath){
        this(url,filePath,MediaConfig.defaultConfig());
    }

    public Mp4MeidaOutputStream(String url,String filePath,MediaConfig config){
        mUrl = url;
        mMP4Config = config;
        mMP4Config.setOutputFile(filePath);
    }

    public String getUrl() {
        return mUrl;
    }

    @Override
    public void init() {
        initMuxer();
    }

    private void initMuxer(){
        DebugLog.info("initMuxer");
        if(mMP4Config.isVideoCodec(Media.MediaCodec.CODEC_H265)){
            mMP4StreamMuxer = new H265StreamMp4Muxer();
            mMP4StreamMuxer.setOnMuxerCallBack(this);
        }else{
            mMP4StreamMuxer = new H264StreamMp4Muxer();
            mMP4StreamMuxer.setOnMuxerCallBack(this);
        }

        if(mMP4StreamMuxer!=null){
            mMP4StreamMuxer.init(mMP4Config);
        }else{
            throw new IllegalStateException("not find muxer");
        }
    }

    public void restartMuxer(MediaConfig config){
        releaseMuxer();
        mMP4Config = config;
        initMuxer();
    }

    @Override
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo) throws IOException {
        if(mMP4StreamMuxer!=null){
            DebugLog.debug("write:%s",bufferInfo.toString());
            mMP4StreamMuxer.write(channel, packet, bufferInfo);
        }
    }

    public boolean hasAudio(){
        if(mMP4Config!=null){
            return mMP4Config.audioCodec!=null;
        }
        return false;
    }

    public void releaseMuxer(){
        if(mMP4StreamMuxer!=null){
            mMP4StreamMuxer.release();
        }
    }

    @Override
    public void close() {
        releaseMuxer();
    }

    @Override
    public void onEvent(int code, String message,Object... params) {
        if(code == Mp4Muxer.Event_SpsChanged){
            onSpsChanged((H264SPS) params[0]);
        }else if(code == Mp4Muxer.Event_EncodeChanged){
            onEncoderChanged((Integer) params[0]);
        }else if(code == Mp4Muxer.Event_TimeError){
            onTimeError();
        }else if(code == Mp4Muxer.Event_VideoSize){
            onVideoSize((Integer)params[0],(Integer)params[1]);
        }
    }

    @Override
    public void onMp4(byte[] data, int length) {
    }

    protected void onSpsChanged(H264SPS h264SPS){
        DebugLog.info("onSpsChanged");
    }

    protected void onEncoderChanged(int newEncoder){
        DebugLog.info("onEncoderChanged encoder:"+newEncoder);
    }

    protected void onTimeError(){

    }

    protected void onVideoSize(int width,int height){

    }

}
