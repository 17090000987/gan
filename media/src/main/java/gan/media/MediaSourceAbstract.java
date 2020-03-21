package gan.media;

import gan.log.DebugLog;
import gan.media.rtsp.RtspMediaServer;
import gan.media.rtsp.RtspMediaServerManager;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class MediaSourceAbstract extends MediaServer implements MediaSource{

    String mName;
    MediaSourceInfo mMediaSource;
    MediaOutputStreamRunnableList mOutputStreamRunnables;
    private boolean mOutputEmptyAutoFinish = false;
    private volatile boolean mIsInputing;

    @Override
    protected void onCreateSession(MediaSession session) {
        super.onCreateSession(session);
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        try{
            stopInput();
        }finally {
            clearMediaOutputStreamRunnableList();
        }
    }

    @Override
    public String getId() {
        if(mMediaSession!=null){
            return mMediaSession.getSessionId();
        }
        return null;
    }

    public void setName(String name) {
        this.mName = name;
    }

    @Override
    public MediaSourceInfo getMediaInfo() {
        return mMediaSource;
    }

    @Override
    public ByteBuffer getIFrame() {
        return null;
    }

    public void startInput(){
        if(isFinishing()){
            throw new IllegalStateException("server isFinishing");
        }
        if(mIsInputing){
            throw new IllegalStateException("is inputstreaming");
        }
        mIsInputing = true;
        MediaSourceInfo mediaSession = createMediaSourceSession();
        mMediaSource = mediaSession;
        onStartInput();
        for(MediaSesstionObserver observer:RtspMediaServerManager.getManagers(MediaSesstionObserver.class)){
            observer.onSourceSessionCreate(mediaSession);
        }
    }

    protected void onStartInput(){
    }

    public void stopInput(){
        mIsInputing = false;
        if(mMediaSource!=null){
            for(MediaSesstionObserver observer:RtspMediaServerManager.getManagers(MediaSesstionObserver.class)){
                observer.onSourceSessionDestory(mMediaSource);
            }
        }
    }

    public boolean isInputing() {
        return mIsInputing;
    }

    @Override
    public void addMediaOutputStreamRunnable(MediaOutputStreamRunnable runnable) {
        if(isFinishing()){
            return;
        }
        synchronized (this){
            if(mOutputStreamRunnables==null){
                mOutputStreamRunnables = new MediaOutputStreamRunnableList();
            }
        }
        synchronized (mOutputStreamRunnables){
            mOutputStreamRunnables.add(runnable);
            MediaOutputInfo outputInfo = (MediaOutputInfo) runnable.getMediaInfo();
            mMediaSource.addMediaOutputInfo(outputInfo);
            for(MediaSesstionObserver observer:RtspMediaServerManager.getManagers(MediaSesstionObserver.class)){
                observer.onOutputSessionCreate(mMediaSource,outputInfo);
            }
            notifyMediaOutputStreamRunnableChanged(mOutputStreamRunnables.size(), runnable.getPacketType(),
                    mOutputStreamRunnables.size(runnable.getPacketType()));
        }
    }

    @Override
    public void removeMediaOutputStreamRunnable(MediaOutputStreamRunnable runnable) {
        if(mOutputStreamRunnables!=null){
            synchronized (mOutputStreamRunnables){
                mOutputStreamRunnables.remove(runnable);
            }
            MediaOutputInfo outputInfo = (MediaOutputInfo) runnable.getMediaInfo();
            mMediaSource.removeMediaOutputInfo(outputInfo);
            for(MediaSesstionObserver observer:RtspMediaServerManager.getManagers(MediaSesstionObserver.class)){
                observer.onOutputSessionDestory(mMediaSource, outputInfo);
            }
            notifyMediaOutputStreamRunnableChanged(mOutputStreamRunnables.size(), runnable.getPacketType(),
                    mOutputStreamRunnables.size(runnable.getPacketType()));
        }
    }

    @Override
    public Collection<MediaOutputStreamRunnable> getAllMediaOutputStreamRunnable() {
        if(mOutputStreamRunnables!=null){
            return mOutputStreamRunnables.getAll();
        }
        return Collections.emptyList();
    }

    /**
     * packetType 定义了的转发数据类型 (rtp, h264 单数据包， 帧数据)
     * @param packetType {@link MediaOutputStreamRunnable}
     * @param channel // rtsp 通道
     * @param packet 数据
     * @param offset
     * @param len
     * @param time
     */
    public final void outputPacketStream(String packetType,byte channel, byte[] packet, int offset,int len,long time){
        synchronized (mOutputStreamRunnables){
            Collection<MediaOutputStreamRunnable> runnables = mOutputStreamRunnables.get(packetType);
            if(runnables.size()>0){
                List<MediaOutputStreamRunnable> errors = new ArrayList<>();
                for(MediaOutputStreamRunnable runnable:runnables){
                    try {
                        runnable.putPacket(channel,packet,offset,len,time);
                    } catch (Exception e) {
                        //ignore
                        DebugLog.debug("receiveRtpPacket forwardRtsp exception");
                        e.printStackTrace();
                        runnable.close();
                        errors.add(runnable);
                    }
                }
                mOutputStreamRunnables.removeAll(errors);
            }
        }
    }

    @Override
    public String getMediaCodec() {
        return null;
    }

    @Override
    public boolean hasAudio() {
        return false;
    }

    @Override
    public boolean capture(MediaCaptureCallBack captureCallBack) {
        return false;
    }

    protected void clearMediaOutputStreamRunnableList(){
        if(mOutputStreamRunnables!=null){
            synchronized (mOutputStreamRunnables){
                for(MediaOutputStreamRunnable runnable:mOutputStreamRunnables.getAll()){
                    runnable.close();
                }
                mOutputStreamRunnables.clear();
            }
        }
    }

    protected MediaSourceInfo createMediaSourceSession(){
        return new MediaSourceInfo(getId(), getUri(), mName==null? getUri(): mName);
    }

    protected void notifyMediaOutputStreamRunnableChanged(int newAllCount, String packetType, int packetTypeRunnableCount){
        try{
            for(RtspMediaServer.OnMediaOutputStreamRunnableChangedPlugin plugin: getPlugin(RtspMediaServer.OnMediaOutputStreamRunnableChangedPlugin.class)){
                plugin.onMediaOutputStreamRunnableChanged(newAllCount,packetType,packetTypeRunnableCount);
            }
        }finally {
            if(mOutputEmptyAutoFinish){
                if(newAllCount<=0){
                    DebugLog.info("no visit finish server session:"+getSessionId());
                    finish();
                }
            }
        }
    }

    public MediaSourceAbstract setOutputEmptyAutoFinish(boolean outputEmptyAutoFinish) {
        this.mOutputEmptyAutoFinish = outputEmptyAutoFinish;
        return this;
    }
}
