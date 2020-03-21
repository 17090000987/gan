package gan.server.web.websocket;

import android.os.JSONObject;
import gan.core.system.SystemUtils;
import gan.core.system.server.BaseServer;
import gan.core.system.server.SystemServer;
import gan.core.utils.TextUtils;
import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.media.*;
import gan.media.file.MediaSourceFile;
import gan.media.h264.H264InterceptPacketListener;
import gan.media.mp4.Fmp4;
import gan.media.mp4.Mp4DataCallBack;
import gan.media.rtsp.RtspMediaServer;
import gan.media.rtsp.RtspMediaServerManager;
import gan.media.rtsp.RtspSource;
import gan.network.MapValueBuilder;
import gan.network.NetParamsMap;
import gan.web.base.Result;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class WebSocketServer extends BaseServer {

    final static String Tag = WebSocketServer.class.getName();
    final static String end = "\r\n";

    protected MediaSession mSession;
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
        DebugLog.info("onCreate:"+session.getId());
        mSession = new MediaSessionWebSocket(session);
        mSessionId  =  session.getId();
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        DebugLog.info("onDestory:"+mSessionId);
        SystemUtils.close(mSession);
        stopOutputStream();
    }

    protected void onMessage(String message)throws IOException{
        DebugLog.debug("session:"+mSessionId+",recevie message:"+message);
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

    public boolean isOutputStreaming(){
        return mOutputStreaming.get();
    }

    /**
     * 使用ffmpeg 拉流rtsp,输出为MP4
     * @throws IOException
     */
    public void rtsp2mp4()throws IOException{
        Fmp4 fmp4 = new Fmp4();
        fmp4.setMp4DataCallBack(new Mp4DataCallBack() {
            @Override
            public void onMp4(byte[] data, int length) {
                try {
                    mSession.sendMessage(data);
                } catch (IOException e) {
                    e.printStackTrace();
                    finish();
                }
            }
        });
        fmp4.rtsp2mp4(mToken,null);
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

        FileLogger logger = RtspMediaServerManager.getLogger(url);
        double ver = jo.optDouble("ver");
        PlayMessage playMessage = SystemUtils.safeJsonValues(jo.toString(),PlayMessage.class);
        MediaRequest request = MediaRequest.obtainRequest(url);
        try{
            request.parseFormJSONObject(jo);
            MediaSourceResult result = MediaServerManager.getInstance().getMediaSourceResult(request, playMessage.mediaType);
            if(MediaSourceResult.isError(result)){
                logger.log("startOutputStream fail no source url:%s",url);
                try {
                    sendMessage(result);
                } catch (IOException e) {
                    FileLogger.getExceptionLogger().log(e);
                }
                finish();
                return;
            }
            if(ver>0){
                try {
                    MediaSource source = result.mediaSource;
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
            SystemServer.executeThread(mOutputRunnale = new OutputRunnale(result.mediaSource, url,playMessage));
        }finally {
            request.recycle();
        }
    }

    private class OutputRunnale implements Runnable{

        String url;
        double ver;
        int mediaType;
        MediaSource mMediaSource;
        MediaOutputStreamRunnable mOutputStreamRunnable;
        AtomicBoolean start = new AtomicBoolean();
        FileLogger mLogger;

        public OutputRunnale(MediaSource source,String url,PlayMessage playMessage){
            this.mMediaSource = source;
            this.url = url;
            mLogger = RtspMediaServerManager.getLogger(url);
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
                    MediaOutputInfo mediaOutputSession = createMediaOutputSession(url);
                    if(mediaType==2){
                        mOutputStreamRunnable = new MediaOutputStreamRunnable1(
                                new MediaOutputStreamSession(mSession),
                                mediaOutputSession, 32768, MediaOutputStreamRunnable.PacketType_None);
                    }else{
                        MediaConfig mp4Config = getMediaConfig(ver, mMediaSource);
                        MediaOutputStream outputStream;
                        if(mp4Config.isVideoCodec(Media.MediaCodec.CODEC_H265)){
                            mLogger.log("isVideoCodec h265");
                            outputStream = new MediaOutputStreamSession(mSession);
                            MediaOutputStreamRunnableFrame outputStreamRunnable = new MediaOutputStreamRunnableFrame(
                                    outputStream, mediaOutputSession);
                            mOutputStreamRunnable = outputStreamRunnable;
                        }else{
                            mLogger.log("isVideoCodec h264");
                            outputStream = new WebSocketOutputStream(mMediaSource.getUri(), mSession, mp4Config);
                            MediaOutputStreamRunnableFrame outputStreamRunnable = new MediaOutputStreamRunnableFrame(
                                    outputStream, mediaOutputSession);
                            outputStreamRunnable.setInterceptPacketListener(new H264InterceptPacketListener());
                            mOutputStreamRunnable=outputStreamRunnable;
                        }
                    }
                }
                mMediaSource.addMediaOutputStreamRunnable(mOutputStreamRunnable);
                mOutputStreamRunnable.start();
            }catch (Throwable e){
                FileLogger.getExceptionLogger().log(e);
            }finally {
                mMediaSource.removeMediaOutputStreamRunnable(mOutputStreamRunnable);
                finish();
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
                                        if(tag!=null &&((Boolean)tag)){//回放
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
                FileLogger.getExceptionLogger().log(e);
            }
            return;
        }

        public synchronized void stopOutputStream(){
            start.set(false);
            if(mOutputStreamRunnable != null){
                mOutputStreamRunnable.close();
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
        mSession.sendMessage(text);
    }

    @Override
    public void sendMessage(int b) throws IOException {
        mSession.sendMessage(b);
    }

    @Override
    public void sendMessage(byte[] b) throws IOException {
        mSession.sendMessage(b);
    }

    @Override
    public void sendMessage(byte[] b, int off, int len) throws IOException {
        mSession.sendMessage(b, off, len);
    }

    public final static String formatWebsocketMediaSessionId(String id){
        return "session_websocket_"+id+UUID.randomUUID().toString();
    }

    public MediaOutputInfo createMediaOutputSession(String url){
        return new MediaOutputInfo(formatWebsocketMediaSessionId(mSessionId), url, url);
    }

    public static String parseRequest(String request, NetParamsMap params) throws IOException {
        BufferedReader sr = new BufferedReader(new StringReader(request));
        String str = "";
        String fun="";
        while (!TextUtils.isEmpty(str=sr.readLine())){
            if(str.startsWith("WSP")){
                fun = str.trim();
                continue;
            }
            if(str.contains(":")){
                String[] map = str.split(":");
                if(map.length>1){
                    params.put(map[0].trim(),map[1].trim());
                }
            }
        }
        return fun;
    }

    public int responseRequest(int code,String message)throws IOException{
        return responseRequest(mSession, code, message,null);
    }

    public int responseRequest(int code,String message,String content)throws IOException{
        return responseRequest(mSession, code, message,content);
    }

    public static int responseRequest(MediaSession session,int code,String message,String content)throws IOException{
        StringBuffer sb = new StringBuffer();
        String responseHead = "WSP/1.1 "+ code + " "+ code(code) + end;
        sb.append(responseHead)
                .append(message)
                .append(end);
        if(!TextUtils.isEmpty(content)){
            sb.append(content);
        }
        session.sendMessage(sb.toString());
        return code;
    }

    public static int responseRequest(WebSocketSession session,int code,String message,String content)throws IOException{
        StringBuffer sb = new StringBuffer();
        String responseHead = "WSP/1.1 "+ code + " "+ code(code) + end;
        sb.append(responseHead)
                .append(message)
                .append(end);
        if(!TextUtils.isEmpty(content)){
            sb.append(content);
        }
        session.sendMessage(new TextMessage(sb.toString()));
        return code;
    }

    public static String code(int code){
        switch (code){
            case 200:
                return "OK";
            case 400:
                return "400 means error";
            default:
                return "Error";
        }
    }

    public String getChannel(){
        return mSession.getSessionId();
    }

    protected void onHandleRequestINIT(String request,NetParamsMap params) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(SystemUtils.map2NetParams(new MapValueBuilder()
                .put("CSeq", params.get("CSeq"))
                .put("channel",getChannel())
                .build()));
        responseRequest(200, sb.toString());
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
        public int mediaType;

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
