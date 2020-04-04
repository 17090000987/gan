package gan.media.rtsp;

import android.os.Handler;
import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.core.utils.TextUtils;
import gan.media.*;
import gan.media.h264.H264Utils;
import gan.media.parser.PSOverTcpStreamParser;
import gan.media.parser.RtpOverTcpStreamParser;
import gan.media.parser.StreamParser;
import gan.media.utils.MediaUtils;
import gan.network.MapValueBuilder;
import gan.network.NetParamsMap;
import gan.core.system.SystemUtils;
import gan.core.system.server.ServerListener;
import gan.core.system.server.SystemServer;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;

public class RtspMediaServer extends MediaServer implements RtspSource{

    final static String end = "\r\n";
    final static String charsetName = "UTF-8";

    private FileLogger mLogger;
    private String mSdpStr;
    private Sdp mSdp;
    private String mRtspUrl;
    private String mName;
    private String mMediaType = MediaType.ES;
    private volatile boolean mInputStreaming;
    private volatile boolean mOutputStreaming;
    private boolean mIsSource;
    private MediaSourceInfo mMediaSource;
    private boolean hasAudio = true;
    private boolean mOutputEmptyAutoFinish = false;
    StreamParser mStreamParser;
    RtspSdpParser mRtspSdpParser;
    RtpParserPlugin mRtpParserPlugin;
    private int mStartTimeOut = 60000;
    private int mRtpTimeOut;
    MediaOutputStreamRunnableList mOutputStreamRunnables;
    Runnable  mRtspSessionTimeOut = new Runnable() {
        @Override
        public void run() {
            DebugLog.warn("rtsp session timeout, url:%s",mRtspUrl);
            mLogger.log("rtsp session timeout url%s",mRtspUrl);
            finish();
        }
    };
    Runnable mRtpSessionTimeOut = new Runnable() {
        @Override
        public void run() {
            DebugLog.warn("rtp session timeout,url:%s",mRtspUrl);
            mLogger.log("rtp session timeout url%s",mRtspUrl);
            finish();
        }
    };

    @Override
    protected void finalize() throws Throwable {
        try{
            rtspSessionEnd();
            rtpSessionEnd();
            clearMediaOutputStreamRunnableList();
        }finally {
            super.finalize();
        }
    }

    protected void rtspSession(){
        Handler handler = SystemServer.getMainHandler();
        handler.removeCallbacks(mRtspSessionTimeOut);
        handler.postDelayed(mRtspSessionTimeOut, MediaApplication.getMediaConfig().rtspSessionTimeOut);
    }

    private void rtspSessionEnd(){
        Handler handler = SystemServer.getMainHandler();
        handler.removeCallbacks(mRtspSessionTimeOut);
    }

    protected void rtpSessionTimeOut(){
        sessionTimeOut(mRtpTimeOut);
    }

    public final void sessionTimeOut(long time){
        Handler handler = SystemServer.getMainHandler();
        handler.removeCallbacks(mRtpSessionTimeOut);
        handler.postDelayed(mRtpSessionTimeOut, time);
    }

    private void rtpSessionEnd(){
        Handler handler = SystemServer.getMainHandler();
        handler.removeCallbacks(mRtpSessionTimeOut);
    }

