package gan.media;

import gan.log.DebugLog;
import gan.core.system.SystemUtils;

import java.nio.ByteBuffer;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

public class MediaOutputStreamRunnable1 implements MediaOutputStreamRunnable {

    final static String Tag = MediaOutputStreamRunnable1.class.getName();

    MediaOutputStream mOutputStream;
    Vector<PacketInfo> mRtspPacketInfos;
    private int mPacketBufferMaxCount = 5;
    PacketInfoRecyclePool mByteBufferPool;
    private AtomicBoolean mColsed = new AtomicBoolean(false);
    MediaOutputInfo mMediaInfo;
    protected String mPacketType;
    DiscardPacketInCacheMethod mDiscardPacketInCacheMethod;

    public MediaOutputStreamRunnable1(MediaOutputStream out, MediaOutputInfo mediaInfo, int capacity, String packetType){
        this(out,mediaInfo,20,capacity,packetType);
    }

    public MediaOutputStreamRunnable1(MediaOutputStream out, MediaOutputInfo mediaInfo, int poolSize, int capacity, String packetType){
        mOutputStream = out;
        mRtspPacketInfos = new Vector<>(poolSize);
        mByteBufferPool = new PacketInfoRecyclePool(poolSize,capacity);
        mPacketBufferMaxCount = poolSize;
        mMediaInfo = mediaInfo;
        if(packetType==null){
            throw new NullPointerException("packetType is disallow null");
        }
        mPacketType = packetType;
    }

    public MediaOutputStreamRunnable1 setPacketBufferMaxCount(int packetBufferMaxCount) {
        if(packetBufferMaxCount<1){
            throw new IllegalArgumentException("packetBufferMaxCount must>=1");
        }
        this.mPacketBufferMaxCount = packetBufferMaxCount;
        return this;
    }

    public MediaOutputStreamRunnable1 setDiscardPacketInCacheFuncation(DiscardPacketInCacheMethod discardPacketInCacheMethod) {
        this.mDiscardPacketInCacheMethod = discardPacketInCacheMethod;
        return this;
    }

    @Override
    public MediaInfo getMediaInfo() {
        return mMediaInfo;
    }

    @Override
    public String getPacketType() {
        return mPacketType;
    }

    public void putPacket(byte channel, byte[] packet, int offset,int len,long time){
        if(mColsed.get()){
            return;
        }

        if(isBufferLarge()){
            discardPacketInCache(mRtspPacketInfos,mByteBufferPool);
        }

        if(onInterceptPacket(channel,packet,offset,len,time)){
            DebugLog.debug("onInterceptPacket return");
            return;
        }

        if(isBufferLarge(1000)){//大于1000缓存太多了，是一种不健康的表现，服务器内存开销大
            DebugLog.info("session:"+ mMediaInfo.getId()+
                    "cahce>PacketBufferMaxCount:"+mRtspPacketInfos.size()+">"+mPacketBufferMaxCount);
            return;
        }

        PacketInfo rtspPacketInfo = mByteBufferPool.poll();
        try{
            System.arraycopy(packet, offset, rtspPacketInfo.mByteBuffer.array(), 0, len);
            rtspPacketInfo.length(len).offset(0).channel(channel).time(time);
            mRtspPacketInfos.add(rtspPacketInfo);
        }catch (Exception e){
            e.printStackTrace();
            DebugLog.warn(rtspPacketInfo.toString());
        }
    }

    protected boolean onInterceptPacket(byte channel, byte[] packet, int offset,int len,long time){
        return false;
    }

    protected void discardPacketInCache(Vector<PacketInfo> packetInfos,PacketInfoRecyclePool pool){
        onDiscardPacketInCache(packetInfos,pool);
    }

    protected boolean onDiscardPacketInCache(Vector<PacketInfo> packetInfos,PacketInfoRecyclePool pool){
        if(mDiscardPacketInCacheMethod !=null){
            mDiscardPacketInCacheMethod.onDiscardPacketInCache(packetInfos,pool);
        }
        return false;
    }

    public boolean isBufferLarge(){
        return isBufferLarge(mPacketBufferMaxCount);
    }

    public boolean isBufferLarge(int count){
        return mRtspPacketInfos.size()>count;
    }

    public PacketInfo getPacketInfo(){
        return mByteBufferPool.poll();
    }

    public void putPacketInfo(PacketInfo info){
        mRtspPacketInfos.add(info);
    }

    public boolean isColsed() {
        return mColsed.get();
    }

    @Override
    public void start(){
        mColsed.set(false);
        run();
    }

    @Override
    public void run() {
       try{
           DebugLog.info("session:"+ mMediaInfo.getId()+" out start");
           mOutputStream.init();
           while (!mColsed.get()){
               if(!mRtspPacketInfos.isEmpty()){
                   PacketInfo rtspPacketInfo = mRtspPacketInfos.remove(0);
                   ByteBuffer byteBuffer = rtspPacketInfo.mByteBuffer;
                   BufferInfo bufferInfo = rtspPacketInfo.mBufferInfo;
                   mOutputStream.write(bufferInfo.channel,byteBuffer,bufferInfo);
                   mByteBufferPool.recycle(rtspPacketInfo);
               }
               Thread.sleep(1);
           }
       }catch (Exception e) {
           DebugLog.debug("run e:"+SystemUtils.throwable2String(e));
       }finally {
           DebugLog.info("session:"+ mMediaInfo.getId()+"out finish");
           mOutputStream.close();
           mColsed.set(true);
           for(PacketInfo rtspPacketInfo : mRtspPacketInfos){
               rtspPacketInfo.clear();
           }
           mByteBufferPool.release();
       }
    }

    public void close(){
        mColsed.set(true);
    }

    public static interface DiscardPacketInCacheMethod {
        public void onDiscardPacketInCache(Vector<PacketInfo> packetInfos,PacketInfoRecyclePool pool);
    }
}
