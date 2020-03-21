package gan.media.h265;

import gan.log.DebugLog;
import gan.media.BufferInfo;
import gan.media.MediaApplication;
import gan.media.MediaConfig;
import gan.media.h264.H264SPS;
import gan.media.h264.H264SPSPaser;
import gan.media.h26x.HUtils;
import gan.media.mp4.MP4StreamMuxer;
import gan.media.mp4.Mp4Muxer;
import gan.media.mp4.Mp4MuxerImpl;
import gan.core.system.SystemUtils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class H265StreamMp4Muxer extends Mp4MuxerImpl implements MP4StreamMuxer {


    public final static int WRITE_MODE_ALL = 0;
    public final static int WRITE_MODE_FRAME=1;

    int mWriteMode=WRITE_MODE_FRAME;
    boolean mFrameStarted;
    boolean firstIFrame;
    int videoTrack=-1,aacTrack=-1;
    ByteBuffer mFirstIFrameBuffer;
    BufferInfo mFirstIFrameBufferInfo;
    H264SPSPaser mH264SPSPaser;
    byte[] mXpsBuffer = new byte[128];
    int[] xpsLen = new int[]{128};
    private Integer mEncoder;
    private MediaConfig mMP4Config;

    @Override
    public void init(MediaConfig config) {
        DebugLog.info("init");
        mMP4Config =config;
        super.init(config);
        mH264SPSPaser = new H264SPSPaser();
    }

    public H265StreamMp4Muxer setWriteMode(int writeMode) {
        this.mWriteMode = writeMode;
        return this;
    }

    protected void onEvent(int code,String message,Object... params){
        DebugLog.info("onEvent:"+message);
        if(mOnMuxerCallBack!=null){
            mOnMuxerCallBack.onEvent(code,message,params);
        }
    }

    long lastVideoTimeSample;
    public long getVideoTime(long timeSample,long lastTimeSample){
        if(lastTimeSample==0){
            try{
                if(timeSample<lastVideoTimeSample){
                    DebugLog.info("videotime error");
                    onEvent(Mp4Muxer.Event_TimeError,"video time error");
                }
                return timeSample;
            }finally {
                if(lastVideoTimeSample==0){
                    lastVideoTimeSample = timeSample;
                }
                long time = timeSample-lastVideoTimeSample;
                if(time>24000
                        ||time<0){
                    DebugLog.info("videotimex:"+time);
                }
                lastVideoTimeSample = timeSample;
            }
        }
        return timeSample - lastTimeSample;
    }

    long lastAudioTimeSample;
    public long getAudioTime(long timeSample,long lastTimeSample){
        if(lastTimeSample==0){
            try{
                if(timeSample<lastAudioTimeSample){
                    DebugLog.info("audiotime error");
                    onEvent(Mp4Muxer.Event_TimeError,"audiotime error");
                }
                return timeSample;
            }finally {
                if(lastAudioTimeSample == 0){
                    lastAudioTimeSample = timeSample;
                }
                long time = timeSample-lastAudioTimeSample;
                if(time>24000
                        ||time<0){
                    DebugLog.info("audiotimex:"+time);
                }
                lastAudioTimeSample = timeSample;
            }
        }
        return timeSample - lastTimeSample;
    }

    private void setEncoder(int encoder){
        if(mEncoder==null){
            DebugLog.info("encoder:"+encoder);
            mEncoder = encoder;
        }else if(encoder!=mEncoder.intValue()){
            mEncoder = encoder;
            onEncoderChanged(mEncoder);
        }
    }

    protected void onEncoderChanged(int newEncoder){
        DebugLog.info("onEncoderChanged encoder:"+newEncoder);
        onEvent(Mp4Muxer.Event_EncodeChanged,"onEncoderChanged encoder:"+newEncoder, newEncoder);
    }

    ByteBuffer mByteBufferTemp;
    boolean vps,sps,pps;
    static int index;
    @Override
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo) throws IOException {
        if(!isRuning()){
            return;
        }

        if(mFrameStarted){
            if(channel==0){
                if(HUtils.startCodeSize(packet.array(),bufferInfo.offset,5)<3){
                    DebugLog.info("data error do not h264 data 0001 001");
                    return;
                }

                if(H265Utils.isXPS(packet.array(),bufferInfo.offset,40)){
                    firstIFrame = false;//sps pps 后边必须是IFrame (ps 传过来的中间有其他数据，chrome播放器会出问题)
                    offsetSpsPps(packet,bufferInfo);
                    if(firstIFrame= H265Utils.isIFrame(packet.array(), bufferInfo.offset , 5)){
                        setEncoder(1);
                        DebugLog.debug("write iFrame");
                        writeSample(videoTrack, packet.array(), bufferInfo.offset, bufferInfo.length,
                                getVideoTime(bufferInfo.time,0),1);
                        return;
                    }else{
                        setEncoder(2);
                        while(bufferInfo.length>0){
                            int frameLen = HUtils.frameLen(packet.array(),bufferInfo.offset,bufferInfo.length);
                            if(firstIFrame = H265Utils.isIFrame(packet.array(), bufferInfo.offset, 5)){
                                writeSample(videoTrack, packet.array(), bufferInfo.offset, bufferInfo.length,
                                        getVideoTime(bufferInfo.time,0),1);
                                return;
                            }
                            bufferInfo.offsets(frameLen);
                            bufferInfo.length-=frameLen;
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                if(mWriteMode==WRITE_MODE_FRAME){
                    writeFrameSample(packet,bufferInfo);
                }else{
                    writeStreamSample(packet,bufferInfo);
                }

            }else{
                if(aacTrack>=0){
                    bufferInfo.offsets(7);
                    bufferInfo.length-=7;
                    long audioTime = getAudioTime(bufferInfo.time,0);
                    writeSample(aacTrack, packet.array(), bufferInfo.offset, bufferInfo.length,
                                audioTime ,1);
                }
            }
        }else{
            if(channel == 0){
                if(HUtils.startCodeSize(packet.array(),bufferInfo.offset,5)<3){
                    DebugLog.info("h264Track data error do not h264 data 0001 001");
                    return;
                }
                //if(mHasAudio? aacTrack>=0&&h264Track<0:h264Track<0) {
                if(videoTrack<0){
                    if(checkKeyFrameAvaible(packet.array(),bufferInfo.offset,100)){
                        videoTrack = addVideoTrack(packet.array(), bufferInfo.offset, bufferInfo.length,
                                getVideoTime(bufferInfo.time,0));
                        DebugLog.info("videoTrack:"+videoTrack);
                        offsetSpsPps(packet,bufferInfo);
                        mFirstIFrameBuffer = ByteBuffer.allocate(bufferInfo.length);
                        mFirstIFrameBuffer.put(packet.array(), bufferInfo.offset, bufferInfo.length);
                        mFirstIFrameBufferInfo = bufferInfo.clone();
                        mFirstIFrameBufferInfo.offset = 0;
                        firstIFrame = true;//test
                        checkStart();
                    }else{
                        while(bufferInfo.length>0){
                            int frameLen = HUtils.frameLen(packet.array(),bufferInfo.offset,bufferInfo.length);
                            int frameType = H265Utils.getFrameType(packet.array(), bufferInfo.offset, 5);
                            if((frameType==H265Utils.Type_VPS)&&!vps){
                                mByteBufferTemp = ByteBuffer.allocate(MediaApplication.getMediaConfig().rtspFrameBufferSize);
                                mByteBufferTemp.put(packet.array(),bufferInfo.offset, frameLen);
                                vps = true;
                            }else if(vps&&(frameType==H265Utils.Type_SPS)&&!sps){
                                mByteBufferTemp.put(packet.array(),bufferInfo.offset, frameLen);
                                int startCodeSize = HUtils.startCodeSize(mByteBufferTemp.array(),0,5);
                                if(startCodeSize>0){
                                    int spsLen = frameLen-startCodeSize;
                                    System.arraycopy(mByteBufferTemp.array(), startCodeSize, mXpsBuffer,0, spsLen);
                                    parseSps(mXpsBuffer, spsLen);
                                }else{
                                    parseSps(mByteBufferTemp.array(), frameLen);
                                }
                                sps = true;
                            }else if(sps&&(frameType==H265Utils.Type_PPS)&&!pps){
                                mByteBufferTemp.put(packet.array(),bufferInfo.offset, frameLen);
                                pps = true;
                            }else if(sps&&pps&&(frameType==H265Utils.Type_IDR)){
                                mByteBufferTemp.put(packet.array(),bufferInfo.offset, frameLen);
                                videoTrack = addVideoTrack(mByteBufferTemp.array(), 0, mByteBufferTemp.position(),
                                        getVideoTime(bufferInfo.time,0));
                                mByteBufferTemp.clear();
                                mByteBufferTemp=null;
                                DebugLog.info("videoTrack:"+videoTrack);
                                mFirstIFrameBuffer = ByteBuffer.allocate(frameLen);
                                mFirstIFrameBuffer.put(packet.array(), bufferInfo.offset, frameLen);
                                mFirstIFrameBufferInfo = bufferInfo.clone();
                                mFirstIFrameBufferInfo.length = frameLen;
                                mFirstIFrameBufferInfo.offset = 0;
                                firstIFrame = true;//test
                                checkStart();
                            }

                            bufferInfo.offsets(frameLen);
                            bufferInfo.length-=frameLen;
                            try {
                                Thread.sleep(1);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }else{
                if(hasAudio()){
                    if(aacTrack<0){
                        aacTrack = addAudioTrack(packet.array(), bufferInfo.offset, bufferInfo.length,
                                getAudioTime(bufferInfo.time, 0));
                        DebugLog.info("aacTrack:"+aacTrack);
                        checkStart();
                    }
                }
            }
        }
    }

    /**
     * 多帧数据使用，确定只有一个帧的数据请使用{@link #writeFrameSample(ByteBuffer, BufferInfo)}}
     * @param packet
     * @param bufferInfo
     */
    private void writeStreamSample(ByteBuffer packet, BufferInfo bufferInfo){
        while(bufferInfo.length>0){
            int frameLen = HUtils.frameLen(packet.array(),bufferInfo.offset,bufferInfo.length);
            writeFrameSample(packet,bufferInfo);
            bufferInfo.offsets(frameLen);
            bufferInfo.length-=frameLen;
            if (bufferInfo.length>0) {
                DebugLog.info("more frame");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void writeFrameSample(ByteBuffer packet, BufferInfo bufferInfo){
        byte nula = HUtils.getNaluByte(packet.array(), bufferInfo.offset, 10);
        /**
         * forbidden_zero_bit ==1  这个值应该为0，当它不为0时，表示网络传输过程中，当前NALU中可能存在错误，
         * 解码器可以考虑不对这个NALU进行解码。
         */
        if((nula&0x80)==0x80){
            DebugLog.warn("nula forbidden_zero_bit=1 form error frame data");
            return;
        }

        int frameType = H265Utils.getNulaType(nula);
        writeH265FrameSample(packet,bufferInfo,frameType);
    }

    FileOutputStream fos;
    private void writeH265FrameSample(ByteBuffer packet, BufferInfo bufferInfo,int frameType){
        DebugLog.debug("bufferInfo:"+bufferInfo.toString());
        DebugLog.debug("frameType:"+frameType);

        if(frameType == H265Utils.Type_Unknow){
            DebugLog.warn("unknown nula form error frame data");
            return;
        }

        if(frameType!=H265Utils.Type_IDR
                &&frameType!=H265Utils.Type_02){
            //其他帧数据，合成的视频在google浏览器上边播放中途会断开
            DebugLog.debug("exception frameType:"+frameType);
            return;
        }

//        if(GanServer.getGan().debug){
//            try{
//                if(fos==null){
//                    fos = FileHelper.createFileOutputStream(SystemServer.getRootPath("/media/h264/error_"+index++));
//                }
//                fos.write(packet.array(),bufferInfo.offset,bufferInfo.length);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }

        boolean iFrame = frameType == H265Utils.Type_IDR;
        if (firstIFrame) {
            long videoTime = getVideoTime(bufferInfo.time, 0);
            //h264.putInt(0,bufferInfo.length-4);//mp4
            writeSample(videoTrack, packet.array(), bufferInfo.offset, bufferInfo.length,
                    videoTime, iFrame ? 1 : 0);
        } else {
            if (iFrame) {
                long videoTime = getVideoTime(bufferInfo.time, 0);
                //h264.putInt(0,bufferInfo.length-4);
                writeSample(videoTrack, packet.array(), bufferInfo.offset, bufferInfo.length,
                        videoTime, 1);
                firstIFrame = true;
            }
        }
    }

    H264SPS mH264SPS;
    H264SPS mH264SPS1=new H264SPS();
    private int offsetSpsPps(ByteBuffer packet, BufferInfo bufferInfo){
        int offset = 0;
        xpsLen[0]=128;
        int startCodeSize = HUtils.startCodeSize(packet.array(),bufferInfo.offset,5);
        int ret = H265Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length, mXpsBuffer, xpsLen,H265Utils.Type_SPS, startCodeSize);
        if(ret>=0){
            int len = startCodeSize+ xpsLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
            parseSps(mXpsBuffer, xpsLen[0]);
        }

        xpsLen[0]=128;
        startCodeSize = HUtils.startCodeSize(packet.array(),bufferInfo.offset, 5);
        ret = H265Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length, mXpsBuffer, xpsLen,H265Utils.Type_PPS, startCodeSize);
        if(ret>=0){
            int len = startCodeSize+ xpsLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
        }

        xpsLen[0]=128;
        startCodeSize = HUtils.startCodeSize(packet.array(),bufferInfo.offset, 5);
        ret = H265Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length, mXpsBuffer, xpsLen,H265Utils.Type_VPS, startCodeSize);
        if(ret>=0){
            int len = startCodeSize+ xpsLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
        }

        return offset;
    }

    protected void parseSps(byte[] buf,int nLen){
        if(mH264SPSPaser.h264_decode_seq_parameter_set(buf, nLen, mH264SPS1)){
            if(mH264SPS==null){
                mH264SPS = mH264SPS1.clone();
                int width = (mH264SPS.pic_width_in_mbs_minus1+1)*16;
                int height = (mH264SPS.pic_height_in_map_units_minus1+1)*16;
                DebugLog.info("width:"+width+";height:"+height);
            }else if(!mH264SPS.equals(mH264SPS1)){
                mH264SPS.copy(mH264SPS1);
                onSpsChanged(mH264SPS);
            }
        }
    }

    protected void onSpsChanged(H264SPS h264SPS){
        DebugLog.info("onSpsChanged");
        onEvent(Mp4Muxer.Event_SpsChanged,"sps changed",h264SPS);
    }

    private boolean checkKeyFrameAvaible(byte[] frame, int offset, int lenght){
        boolean hasPPS = false, hasSPS = false, hasIFrame = false, hasVps = false;
        int maxCheckLen = lenght;
        //PPS,SPS,IFrame头加起来通常不会超过100
        if(maxCheckLen > 200){
            maxCheckLen = 200;
        }
        for(int i=offset; i< maxCheckLen-4; i++){
            if(!hasPPS && frame[i] == 0 && frame[i+1] == 0 && frame[i+2]==0x1 && H265Utils.isNulaType(frame[i+3],H265Utils.Type_PPS)){
                hasPPS = true;
            }
            if(!hasSPS && frame[i] == 0 && frame[i+1] == 0 && frame[i+2]==0x1 && H265Utils.isNulaType(frame[i+3],H265Utils.Type_SPS)){
                hasSPS = true;
            }
            if(!hasIFrame && frame[i] == 0 && frame[i+1] == 0 && frame[i+2]==0x1 && H265Utils.isNulaType(frame[i+3],H265Utils.Type_VPS)){
                hasVps = true;
            }

            if(!hasIFrame && frame[i] == 0 && frame[i+1] == 0 && frame[i+2]==0x1 && H265Utils.isNulaType(frame[i+3],H265Utils.Type_IDR)){
                hasIFrame = true;
            }

            if(hasVps && hasPPS && hasSPS && hasIFrame){
                return true;
            }
        }
        return false;
    }

    public boolean hasAudio(){
        if(mMP4Config!=null){
            return mMP4Config.audioCodec!=null;
        }
        return false;
    }

    public void checkStart(){
        boolean start = hasAudio()?
                aacTrack>=0&&videoTrack>=0:
                videoTrack>=0;
        if(start){
            mFrameStarted = true;
            if(mFirstIFrameBuffer!=null){
                writeSample(videoTrack, mFirstIFrameBuffer.array(),
                        mFirstIFrameBufferInfo.offset, mFirstIFrameBufferInfo.length,
                        getVideoTime(mFirstIFrameBufferInfo.time,0), 1);
                mFirstIFrameBuffer.clear();
                mFirstIFrameBuffer = null;
                mFirstIFrameBufferInfo = null;
            }
        }
    }

    @Override
    public void release() {
        super.release();
        videoTrack =aacTrack = -1;
        mFrameStarted = false;
        mH264SPS = null;

        SystemUtils.close(fos);
    }

}