    @Override
    protected void onCreateSession(MediaSession session) {
        super.onCreateSession(session);
        mLogger = FileLogger.getInstance("/rtsp/info");
        mRtpTimeOut = MediaApplication.getMediaConfig().rtpSessionTimeOut;
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        try{
            stopInputStream();
            clearMediaOutputStreamRunnableList();
        }finally {
            rtspSessionEnd();
            rtpSessionEnd();
            mLogger.log("onDestory");
            SystemUtils.close(fos);
        }
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

    @Override
    public String getType() {
        return "rtsp";
    }

    @Override
    public void onSocketStream(InputStream is,OutputStream out)throws IOException {
        if(mInputStreaming){
            inputStream(is);
        }else{
            rtspSession();
            StringBuffer sb = new StringBuffer();
            BufferedReader sr = new BufferedReader(new InputStreamReader(is,charsetName));
            NetParamsMap params = new NetParamsMap();
            parseRequest(sr,sb,params);
            String request = sb.toString();
            mLogger.log("request:%s",request);

            if(request.startsWith("ANNOUNCE")){
                mIsSource = true;
                int content_len = Integer.valueOf(params.get("Content-Length"));
                onHanldeRequestANNOUNCE(request, params, parseContent(sr,content_len));
            }else if(request.startsWith("SETUP")){
                onHanldeRequestSETUP(request,params);
                sb = new StringBuffer();
                params.clear();
                parseRequest(sr,sb,params);
                request = sb.toString();
                mLogger.log("request2:%s",request);
                if(request.startsWith("SETUP")){
                    //SETUP不会等待第一个结果，2个连续的SETUP
                    onHanldeRequestSETUP(request,params);
                }else if(request.startsWith("PLAY")){
                    //有发现只传一个SETUP 后直接PLAY
                    if(onHanldeRequestPLAY(request,params) == RtspResponseCode.OK){
                        if(mIsSource){
                            startInputStream(is);
                        }else{
                            startOutputStream(out);
                        }
                    }
                }
            }else if(request.startsWith("PLAY")){
                if(onHanldeRequestPLAY(request,params) == RtspResponseCode.OK){
                    if(mIsSource){
                        startInputStream(is);
                    }else{
                        startOutputStream(out);
                    }
                }
            }else if(request.startsWith("OPTIONS")){
                mIsSource = false;
                onHanldeRequestOPTIONS(request,params);
            }else if(request.startsWith("DESCRIBE")){
                onHanldeRequestDESCRIBE(request,params);
            }else if(request.startsWith("TEARDOWN")){
                onHanldeRequestTEARDOWN(request,params);
            }else if(request.startsWith("PAUSE")){
                onHanldeRequestPAUSE(request,params);
            }else if(request.startsWith("RECORD")){
                if(onHanldeRequestRECORD(request,params) == RtspResponseCode.OK){
                    if(mIsSource){
                        startInputStream(is);
                    }else{
                        startOutputStream(out);
                    }
                }
            }
        }
    }

    public boolean isInputStreaming() {
        return mInputStreaming;
    }

    public boolean isOutputStreaming() {
        return mOutputStreaming;
    }

    private void parseRequest(BufferedReader sr, StringBuffer sb, NetParamsMap params) throws IOException {
        String str = "";
        while (!TextUtils.isEmpty(str=sr.readLine())){
            sb.append(str).append(end);
            if(str.contains("rtsp://")){
                continue;
            }
            if(str.contains(":")){
                String[] map = str.split(":");
                if(map.length>1){
                    params.put(map[0].trim(),map[1].trim());
                }
            }
        }
    }

    protected void onHanldeRequestANNOUNCE(String request, NetParamsMap params, String sdp) throws IOException {
        mLogger.log("sdp:%s",sdp);
        mSdp = getRtspSdpParser().parserSdp(mSdpStr=sdp,mSdp);

        BufferedReader br = null;
        try{
            br = new BufferedReader(new StringReader(request));
            mRtspUrl = parseUrl(br.readLine());
            mLogger.log("url:%s",mRtspUrl);
            mName = MediaUtils.parseName(mRtspUrl);
            mLogger.log("mName:%s",mName);
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            SystemUtils.close(br);
        }

        if(TextUtils.isEmpty(mRtspUrl)){
            responseRequest(RtspResponseCode.Internal_Server_Error,"parse url fail");
            finish();
            return;
        }

        if(RtspMediaServerManager.getInstance().containsRtspSource(mRtspUrl)){
            responseRequest(RtspResponseCode.Error_Not_Acceptable,mRtspUrl+":push stream has in server");
            finish();
            return;
        }

        mLogger.log("sdp:%s",sdp);
        String cseqStr = "CSeq:"+ params.get("CSeq") +end;
        responseRequest(RtspResponseCode.OK, cseqStr);
    }

    protected void onHanldeRequestSETUP(String request,NetParamsMap params) throws IOException {
        final String session = generateSession();

        if(TextUtils.isEmpty(mRtspUrl)){
            responseRequest(RtspResponseCode.Error_Not_Acceptable,"rtsp message," +
                    "please check DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS, ANNOUNCE, RECORD");
            finish();
            return;
        }

        final String date = new Date().toString();
        StringBuffer sb = new StringBuffer();
        sb.append(SystemUtils.map2NetParams(new MapValueBuilder()
                .put("CSeq",params.get("CSeq"))
                .put("Session", session)
                .put("Cache-Control","no-cache")
                .put("Date",date)
                .put("Expires",date)
                .put("Transport",params.get("Transport"))
                .build()));
        responseRequest(RtspResponseCode.OK, sb.toString());
    }

    protected int onHanldeRequestPLAY(String request,NetParamsMap params) throws IOException {
        if(TextUtils.isEmpty(mRtspUrl)){
            responseRequest(RtspResponseCode.Error_Not_Acceptable,"rtsp message," +
                    "please check DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS, ANNOUNCE, RECORD");
            finish();
            return RtspResponseCode.Internal_Server_Error;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(SystemUtils.map2NetParams(new MapValueBuilder()
                .put("CSeq", params.get("CSeq"))
                .put("Session", generateSession())
                .put("RTP-Info", "url="+mRtspUrl+",url="+mRtspUrl+mName)
                .build()));
        return responseRequest(RtspResponseCode.OK, sb.toString());
    }

    protected void onHanldeRequestOPTIONS(String request,NetParamsMap params) throws IOException {
        StringBuffer sb = new StringBuffer();
        sb.append(SystemUtils.map2NetParams(new MapValueBuilder()
                .put("CSeq", params.get("CSeq"))
                .put("Public","DESCRIBE, SETUP, TEARDOWN, PLAY, PAUSE, OPTIONS, ANNOUNCE, RECORD")
                .build()));
        responseRequest(RtspResponseCode.OK, sb.toString());
    }

    protected void onHanldeRequestDESCRIBE(String request,NetParamsMap params)throws IOException{
        BufferedReader br = null;
        try{
            br = new BufferedReader(new StringReader(request));
            mRtspUrl = parseUrl(br.readLine());
            mName = MediaUtils.parseName(mRtspUrl);
        }catch (Exception e){
            e.printStackTrace();
            DebugLog.warn("请求头不合法:"+e.getMessage());
        }finally {
            SystemUtils.close(br);
        }

        if(TextUtils.isEmpty(mRtspUrl)){
            responseRequest(RtspResponseCode.Error_Not_Acceptable,"parse url fail");
            finish();
            return;
        }

        MediaSource mediaSource;
        MediaRequest mediaRequest = MediaRequest.obtainRequest(mRtspUrl);
        try{
            mediaSource = MediaServerManager.getInstance().getRtspSource(mediaRequest,false);
            if(mediaSource==null){
                if(MediaUtils.isRtspURL(mName)){
                    mLogger.log("isRtspURL:%s", mName);
                    if(MediaUtils.isLocalURL(mName)){
                        mLogger.log("isLocalURL:%s", mName);
                    }else{
                        mediaRequest.setUrl(mName);
                        mediaSource = RtspMediaServerManager.getInstance().getRtspSourceByPull(mediaRequest);
                    }
                }
                if(mediaSource==null){
                    DebugLog.info("找不到数据源");
                    responseRequest(RtspResponseCode.Internal_Server_Error, "找不到数据源");
                    finish();
                    return;
                }
            }
        }finally {
            mediaRequest.recycle();
        }

        if(mediaSource instanceof RtspSource){
            RtspSource rtspSource = (RtspSource)mediaSource;
            String sdp = rtspSource.getOutputSdp();
            int content_len = sdp.getBytes(charsetName).length;
            final String date = new Date().toString();
            StringBuffer sb = new StringBuffer();
            sb.append(SystemUtils.map2NetParams(new MapValueBuilder()
                    .put("CSeq", params.get("CSeq"))
                    .put("Cache-Control","no-cache")
                    .put("Date", date)
                    .put("Expires",date)
                    .put("Content-Length",content_len)
                    .put("Content-type","application/sdp")
                    .put("x-Accept-Retransmit","our-retransmit")
                    .put("x-Accept-Dynamic-Rate",1)
                    .put("Content-Base",MediaUtils.parseUrl(mRtspUrl) +"/")
                    .build()));
            responseRequest(RtspResponseCode.OK, sb.toString(), sdp);
        }else{
            finish();
        }
    }

    protected void onHanldeRequestTEARDOWN(String request,NetParamsMap params) throws IOException {
        try{
            responseRequest(RtspResponseCode.OK,"rtsp teardown");
        }finally {
            finish();
        }
    }

    protected void onHanldeRequestPAUSE(String request,NetParamsMap params){

    }

    protected int onHanldeRequestRECORD(String request,NetParamsMap params) throws IOException {
       return onHanldeRequestPLAY(request,params);
    }

    public void setStartTimeOut(int startTimeOut) {
        this.mStartTimeOut = startTimeOut;
    }

    private void startInputStream(InputStream is)throws IOException{
        startInputStream(mRtspUrl, getInputSdp(), is);
    }

    public void startInputStream(String rtsp,String sdp,InputStream is) throws IOException{
        if(RtspMediaServerManager.getInstance().getRtspSource(rtsp)!=null){
            responseRequest(RtspResponseCode.Error_Not_Acceptable,"has rtsp:"+rtsp);
            finish();
            return;
        }
        try{
            startInputStream(rtsp, sdp);
            if(mInputStreaming
                    &&is!=null){
                inputStream(is);
            }
        }finally {
           stopInputStream();
           finish();
        }
    }

    protected StreamParser createStreamParser(String meidaType){
        mLogger.log("meidaType:%s",meidaType);
        if(MediaType.ES.equals(meidaType)){
            return new RtpOverTcpStreamParser(1500, new RtpOverTcpStreamParser.PacketListener() {
                @Override
                public void onTcpPacket(byte channel, ByteBuffer packet, int offset, int length) {
                    rtpSessionTimeOut();
                    receiveRtpPacket(channel,packet,offset,length);
                }
            });
        } else if (MediaType.PS.equals(meidaType)) {
            registerPlugin(new RtspFrame2RtpPlugin());
            return new PSOverTcpStreamParser(new PSOverTcpStreamParser.FrameListener() {
                @Override
                public void onFrame(byte channel, byte[] frame, int offset, int length, long timeSample) {
                    putFrame(channel, frame, offset, length, timeSample);
                }
            });
        }
        return null;
    }

    public RtspSdpParser getRtspSdpParser(){
        if(mRtspSdpParser==null){
            mRtspSdpParser = new RtspSdpParser();
        }
        return mRtspSdpParser;
    }

    public synchronized void startInputStream(String rtsp,String sdp){
        rtspSessionEnd();
        if(isFinishing()){
            throw new IllegalStateException("server isFinishing");
        }
        if(mInputStreaming){
            throw new IllegalStateException("is inputstreaming");
        }
        mRtspUrl = rtsp;
        mName = MediaUtils.parseName(rtsp);
        if(!TextUtils.isEmpty(sdp)){
            try {
                mSdp = getRtspSdpParser().parserSdp(mSdpStr = sdp, mSdp);
                mLogger.log(mSdp.toString());
                mMediaType = getMediaTypeBySdp();
                String audioCodec = mSdp.aCodec;
                if(!TextUtils.isEmpty(audioCodec)){
                    //如果语音是MPEG4-GENERIC开启语音
                    setHasAudio(lookupAudioCodec(audioCodec));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mStreamParser = createStreamParser(mMediaType);
        mRtpParserPlugin = RtpParserPlugin.get(this);
        mRtpParserPlugin.initParser(mSdp);
        MediaSourceInfo mediaSession = createMediaSourceSession();
        mMediaSource = mediaSession;
        try {
            RtspMediaServerManager.getInstance().managerRtspSource(mRtspUrl,this);
        } catch (Exception e) {
            e.printStackTrace();
            DebugLog.warn(e.getMessage());
            finish();
            return;
        }
        mLogger.log("startInputStream sdp:%s\r\n", sdp);
        sessionTimeOut(mStartTimeOut);//等待数据一分钟超时
        mInputStreaming = true;
        for(MediaSesstionObserver observer:RtspMediaServerManager.getManagers(MediaSesstionObserver.class)){
            observer.onSourceSessionCreate(mediaSession);
        }
    }

    public void setStreamParser(StreamParser streamParser) {
        this.mStreamParser = streamParser;
    }

    public void setRtspSdpParser(RtspSdpParser rtspSdpParser) {
        this.mRtspSdpParser = rtspSdpParser;
    }

    public String getMediaTypeBySdp(){
        if(mSdp!=null
                ||mSdpStr!=null){
            String videoInfo = getRtspSdpParser().getMediaLineByKey("m=video","a=rtpmap");
            if(videoInfo.contains(" ")){
                try{
                    String[] infos = videoInfo.split(" ");
                    String mediaType = MediaType.findMediaType(infos[1].toLowerCase().split("/")[0]);
                    if (TextUtils.isEmpty(mediaType)) {
                        String server = mRtspSdpParser.getValue("s=");
                        if(server.contains("hikvision")){
                            return MediaType.PS;
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
        return MediaType.ES;
    }

    /**
     * 检查语音编码，是否需要开启语音
     * @param audioCodec 现在只支持语音MPEG4-GENERIC编码格式
     * @return
     */
    protected boolean lookupAudioCodec(String audioCodec){
        if(!TextUtils.isEmpty(audioCodec)){
            return audioCodec.startsWith("MPEG4-GENERIC");
        }
        return false;
    }

    public void putInputStream(InputStream is) throws IOException {
        try{
            inputStream(is);
        }finally {
            stopInputStream();
            finish();
        }
    }

    protected MediaSourceInfo createMediaSourceSession(){
        return new MediaSourceInfo(getId(), getUri(),mName);
    }

    public MediaOutputInfo createMediaOutputSession(){
        return new MediaOutputInfo(getId(),mRtspUrl,mName);
    }

    public synchronized void stopInputStream(){
        DebugLog.info("stopInputStream rtspUrl:"+mRtspUrl);
        try{
            mInputStreaming = false;
            if(mStreamParser!=null){
                mStreamParser.stop();
            }
            rtpSessionEnd();
            onStreamStop();
        }finally {
            if(mMediaSource!=null){
                for(MediaSesstionObserver observer:RtspMediaServerManager.getManagers(MediaSesstionObserver.class)){
                    observer.onSourceSessionDestory(mMediaSource);
                }
                if(!TextUtils.isEmpty(mRtspUrl)){
                    RtspMediaServerManager.getInstance().removeRtspSource(mRtspUrl);
                }
            }
        }
    }

    @Override
    public ByteBuffer getIFrame() {
        return null;
    }

    /**
     * 阻塞
     * @param outputStream
     */
    protected void startOutputStream(OutputStream outputStream)throws IOException{
        rtspSessionEnd();
        DebugLog.info(mRtspUrl+":"+"startOutputStream");
        MediaSource mediaSource = MediaServerManager.getInstance().getRtspSource(mRtspUrl);
        if(mediaSource == null){
            finish();
            return;
        }

        mOutputStreaming = true;
        MediaOutputStreamRunnable runnable = null;
        try{
            MediaOutputInfo mediaOutputSession =createMediaOutputSession();
            runnable = new RtspOutputStreamRunnable(new RtspTcpOutputStream(outputStream),mediaOutputSession);
            mediaSource.addMediaOutputStreamRunnable(runnable);
            runnable.start();//阻塞
        }finally {
            if(runnable!=null){
                runnable.close();
                mediaSource.removeMediaOutputStreamRunnable(runnable);
                mOutputStreaming = false;
                finish();
            }
        }
    }

    public String getUrl() {
        return mRtspUrl;
    }

    @Override
    public String getId() {
        String sessionId = getSessionId();
        if(!TextUtils.isEmpty(sessionId)){
            return sessionId;
        }
        return mRtspUrl;
    }

    @Override
    public String getUri() {
        return mRtspUrl;
    }

    @Override
    public MediaSourceInfo getMediaInfo() {
        return mMediaSource;
    }


    @Override
    public Sdp getSdp() {
        return mSdp;
    }

    @Override
    public String getInputSdp() {
        if(mSdp!=null){
            return mSdp.sdp;
        }
        return mSdpStr;
    }

    @Override
    public String getOutputSdp() {
        if(hasAudio()){
            if(mSdp!=null){
                if(!TextUtils.isEmpty(mSdp.sdp)){
                    return mSdp.sdp;
                }else {
                    return mSdpStr;
                }
            }
            return Sdp.SDP;
        }else{
            return Sdp.SDP_OnlyVideo;
        }
    }

    @Override
    public boolean hasAudio() {
        return hasAudio;
    }

    @Override
    public void addMediaOutputStreamRunnable(MediaOutputStreamRunnable runnable){
        if(runnable==null){
            throw new IllegalArgumentException("runnable null");
        }
        synchronized (this){
            if(mOutputStreamRunnables==null){
                mOutputStreamRunnables = new MediaOutputStreamRunnableList();
            }
        }
        synchronized (mOutputStreamRunnables){
            if(isFinishing()){
                throw new IllegalStateException("server finishing");
            }
            mOutputStreamRunnables.add(runnable);
            if(mMediaSource != null){
                MediaOutputInfo outputInfo = (MediaOutputInfo) runnable.getMediaInfo();
                mMediaSource.addMediaOutputInfo(outputInfo);
                for(MediaSesstionObserver observer:RtspMediaServerManager.getManagers(MediaSesstionObserver.class)){
                    observer.onOutputSessionCreate(mMediaSource,outputInfo);
                }
            }
            notifyMediaOutputStreamRunnableChanged(mOutputStreamRunnables.size(), runnable.getPacketType(),
                    mOutputStreamRunnables.size(runnable.getPacketType()));
        }
    }

    @Override
    public void removeMediaOutputStreamRunnable(MediaOutputStreamRunnable runnable){
        if(runnable==null){
            return;
        }
        if(mOutputStreamRunnables!=null){
            synchronized (mOutputStreamRunnables){
                mOutputStreamRunnables.remove(runnable);
                if(mMediaSource != null){
                    MediaOutputInfo outputInfo = (MediaOutputInfo) runnable.getMediaInfo();
                    mMediaSource.removeMediaOutputInfo(outputInfo);
                    for(MediaSesstionObserver observer:RtspMediaServerManager.getManagers(MediaSesstionObserver.class)){
                        observer.onOutputSessionDestory(mMediaSource, outputInfo);
                    }
                }
                notifyMediaOutputStreamRunnableChanged(mOutputStreamRunnables.size(), runnable.getPacketType(),
                        mOutputStreamRunnables.size(runnable.getPacketType()));
            }
        }
    }

    @Override
    public Collection<MediaOutputStreamRunnable> getAllMediaOutputStreamRunnable() {
        if(mOutputStreamRunnables!=null){
            return mOutputStreamRunnables.getAll();
        }
        return Collections.emptyList();
    }

    public int getOutputSize(){
        if(mOutputStreamRunnables != null){
            return mOutputStreamRunnables.size();
        }else{
            return 0;
        }
    }

    public void setHasAudio(boolean hasAudio) {
        if(this.hasAudio){
            this.hasAudio = hasAudio;
        }
    }

    @Override
    public String getMediaCodec() {
        if(hasAudio){
            if (mSdp.isVCodec(Media.MediaCodec.CODEC_H264)) {
                return "video/mp4; codecs="+"avc1.42E01E, mp4a.40.2";
            }
            return "v:"+mSdp.vCodec+",a:"+mSdp.aCodec;
        }else{
            if (mSdp.isVCodec(Media.MediaCodec.CODEC_H264)) {
                return "video/mp4; codecs="+"avc1.42E01E";
            }
            return "v:"+mSdp.vCodec;
        }
    }

    public RtspMediaServer setOutputEmptyAutoFinish(boolean outputEmptyAutoFinish) {
        this.mOutputEmptyAutoFinish = outputEmptyAutoFinish;
        return this;
    }

    protected void notifyMediaOutputStreamRunnableChanged(int newAllCount, String packetType, int packetTypeRunnableCount){
        try{
            for(OnMediaOutputStreamRunnableChangedPlugin plugin: getPlugin(OnMediaOutputStreamRunnableChangedPlugin.class)){
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

    protected void inputStream(InputStream is) throws IOException{
        if(isFinishing()){
            return;
        }
        try{
            onStreamStart();
            rtpSessionTimeOut();
            mStreamParser.start();
            mStreamParser.inputStream(is);
        }finally {
            mStreamParser.stop();
        }
    }

    protected void onStreamStart(){
        for(OnStreamStateListenerPlugin plugin:getPlugin(OnStreamStateListenerPlugin.class)){
            plugin.onStreamStarted();
        }
    }

    protected void onStreamStop(){
        for(OnStreamStateListenerPlugin plugin:getPlugin(OnStreamStateListenerPlugin.class)){
            plugin.onStreamStop();
        }
    }

    public boolean isVideoPlayLoad(int playLoad){
        if(mSdp.vPlayLoad>=0){
            return playLoad==mSdp.vPlayLoad;
        }
        return playLoad == 96;
    }

    public boolean isAudioPlayLoad(int playLoad){
        if(mSdp.aPlayLoad>=0){
            return playLoad==mSdp.aPlayLoad;
        }
        return playLoad == 97;
    }

    protected void receiveRtpPacket(byte channel,ByteBuffer packet,int offset,int length){
        //先把数据转发给各个输出通道
        outputPacketStream(MediaOutputStreamRunnable.PacketType_Rtp, channel,packet.array(),offset,length, 0);

        int playLoad = packet.get(offset+1)&0x7f;
        boolean isVideoPacket = isVideoPlayLoad(playLoad);
        channel = isVideoPacket? (byte)0:channel;
        //下边解析音视频帧数据(测试时间是很快的，对转发rtp数据影响很小)
        if(isVideoPacket){//h264
//            //解析视频帧，缓存IFrame
            mRtpParserPlugin.parseVideoFrame(channel, packet, offset, length);
        }else if(isAudioPlayLoad(playLoad)){
            if(hasAudio()){
                mRtpParserPlugin.parseAudioFrame(channel, packet, offset, length);
            }
        }
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
        if(mOutputStreamRunnables!=null){
            synchronized (mOutputStreamRunnables){
                Collection<MediaOutputStreamRunnable> runnables = mOutputStreamRunnables.get(packetType);
                if(runnables.size()>0){
                    List<MediaOutputStreamRunnable> errors = new ArrayList<>();
                    for(MediaOutputStreamRunnable runnable:runnables){
                        try {
                            runnable.putPacket(channel,packet,offset,len,time);
                        } catch (Exception e) {
                            //ignore
                            DebugLog.warn("receiveRtpPacket forwardRtsp exception");
                            e.printStackTrace();
                            runnable.close();
                            errors.add(runnable);
                        }
                    }
                    mOutputStreamRunnables.removeAll(errors);
                }
            }
        }
    }

    /**
     * 处理去掉了rtp头的 媒体流数据包
     * @param channel
     * @param packet
     */
    protected void receivePacket(byte channel,byte[] packet,int offset,int length,long time){
        outputPacketStream(MediaOutputStreamRunnable.PacketType_None,channel,packet,offset, length,time);
    }

    BufferInfo frameInfo;
    public void putFrame(byte channel,byte[] data,int offset,int length,long time){
        if(isFinishing()){
            throw new IllegalStateException("server is finishing");
        }
        if(frameInfo==null){
            frameInfo= new BufferInfo();
        }
        frameInfo.reset();
        frameInfo.setTime(time).setOffset(offset).setLength(length);
        putFrame(channel, ByteBuffer.wrap(data), frameInfo);
    }

    public void putFrame(byte channel,ByteBuffer frame,BufferInfo frameInfo){
        if(isFinishing()){
            throw new IllegalStateException("server is finishing");
        }
        onFrame(channel, frame, frameInfo);
    }

    FileOutputStream fos;
    protected void onFrame(byte channel,ByteBuffer frame,BufferInfo frameInfo){
        DebugLog.debug("onFrame: channel:%s",channel);
        rtpSessionTimeOut();

        if(onInterceptFrame(channel,frame,frameInfo)){
            DebugLog.debug("onInterceptFrame channel:%s",channel);
            return;
        }

//        if(GanServer.getGan().isDebug()){
//            try{
//                if(fos==null){
//                    fos = FileHelper.createFileOutputStream(SystemServer.getRootPath("/media/frames"+index++));
//                }
//                fos.write(frame.array(),frameInfo.offset,frameInfo.length);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }

        for(OnFrameCallBackPlugin plugin: getPlugin(OnFrameCallBackPlugin.class)){
            plugin.onFrame(channel,frame,frameInfo);
        }
        outputPacketStream(MediaOutputStreamRunnable.PacketType_Frame,channel, frame.array(),
                frameInfo.offset, frameInfo.length, frameInfo.time);
    }

    protected boolean onInterceptFrame(byte channel,ByteBuffer frame,BufferInfo frameInfo){
        int frameType = H264Utils.getFrameType(frame.array(), frameInfo.offset, frameInfo.length);
        DebugLog.debug("frameType%s",frameType);
        if(!hasAudio){
            return channel!=0;
        }
        return false;
    }

    private String generateSession(){
        return System.currentTimeMillis()+"_"+RtspMediaServerManager.getInstance().sourceCount();
    }

    public int responseRequest(int code,String message)throws IOException{
        return responseRequest(code,message,null);
    }

    public int responseRequest(int code,String message,String content)throws IOException{
        StringBuffer sb = new StringBuffer();
        String responseHead = "RTSP/1.0 "+ code + " "+ code(code) + end;
        final String server = "Server:"+"Gan/1.0"+end;
        sb.append(responseHead)
        .append(server)
        .append(message)
        .append(end);
        if(!TextUtils.isEmpty(content)){
            sb.append(content);
        }
        sendMessage(sb.toString());
        return code;
    }

    public void sendMessage(String message)throws IOException{
        sendMessage(message.getBytes(charsetName));
    }

    public String code(int code){
        switch (code){
            case RtspResponseCode.OK:
                return "OK";
            case RtspResponseCode.Error_Method_Not_Allowed:
                return "Internal_Server_Error Method Not Allowed";
            case RtspResponseCode.Error_Not_Acceptable:
                return "Error_Not_Acceptable";
            default:
                return "Internal_Server_Error";
        }
    }

    public String parseUrl(String method){
        return method.split(" ")[1];
    }

    public String parseContent(BufferedReader br,int content_len) throws IOException{
        StringBuffer sb = new StringBuffer();
        int len=0;
        while (len<content_len){
            sb.append(br.readLine()).append(end);
            len=sb.toString().getBytes(charsetName).length;
        }
        return sb.toString();
    }

    public String sendRtspRequest(String rtsp){
        if(mMediaSession instanceof RtspConnectionMediaSession){
            try{
                ((RtspConnectionMediaSession)mMediaSession).sendRtspRequest(rtsp);
            }catch (Exception e){
                e.printStackTrace();
            }
        }else{
            //undo
        }
        return null;
    }

    public RtspMediaServer setRtpTimeOut(int rtpTimeOut) {
        this.mRtpTimeOut = rtpTimeOut;
        return this;
    }

    @Override
    public boolean capture(final MediaCaptureCallBack captureCallBack) {
        registerPlugin(new RtspCapturePlugin(captureCallBack));
        return true;
    }

    @Override
    public String toString() {
        return "RtspMediaServer{" +
                "id='" + getId() + '\'' +
                "url=" + mRtspUrl + "\'"+
                "mSdpStr='" + mSdpStr + '\'' +
                '}';
    }

    public static interface OnFrameCallBackPlugin extends ServerListener{
        public void onFrame(byte channel,ByteBuffer frame, BufferInfo bufferInfo);
    }

    public static interface OnMediaOutputStreamRunnableChangedPlugin extends  ServerListener{
        public void onMediaOutputStreamRunnableChanged(int newAllCount,String packetType,int packetTypeRunnableCount);
    }

    public static interface OnStreamStateListenerPlugin extends ServerListener{
        public void onStreamStarted();
        public void onStreamStop();
    }

}
