package com.xbcx.decoder.web.websocket;

import android.os.JSONObject;
import com.xbcx.decoder.DecoderApplication;
import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.web.base.Result;
import gan.core.utils.TextUtils;
import gan.media.*;
import gan.media.file.MediaSourceFile;
import gan.media.rtsp.RtspDataDecoderPlugin;
import gan.media.rtsp.RtspMediaServer;
import gan.media.rtsp.RtspMediaServerManager;
import gan.media.rtsp.RtspSource;
import gan.core.system.SystemUtils;
import gan.core.system.server.BaseServer;
import gan.core.system.server.SystemServer;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketServer extends BaseServer {

    final static String end = "\r\n";

    protected WebSocketSession mSession;
    private String mSessionId;
    private String mToken;
    private AtomicBoolean mOutputStreaming = new AtomicBoolean();
    private OutputRunnale mOutputRunnale;

    @Override
    protected void onCreate(Object... parameter) {
        super.onCreate(parameter);
        onCreateSession((WebSocketSession) parameter[0]);
    }

    protected void onCreateSession(WebSocketSession session) {
        DebugLog.info("onCreate:%s",session.getId());
        mSession = session;
        mSessionId  =  session.getId();
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        SystemUtils.close(mSession);
        DebugLog.info("onDestory:%s", mSessionId);
        stopOutputStream();
    }

    protected void onMessage(String message)throws IOException{
        DebugLog.debug("session:%s,recevie message:%s",mSession.getId(),message);
        if(isOutputStreaming()){
            if(mOutputRunnale!=null){
                mOutputRunnale.control(message);
            }
        }

        try{
            JSONObject jo = new JSONObject(message);
            mToken = jo.optString("url");
            startOutputStream(mToken,jo);
        }catch (Exception e){
            mToken = message;
            startOutputStream(mToken,new JSONObject());
        }
    }

    public synchronized boolean isOutputStreaming(){
        return mOutputStreaming.get();
    }


    /**
     *解析帧数据合成MP4
     * @throws IOException
     */
    public synchronized void startOutputStream(String url,final JSONObject jo)throws IOException{
        if(isOutputStreaming()){
            return;
        }

        if(TextUtils.isEmpty(url)){
            DebugLog.info("token error");
            sendMessage(Result.error("参数错误"));
            finish();
            return ;
        }

        FileLogger logger = DecoderApplication.getLogger(url);

        MediaRequest request = MediaRequest.obtainRequest(url);
        try{
            double ver = jo.optDouble("ver");
            RtspMediaServer source = (RtspMediaServer) RtspMediaServerManager.getInstance().getRtspSourceByPull(request);
            if(source == null){
                logger.log("startOutputStream fail no source url:%s",url);
                try {
                    sendMessage(Result.error("没有找到数据源"));
                } catch (IOException e) {
                    FileLogger.getExceptionLogger().log(e);
                }
                finish();
                return;
            }
            RtspDataDecoderPlugin.singleInstance(source);
            if(ver>0){
                try {
                    sendMessage(new MessageResult()
                            .setMediacodec(source.getMediaCodec())
                            .asOk());
                } catch (IOException e) {
                    FileLogger.getExceptionLogger().log(e);
                    finish();
                    return;
                }
            }
            mOutputStreaming.set(true);
            PlayMessage playMessage = SystemUtils.safeJsonValues(jo.toString(),PlayMessage.class);
            SystemServer.executeThread(mOutputRunnale = new OutputRunnale(source, url,playMessage));
        }finally {
            request.recycle();
        }
    }

    private class OutputRunnale implements Runnable{

        String url;
        double ver;
        String mediaType;
        MediaSource mMediaSource;
        MediaOutputStreamRunnable mOutputStreamRunnable;
        AtomicBoolean start = new AtomicBoolean();
        FileLogger mLogger;

        public OutputRunnale(MediaSource source,String url,PlayMessage playMessage){
            this.mMediaSource = source;
            this.url = url;
            mLogger = DecoderApplication.getLogger(url);
            mLogger.log("OutputRunnale url:%s",url);
            if(playMessage!=null){
                ver = playMessage.ver;
                mediaType = playMessage.mediaType;
            }
            start.set(true);
        }

        public boolean isClosed(){
            return !start.get();
        }

        @Override
        public void run() {
            try{
                synchronized (this){
                    if(isClosed()){
                        mLogger.log("startOutputStream isClosed");
                        //如果在创建前被关闭了就不执行
                        return;
                    }
                    mLogger.log("startOutputStream url:%s",url);
                    MediaOutputInfo mediaOutputInfo = createMediaOutputSession(url);
                    if("video".equals(mediaType)){
                        mOutputStreamRunnable = new RawDataMediaOutputStreamRunnable(mediaOutputInfo,
                                new MediaSessionWebSocket(mSession));
                    }else{
                        mOutputStreamRunnable = new RawDataMediaOutputStreamRunnable(mediaOutputInfo,
                                new MediaSessionWebSocket(mSession));
                    }
                }
                mMediaSource.addMediaOutputStreamRunnable(mOutputStreamRunnable);
                mOutputStreamRunnable.start();
            }catch (Throwable e){
                FileLogger.getExceptionLogger().log(e);
            }
        }

        public synchronized void control(String message){
            try{
                if(start.get()){
                    if(mOutputStreamRunnable instanceof MediaOutputStreamRunnableFrame){
                        ControlMessage controlMessage = SystemUtils.safeJsonValues(message,ControlMessage.class);
                        if(controlMessage!=null){
                            if(!TextUtils.isEmpty(controlMessage.rtsp)){
                                if(mMediaSource instanceof RtspMediaServer){
                                    ((RtspMediaServer)mMediaSource).sendRtspRequest(controlMessage.rtsp);
                                }
                            }else{
                                MediaOutputStreamRunnableFrame runnableFrame = ((MediaOutputStreamRunnableFrame)mOutputStreamRunnable);
                                runnableFrame.setSleepTime(controlMessage.sleeptime);
                                /**
                                 * 设备视频回放
                                 */
                                if(mMediaSource instanceof RtspMediaServer){
                                    RtspMediaServer server = (RtspMediaServer) mMediaSource;
                                    if(server!=null){
                                        Object tag = server.getIdTag("replay");
                                        if(tag!=null
                                                &&((Boolean)tag)){//回放
                                        }
                                    }
                                }
                                //直播
                                runnableFrame.setStatus(controlMessage.status);
                            }
                        }
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
                DebugLog.info("message:%s", message);
            }
            return;
        }

        public synchronized void stopOutputStream(){
            start.set(false);
            if(mOutputStreamRunnable != null){
                mOutputStreamRunnable.close();
                mMediaSource.removeMediaOutputStreamRunnable(mOutputStreamRunnable);
            }
        }
    }

    public synchronized void stopOutputStream(){
        mOutputStreaming.set(false);
        if(mOutputRunnale!=null){
            mOutputRunnale.stopOutputStream();
        }
    }

    public MediaConfig getMediaConfig(double ver, MediaSource source){
        if(ver>0){
            if(source instanceof RtspSource){
                return MediaConfig.createConfig(((RtspSource)source).getSdp(),source.hasAudio());
            }else if(source instanceof MediaSourceFile){
                return new MediaConfigBuilder()
                        .setVCodec(Media.MediaCodec.CODEC_H265)
                        .build();
            }else{
                return MediaConfig.defaultConfig();
            }
        }
        return MediaConfig.createConfigH264();
    }

    public void sendMessage(Object object)throws IOException{
        sendMessage(SystemServer.getObjectMapper().writeValueAsString(object));
    }

    public void sendMessage(String text)throws IOException{
        mSession.sendMessage(new TextMessage(text));
    }

    @Override
    public void sendMessage(int b) throws IOException {
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
        mSession.sendMessage(new BinaryMessage(b));
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException {
        mSession.sendMessage(new BinaryMessage(ByteBuffer.wrap(b,off,len)));
    }

    public final static String formatWebsocketMediaSessionId(String id){
        return String.format("session_websocket_%s_%s",id, UUID.randomUUID().toString());
    }

    public MediaOutputInfo createMediaOutputSession(String url){
        return new MediaOutputInfo(formatWebsocketMediaSessionId(mSession.getId()), url, url);
    }

    private static class ControlMessage{
        public long sleeptime;
        public int status;
        public String rtsp;

        @Override
        public String toString() {
            return "status:"+status+",sleepTime:"+sleeptime;
        }
    }

    private static class PlayMessage{
        public String url;
        public double ver;
        public String mediaType;

        @Override
        public String toString() {
            return "PlayMessage{" +
                    "url='" + url + '\'' +
                    ", ver=" + ver +
                    ", mediaType='" + mediaType + '\'' +
                    '}';
        }
    }
}
