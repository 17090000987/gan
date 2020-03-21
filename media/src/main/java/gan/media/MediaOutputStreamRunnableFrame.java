package gan.media;

import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.media.h264.H264Utils;
import gan.media.h26x.HUtils;
import gan.web.config.MediaConfig;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaOutputStreamRunnableFrame implements MediaOutputStreamRunnable {

    public final static int Status_Online = 0;
    public final static int Status_Offline = 1;
    private int mStatus = Status_Online;

    MediaOutputStream mOutputStream;
    Vector<PacketInfo> mRtspPacketInfos;
    private int mPacketBufferMaxCount=10;
    PacketInfoRecyclePool mByteBufferPool;
    private AtomicBoolean mColsed = new AtomicBoolean(false);
    MediaOutputInfo mMediaSession;
    protected String mPacketType;
    public long mSleepTime;
    InterceptPacketListener mInterceptPacketListener;

    public MediaOutputStreamRunnableFrame(MediaOutputStream out, MediaOutputInfo mediaSession){
        this(out,mediaSession,10, MediaApplication.getMediaConfig().rtspFrameBufferSize);
    }

    public MediaOutputStreamRunnableFrame(MediaOutputStream out, MediaOutputInfo mediaSession, int poolSize, int capacity){
        mOutputStream = out;
        mRtspPacketInfos = new Vector<>(poolSize);
        mByteBufferPool = new PacketInfoRecyclePool(poolSize,capacity);
        mPacketBufferMaxCount = poolSize;
        mMediaSession = mediaSession;
        mPacketType = MediaOutputStreamRunnable.PacketType_Frame;
    }

    public MediaOutputStreamRunnableFrame setPacketBufferMaxCount(int packetBufferMaxCount) {
        if(packetBufferMaxCount<1){
            throw new IllegalArgumentException("packetBufferMaxCount must>=1");
        }
        this.mPacketBufferMaxCount = packetBufferMaxCount;
        return this;
    }

    public MediaOutputStreamRunnableFrame setInterceptPacketListener(InterceptPacketListener interceptPacketListener) {
        this.mInterceptPacketListener = interceptPacketListener;
        return this;
    }

    public void setStatus(int status) {
        if(status!=mStatus){
            DebugLog.info(String.format("status change:%s",status));
        }
        this.mStatus = status;
    }

    @Override
    public MediaInfo getMediaInfo() {
        return mMediaSession;
    }

    @Override
    public String getPacketType() {
        return mPacketType;
    }

    public boolean isOffline(){
        return mStatus == Status_Offline;
    }

    private int index;
    private int mTempFrameRate;
    private int mFrameRate;
    private long videoOriginSampleTime = 0;
    private long videoCurSampleTime = -1;
    private long videoOffseSampelTime = -1;
    private long audioOriginSampleTime = 0;
    private long audioCurSampleTime = -1;
    private long audioOffseSampelTime = -1;
    public void putPacket(byte channel, byte[] packet, int offset,int len, long time){
        if(mColsed.get()){
            DebugLog.debug("Runnable isClosed");
            return;
        }

        updateSampleTime(channel,time);
        if(onInterceptPacket(channel,packet,offset,len,time)){
            DebugLog.debug("onInterceptPacket true");
            return;
        }

        if(isBufferLarge()){
            if (H264Utils.isSPS(packet,offset,len)
                    ||H264Utils.isIFrame(packet,offset,len)){
                DebugLog.info("bufferLarge");
            }else{
                return ;
            }
        }

        if(channel==0){
            if(isClientLargeBuffer()){
                if(H264Utils.isSPS(packet,offset,len)
                        ||H264Utils.isIFrame(packet,offset,len)){
                    mTempFrameRate = mFrameRate;
                    index =0;
                    putPacketInfo2(channel, packet, offset, len, time);
                    return;
                }
                if(mTempFrameRate>0){
                    if(index < mTempFrameRate){
                        index++;
                        putPacketInfo2(channel, packet, offset, len, time);
                    }
                }else{
                    putPacketInfo2(channel, packet, offset, len, time);
                }
            }else{
                putPacketInfo2(channel, packet, offset, len, time);
            }
        }else{
            putPacketInfo2(channel, packet, offset, len, time);
        }
    }

    private void updateSampleTime(byte channel,long time) {
        if(channel == 0) {
            if (videoOffseSampelTime<0 && videoOriginSampleTime > 0) {
                videoOffseSampelTime = time - videoOriginSampleTime;
            }
        }else {
            if (audioOffseSampelTime<0 && audioOriginSampleTime > 0) {
                audioOffseSampelTime = time - audioOriginSampleTime;
            }
        }
    }

    public long fixVideoSampleTimeOffset(long offset){
        if(offset>20000||offset<0){
            DebugLog.debug("fixVideoSampleTimeOffset offset:%s",offset);
            return 3600;
        }
        return offset;
    }

    private void putPacketInfo2(byte channel, byte[] packet, int offset,int len, long pts){
        //DebugLog.debug(String.format("channel:%s,offset:%s,len:%s,pts:%s", channel,offset,len,pts));
        if(channel==0){
            if (videoCurSampleTime < 0) { videoCurSampleTime = pts;}
            videoCurSampleTime += fixVideoSampleTimeOffset(videoOffseSampelTime);
            videoOriginSampleTime = pts;
            videoOffseSampelTime = -1;
            putPacketInfo(channel, packet, offset, len, videoCurSampleTime);
        }else{
            if (audioCurSampleTime < 0) { audioCurSampleTime = pts;}
            audioCurSampleTime += audioOffseSampelTime;
            audioOriginSampleTime = pts;
            audioOffseSampelTime = -1;
            putPacketInfo(channel, packet, offset, len, audioCurSampleTime);
        }
    }

    private void putPacketInfo(byte channel, byte[] packet, int offset,int len, long pts){
        PacketInfo rtspPacketInfo = mByteBufferPool.poll();
        try{
            System.arraycopy(packet, offset, rtspPacketInfo.mByteBuffer.array(), 0, len);
            rtspPacketInfo.length(len).offset(0).channel(channel).time(pts);
            mRtspPacketInfos.add(rtspPacketInfo);
        }catch (Exception e){
            e.printStackTrace();
            DebugLog.warn("putPacket e:"+rtspPacketInfo.toString());
        }
    }

    protected boolean onInterceptPacket(byte channel, byte[] packet, int offset,int len,long pts){
        if(isOffline()){
            return true;
        }

        if(channel==0){//video
            byte nalu = HUtils.getNaluByte(packet, offset, 10);
            /**
             * forbidden_zero_bit ==1  这个值应该为0，当它不为0时，表示网络传输过程中，当前NALU中可能存在错误，
             * 解码器可以考虑不对这个NALU进行解码。
             */
            if((nalu&0x80)==0x80){
                DebugLog.debug("nula forbidden_zero_bit=1 form error frame data");
                return true;
            }

            if(mInterceptPacketListener!=null){
                if(mInterceptPacketListener.onInterceptPacket(channel, packet, offset, len, pts)){
                    return true;
                }
            }
        }
        return false;
    }

    public PacketInfo getPacketInfo(){
        return mByteBufferPool.poll();
    }

    public void putPacketInfo(PacketInfo info){
        mRtspPacketInfos.add(info);
    }

    public void setColsed(boolean colsed) {
        this.mColsed.set(colsed);
    }

    public boolean isColsed() {
        return mColsed.get();
    }

    @Override
    public void start(){
        if(mColsed.get()){
            return;
        }
        run();
    }

    public void setSleepTime(double sleepTime){
        BigDecimal bg = new BigDecimal(sleepTime);
        double f1 = bg.setScale(3, BigDecimal.ROUND_HALF_UP).doubleValue();
        setSleepTime((long)(f1*1000));
    }

    long lastSleepTime;
    public void setSleepTime(long sleepTime) {
        long currentTime = System.currentTimeMillis();
        long timex = currentTime-lastSleepTime;
        if(timex>2000){//每隔2s更新一次帧率
            MediaConfig mediaConfig = MediaApplication.getMediaConfig();
            lastSleepTime = currentTime;
            this.mSleepTime = sleepTime;
            if(mSleepTime>=20000){
                mFrameRate = 1;
            }else if(mSleepTime>=8000){
                mFrameRate = mediaConfig.rtspMinFrameRate;
            }else if(mSleepTime>=6000){
                mFrameRate = mediaConfig.rtspMinFrameRate+2;
            }else if(mSleepTime>=4000){
                mFrameRate = mediaConfig.rtspMinFrameRate+4;
            }else {
                mFrameRate = 0;
            }
        }
    }

    @Override
    public void run() {
        try{
            DebugLog.info("out start");
            if(mColsed.get()){
                return;
            }
            mOutputStream.init();
            while (!mColsed.get()){
                if(!mRtspPacketInfos.isEmpty()){
                    PacketInfo rtspPacketInfo;
                    rtspPacketInfo = mRtspPacketInfos.remove(0);
                    ByteBuffer byteBuffer = rtspPacketInfo.mByteBuffer;
                    BufferInfo bufferInfo = rtspPacketInfo.mBufferInfo;
                    mOutputStream.write(bufferInfo.channel,byteBuffer,bufferInfo);
                    mByteBufferPool.recycle(rtspPacketInfo);
                }
                try{
                    Thread.sleep(1);
                }catch (Exception e){
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
            FileLogger.getExceptionLogger().log(e);
            DebugLog.warn(e.getMessage());
        }finally {
            mColsed.set(true);
            mOutputStream.close();
            for(PacketInfo rtspPacketInfo : mRtspPacketInfos){
                rtspPacketInfo.clear();
            }
            mRtspPacketInfos.clear();
            mByteBufferPool.release();
            DebugLog.info("thread end");
        }
    }

    public void close() {
        DebugLog.info("close");
        mColsed.set(true);
    }

    public boolean isBufferLarge(){
        return isBufferLarge(mPacketBufferMaxCount);
    }

    /**
     * 播放端通知服务，缓存过大，延迟大
     * @return
     */
    public boolean isClientLargeBuffer(){
        if(MediaApplication.getMediaConfig().rtspAutoFrameRate){
            return mSleepTime>0;
        }
        return false;
    }

    public boolean isBufferLarge(int count){
        return mRtspPacketInfos.size()>count;
    }

    public interface InterceptPacketListener{
        boolean onInterceptPacket(byte channel, byte[] packet, int offset, int len, long pts);
    }
}

