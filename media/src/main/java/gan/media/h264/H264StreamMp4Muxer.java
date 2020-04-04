package gan.media.h264;

import gan.log.DebugLog;
import gan.core.file.FileHelper;
import gan.media.MediaApplication;
import gan.core.system.SystemUtils;
import gan.core.system.server.SystemServer;
import gan.media.BufferInfo;
import gan.media.Media;
import gan.media.MediaConfig;
import gan.media.h26x.HUtils;
import gan.media.mp4.MP4StreamMuxer;
import gan.media.mp4.Mp4Muxer;
import gan.media.mp4.Mp4MuxerImpl;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class H264StreamMp4Muxer extends Mp4MuxerImpl implements MP4StreamMuxer {

    public final static int WRITE_MODE_ALL = 0;
    public final static int WRITE_MODE_FRAME=1;

    int mWriteMode=WRITE_MODE_FRAME;
    boolean mFrameStarted;
    boolean firstIFrame;
    int h264Track=-1,aacTrack=-1;
    ByteBuffer mFirstIFrameBuffer;
    BufferInfo mFirstIFrameBufferInfo;
    H264SPSPaser mH264SPSPaser;
    byte[] mSpsBuffer = new byte[128];
    int[] spsLen = new int[]{128};
    private Integer mEncoder;
    private MediaConfig mMP4Config;

    @Override
    public void init(MediaConfig config) {
        DebugLog.info("init");
        mMP4Config =config;
        super.init(config);
        mH264SPSPaser = new H264SPSPaser();
    }

    public H264StreamMp4Muxer setWriteMode(int writeMode) {
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
    boolean sps,pps;
    @Override
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo) throws IOException {
        if(!isRuning()){
            DebugLog.debug("muxer closed");
            return;
        }

        if(mFrameStarted){
            if(channel==0){
                if(HUtils.startCodeSize(packet.array(),bufferInfo.offset,5)<3){
                    DebugLog.info("data error do not h264 data 0001 001");
                    return;
                }

                if(H264Utils.isSPSOrPPS(packet.array(),bufferInfo.offset,40)){
                    firstIFrame = false;//sps pps 后边必须是IFrame (ps 传过来的中间有其他数据，chrome播放器会出问题)
                    if(!checkKeyFrameAvaible(packet.array(), bufferInfo.offset, 40)){
                        return;
                    }
                    offsetSpsPps(packet,bufferInfo);
                    if(firstIFrame=H264Utils.isIFrame(packet.array(), bufferInfo.offset , 5)){
                        setEncoder(1);
                        DebugLog.debug("write iFrame");
                        writeSample(h264Track, packet.array(), bufferInfo.offset, bufferInfo.length,
                                getVideoTime(bufferInfo.time,0),1);
                        return;
                    }else{
                        setEncoder(2);
                        while(bufferInfo.length>0){
                            int frameLen = HUtils.frameLen(packet.array(),bufferInfo.offset,bufferInfo.length);
                            if(firstIFrame = H264Utils.isIFrame(packet.array(), bufferInfo.offset, 5)){
                                writeSample(h264Track, packet.array(), bufferInfo.offset, bufferInfo.length,
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
                    writeH264FrameSample(packet,bufferInfo);
                }else{
                    writeH264Sample(packet,bufferInfo);
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
            DebugLog.debug("init channel:%s",channel);
            if(channel == 0){
                if(HUtils.startCodeSize(packet.array(),bufferInfo.offset,5)<3){
                    DebugLog.info("h264Track data error do not h264 data 0001 001");
                    return;
                }
                //if(mHasAudio? aacTrack>=0&&h264Track<0:h264Track<0) {
                if(h264Track<0){
                    if(checkKeyFrameAvaible(packet.array(),bufferInfo.offset,100)){
                        h264Track = addVideoTrack(packet.array(), bufferInfo.offset, bufferInfo.length,
                                getVideoTime(bufferInfo.time,0));
                        DebugLog.info("h264Track:"+h264Track);
                        offsetSpsPps(packet,bufferInfo);
                        mFirstIFrameBuffer = ByteBuffer.allocate(bufferInfo.length);
                        mFirstIFrameBuffer.put(packet.array(), bufferInfo.offset, bufferInfo.length);
                        mFirstIFrameBufferInfo = bufferInfo.clone();
                        mFirstIFrameBufferInfo.offset = 0;
                        firstIFrame = true;//test
                        checkStart();
                    }else{
                        DebugLog.debug("checkKeyFrameAvaible:false");

//                        if(SystemServer.IsDebug()){
//                            try{
//                                if(fos==null){
//                                    fos = FileHelper.createFileOutputStream(SystemServer.getRootPath("/logs/mp4"));
//                                }
//                                fos.write(packet.array(),bufferInfo.offset,bufferInfo.length);
//                                fos.flush();
//                            }catch (Exception e){
//                                e.printStackTrace();
//                            }
//                        }

                        while(bufferInfo.length>0){
                            int frameLen = HUtils.frameLen(packet.array(),bufferInfo.offset,bufferInfo.length);
                            int frameType = H264Utils.getFrameType(packet.array(), bufferInfo.offset, 10);
                            if((frameType==H264Utils.NAL_SPS)){
                                DebugLog.debug("cache sps");
                                mByteBufferTemp = ByteBuffer.allocate(MediaApplication.getMediaConfig().rtspFrameBufferSize);
                                mByteBufferTemp.put(packet.array(),bufferInfo.offset, frameLen);
                                int startCodeSize = HUtils.startCodeSize(mByteBufferTemp.array(),0,10);
                                if(startCodeSize>0){
                                    int startCodeIndex = HUtils.findStartCodeIndex(mByteBufferTemp.array(),0,10);
                                    int offset = startCodeIndex+startCodeSize;
                                    int spsLen = frameLen-offset;
                                    System.arraycopy(mByteBufferTemp.array(), offset, mSpsBuffer,0, spsLen);
                                    parseSps(mSpsBuffer, spsLen);
                                }else{
                                    parseSps(mByteBufferTemp.array(), frameLen);
                                }
                                sps = true;
                                pps = false;
                            }else if(sps&&(frameType==H264Utils.NAL_PPS)&&!pps){
                                DebugLog.debug("cache pps");
                                mByteBufferTemp.put(packet.array(),bufferInfo.offset, frameLen);
                                pps = true;
                            }else if(sps&&pps&&(frameType==H264Utils.NAL_SLICE_IDR)){
                                DebugLog.debug("cache iframe");
                                mByteBufferTemp.put(packet.array(),bufferInfo.offset, frameLen);
                                h264Track = addVideoTrack(mByteBufferTemp.array(), 0, mByteBufferTemp.position(),
                                        getVideoTime(bufferInfo.time,0));
                                mByteBufferTemp.clear();
                                mByteBufferTemp=null;
                                DebugLog.info("h264Track:"+h264Track);
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
                        if(isMPEG4(packet,bufferInfo.offset,bufferInfo.length)){
                            aacTrack = addAudioTrack(packet.array(), bufferInfo.offset, bufferInfo.length,
                                    getAudioTime(bufferInfo.time, 0));
                            DebugLog.info("aacTrack:"+aacTrack);
                            checkStart();
                        }else {
                            throw new UnsupportedEncodingException("不支持的音频编码格式");
                        }
                    }
                }
            }
        }
    }

    public boolean isMPEG4(ByteBuffer packet, int offset, int length){
        if((packet.getShort()&0xfff0) == 0xfff0){
            byte type = packet.get(offset+1);
            return (byte)(type&0x08)==0;
        }
        return false;
    }

    /**
     * 多帧数据使用，确定只有一个帧的数据请使用{@link #writeH264FrameSample(ByteBuffer, BufferInfo)}}
     * @param packet
     * @param bufferInfo
     */
    private void writeH264Sample(ByteBuffer packet, BufferInfo bufferInfo){
        while(bufferInfo.length>0){
            int frameLen = HUtils.frameLen(packet.array(),bufferInfo.offset,bufferInfo.length);
            writeH264FrameSample(packet,bufferInfo);
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

    private void writeH264FrameSample(ByteBuffer packet, BufferInfo bufferInfo){
        byte nula = HUtils.getNaluByte(packet.array(), bufferInfo.offset, 10);
        if(nula == H264Utils.NAL_UNKNOWN){
            DebugLog.warn("unknown nula form error frame data");
            return;
        }
        /**
         * forbidden_zero_bit ==1  这个值应该为0，当它不为0时，表示网络传输过程中，当前NALU中可能存在错误，
         * 解码器可以考虑不对这个NALU进行解码。
         */
        if((nula&0x80)==0x80){
            DebugLog.warn("nula forbidden_zero_bit=1 form error frame data");
            return;
        }

        int frameType = H264Utils.getNulaType(nula);
        writeH264FrameSample(packet,bufferInfo,frameType);
    }

    FileOutputStream fos;
    private void writeH264FrameSample(ByteBuffer packet, BufferInfo bufferInfo,int frameType){
        DebugLog.debug("bufferInfo:"+bufferInfo.toString());
        DebugLog.debug("frameType:"+frameType);
        if(frameType!=H264Utils.NAL_SLICE_IDR
                &&frameType!=H264Utils.NAL_SLICE
                &&frameType!=H264Utils.NAL_SPS
                &&frameType!=H264Utils.NAL_PPS){
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

        boolean iFrame = frameType == H264Utils.NAL_SLICE_IDR;
        if(firstIFrame){
            long videoTime = getVideoTime(bufferInfo.time,0);
            //h264.putInt(0,bufferInfo.length-4);//mp4
            writeSample(h264Track, packet.array(), bufferInfo.offset, bufferInfo.length,
                        videoTime,iFrame?1:0);
        }else{
            if(iFrame){
                long videoTime = getVideoTime(bufferInfo.time, 0);
                    //h264.putInt(0,bufferInfo.length-4);
                    writeSample(h264Track, packet.array(), bufferInfo.offset, bufferInfo.length,
                            videoTime,1);
                    firstIFrame = true;
            }
        }
    }

    H264SPS mH264SPS;
    H264SPS mH264SPS1=new H264SPS();
    private int offsetSpsPps(ByteBuffer packet, BufferInfo bufferInfo){
        int offset = 0;
        spsLen[0]=128;
        int startCodeSize = HUtils.startCodeSize(packet.array(),bufferInfo.offset,5);
        int ret = H264Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length, mSpsBuffer, spsLen,7, startCodeSize);
        if(ret>=0){
            int len = startCodeSize+ spsLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
            parseSps(mSpsBuffer, spsLen[0]);
        }
        spsLen[0]=128;
        startCodeSize = HUtils.startCodeSize(packet.array(),bufferInfo.offset, 5);
        ret = H264Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length, mSpsBuffer, spsLen,8, startCodeSize);
        if(ret>=0){
            int len = startCodeSize+ spsLen[0];
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
                onVideoSize(width,height);
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

    public void onVideoSize(int width,int height){
        DebugLog.info(String.format("width:%s;height:%s",width,height));
        onEvent(Mp4Muxer.Event_VideoSize,"video size", width, height);
    }

    private boolean checkKeyFrameAvaible(byte[] frame, int offset, int lenght){
        boolean hasPPS = false, hasSPS = false, hasIFrame = false;
        int maxCheckLen = lenght;
        //PPS,SPS,IFrame头加起来通常不会超过100
        if(maxCheckLen > 100){
            maxCheckLen = 100;
        }
        for(int i=offset; i< maxCheckLen-4; i++){
            if(!hasPPS && frame[i] == 0 && frame[i+1] == 0 && frame[i+2]==0x1 && frame[i+3] == 0x67){
                hasPPS = true;
            }
            if(!hasSPS && frame[i] == 0 && frame[i+1] == 0 && frame[i+2]==0x1 && frame[i+3] == 0x68){
                hasSPS = true;
            }
            if(!hasIFrame && frame[i] == 0 && frame[i+1] == 0 && frame[i+2]==0x1 && frame[i+3] == 0x65){
                hasIFrame = true;
            }
            if(hasPPS && hasSPS && hasIFrame){
                return true;
            }
        }
        return false;
    }

    private void addADTStoPacket(byte[] packet, int packetLen) {
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF1;
        packet[2] = (byte) (((2 - 1) << 6) + (11 << 2) + (1 >> 2));
        packet[3] = (byte) (((1 & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }

    public boolean hasAudio(){
        if(mMP4Config!=null){
            return mMP4Config.isAudioCodec(Media.MediaCodec.CODEC_AAC)
                    ||mMP4Config.isAudioCodec(Media.MediaCodec.CODEC_MPEG4_GENERIC);
        }
        return false;
    }

    public void checkStart(){
        boolean start = hasAudio()?
                aacTrack>=0&&h264Track>=0:
                h264Track>=0;
        if(start){
            mFrameStarted = true;
            DebugLog.debug("checkStart started");
            if(mFirstIFrameBuffer!=null){
                DebugLog.debug("write first iframe");
                writeSample(h264Track, mFirstIFrameBuffer.array(),
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
        h264Track =aacTrack = -1;
        mFrameStarted = false;
        mH264SPS = null;

        SystemUtils.close(fos);
    }

}
