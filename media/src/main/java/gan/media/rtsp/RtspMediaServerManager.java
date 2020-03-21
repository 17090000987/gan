package gan.media.rtsp;

import gan.core.*;
import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.core.utils.TextUtils;
import gan.media.*;
import gan.media.utils.MediaUtils;
import gan.network.SocketGroupManager;
import gan.network.SocketListener;
import gan.core.system.SystemUtils;
import gan.core.system.server.SystemServer;
import gan.web.config.MediaConfig;

import java.io.IOException;
import java.net.Socket;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RtspMediaServerManager implements BaseListener,SocketListener{

    static {
        sInstance = new RtspMediaServerManager();
    }

    private static RtspMediaServerManager sInstance;

    public static RtspMediaServerManager getInstance() {
        return sInstance;
    }

    private SocketGroupManager mSocketGroupManager;
    private ConcurrentHashMap<MediaSession, RtspMediaServer> mMediaSessionServerMap;
    private ConcurrentHashMap<Object,MediaSession<?>> mSessions;
    private AtomicInteger  mSessionCount = new AtomicInteger(0);
    private HashMap<String, RtspSource> mMapRtspSource;
    private Object sourceLock = new Object();
    private AtomicInteger  mSourceCount = new AtomicInteger(0);
    MediaServerManager mMediaServerManager = MediaServerManager.getInstance();
    private static PluginHelper<MediaListener> pluginHelper = new SyncPluginHelper<MediaListener>();
    private static FileLogger mLogger = FileLogger.getInstance("/rtsp/info");

    private RtspMediaServerManager(){
        final MediaConfig config = MediaApplication.getMediaConfig();
        try {
            DebugLog.info("rtspServer start");
            mMediaSessionServerMap = new ConcurrentHashMap<>(config.rtspMaxConnection);
            mSessions = new ConcurrentHashMap<>();
            mSocketGroupManager = new SocketGroupManager(Integer.valueOf(config.rtspPort), this);
            mSocketGroupManager.start();
            mMapRtspSource = new HashMap<>();
        } catch (IOException e) {
            e.printStackTrace();
            DebugLog.warn("RtspServer start fail:"+e.getMessage());
            return;
        }
    }

    public void initServer(){
        addManager(new MediaEventListener());
    }

    public static FileLogger getLogger() {
        return mLogger;
    }

    public void destory(){
        onDestory();
    }

    protected void onDestory(){
        if(mSocketGroupManager !=null){
            mSocketGroupManager.close();
        }
        for(MediaListener listener:pluginHelper.getManagers(MediaListener.class)){
            removeManager(listener);
        }
    }

    @Override
    public void onSocketCreate(Socket socket) {
        DebugLog.info( "onSocketCreate :"+socket.getInetAddress().getHostAddress()+",port:"+socket.getPort());
        MediaSessionSocket sessionSocket = new MediaSessionSocket(socket);
        addSession(sessionSocket);
    }

    @Override
    public void onSocketStream(Socket socket) throws IOException {
        DebugLog.info( "onSocketStream:"+socket.getInetAddress().getHostAddress()+",port:"+socket.getPort());
        mMediaSessionServerMap.get(mSessions.get(socket)).onSocketStream(socket.getInputStream(),socket.getOutputStream());
    }

    @Override
    public void onSocketClosed(Socket socket) {
        DebugLog.info( "onSocketClosed"+socket.getInetAddress().getHostAddress()+",port:"+socket.getPort());
        removeSession(socket);
    }

    public RtspMediaServer addSession(MediaSession session){
        if(session == null){
            throw new IllegalArgumentException("session error");
        }
        RtspMediaServer server = SystemServer.startServer(RtspMediaServer.class,session);
        MediaConfig config = MediaApplication.getMediaConfig();
        if(mMediaSessionServerMap.size()>=config.rtspMaxConnection){
            SystemServer.executeThread(new Runnable() {
                @Override
                public void run() {
                    try{
                        server.responseRequest(RtspResponseCode.Internal_Server_Error,"connection is full disallow more");
                    }catch (Exception e){
                        e.printStackTrace();
                    }finally {
                        server.finish();
                    }
                }
            });
            return null;
        }
        mMediaSessionServerMap.put(session, server);
        mSessions.put(session.getSession(),session);
        mSessionCount.getAndIncrement();
        return server;
    }

    public void removeSession(Object session){
        if(session == null){
            throw new IllegalArgumentException("session error");
        }
        if(session instanceof  MediaSession){
            RtspMediaServer server = mMediaSessionServerMap.remove(session);
            if(server!=null){
                server.finish();
                mSessions.remove(((MediaSession)session).getSession());
                mSessionCount.getAndDecrement();
            }
        }else{
            MediaSession mediaSession = mSessions.remove(session);
            if(mediaSession!=null){
                RtspMediaServer server = mMediaSessionServerMap.remove(mediaSession);
                if(server!=null){
                    server.finish();
                    mSessionCount.getAndDecrement();
                }
            }
        }
    }

    public void managerRtspSource(String token,RtspSource rtspSource)throws Exception{
        if(TextUtils.isEmpty(token)){
            throw new IllegalArgumentException("token disallow null");
        }
        token = MediaUtils.parseToken(token);
        synchronized (sourceLock){
            DebugLog.info("managerRtspSource rtsp:"+token);
            if(mMapRtspSource.containsKey(token)){
                DebugLog.warn("source has in manager");
                throw new Exception("source has in manager");
            }
            mMapRtspSource.put(token,rtspSource);
            mMediaServerManager.managerMediaSource(token,rtspSource);
            mSourceCount.getAndIncrement();
        }
    }

    public void removeRtspSource(String rtsp){
        if(TextUtils.isEmpty(rtsp)){
            return;
        }
        synchronized (sourceLock){
            DebugLog.info("removeRtspSource rtsp:"+rtsp);
            RtspSource source = mMapRtspSource.remove(rtsp);
            if(source==null){
                String token = MediaUtils.parseToken(rtsp);
                mMapRtspSource.remove(token);
            }
            mMediaServerManager.removeMediaSource(rtsp);
            mSourceCount.decrementAndGet();
        }
    }

    public MediaSource getRtspSourceByPull(MediaRequest request){
        MediaSourceResult sourceResult = getRtspSourceByPull(request,"","");
        if(sourceResult!=null
                &&sourceResult.mediaSource!=null){
            return sourceResult.mediaSource;
        }else{
            return null;
        }
    }

    public MediaSourceResult getMediaSourceResultByPull(MediaRequest request){
        return getRtspSourceByPull(request,"","");
    }

    public static FileLogger getLogger(final String rtsp){
        try{
            URI uri = URI.create(rtsp);
            String host = uri.getHost();
            if(!TextUtils.isEmpty(host)){
                return FileLogger.getInstance("/rtsp/"+URLEncoder.encode(host))
                        .setLogcat(SystemServer.IsDebug());
            }
        }catch (Exception e){
        }
        return mLogger;
    }

    MapKeyLock<String> mMapKeyLock = new MapKeyLock<>();
    public MediaSourceResult getRtspSourceByPull(final MediaRequest request,String userName,String password){
        String rtsp = request.url;
        FileLogger logger = getLogger(rtsp);
        logger.log("getRtspSourceByPull rtsp:%s",rtsp);
        mLogger.log("getRtspSourceByPull rtsp:%s",rtsp);
        if(MediaUtils.isRtspURL(rtsp)){
            mLogger.log("parseToken rtsp:%s",rtsp);
            logger.log("parseToken rtsp:%s",rtsp);
            String token = MediaUtils.parseToken(rtsp);
            RtspConnection connection=null;
            try{
                mLogger.log("ifWait token:%s",token);
                logger.log("ifWait token:%s",token);
                mMapKeyLock.ifWait(token);
                RtspSource source = RtspMediaServerManager.getInstance().getRtspSource(token);
                if(source!=null){
                    return MediaSourceResult.ok(source);
                }

                String rtspUrl = MediaUtils.parseUrl(rtsp);
                mLogger.log("rtspConnection rtspUrl:%s",rtspUrl);
                logger.log("rtspConnection rtspUrl:%s",rtspUrl);
                RtspConnection rtspConnection = new RtspConnection(rtspUrl,userName,password);
                rtspConnection.connect();
                connection = rtspConnection;
                if(rtspConnection.sendRequestOption()){
                    RtspConnection.Response response =rtspConnection.sendRequestDESCRIBE();
                    if(response.status==200) {
                        if (rtspConnection.sendRequestSetup2()) {
                            if(rtspConnection.sendRequestPlay()){
                                RtspMediaServer server = addSession(new RtspConnectionMediaSession(rtspConnection));
                                if(server!=null){
                                    server.setHasAudio(request.hasAudio);
                                    server.setOutputEmptyAutoFinish(true);
                                    server.startInputStream(rtsp, response.content);
                                    mLogger.log("rtspConnection startInputStream rtsp:%s",rtsp);
                                    logger.log("rtspConnection startInputStream rtsp:%s",rtsp);
                                    try{
                                        return MediaSourceResult.ok(server);
                                    }finally {
                                        SystemServer.executeThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    server.putInputStream(rtspConnection.mInputStream);
                                                } catch (IOException e) {
                                                    //
                                                }finally {
                                                    removeSession(server.getSession());
                                                    SystemUtils.close(rtspConnection);
                                                }
                                            }
                                        });
                                    }
                                }else{
                                    SystemUtils.close(rtspConnection);
                                }
                            }else{
                                SystemUtils.close(rtspConnection);
                            }
                        }else{
                            SystemUtils.close(rtspConnection);
                        }
                    }else{
                        SystemUtils.close(rtspConnection);
                    }
                }else{
                    SystemUtils.close(rtspConnection);
                }
            }catch (Exception e){
                DebugLog.debug(e.getMessage());
                mLogger.log(e.getMessage());
                logger.log(e.getMessage());
                SystemUtils.close(connection);
                return MediaSourceResult.error(e.getMessage());
            }finally {
                mMapKeyLock.removeNotify(token);
                mLogger.log("removeNotify token:%s",token);
                logger.log("removeNotify token:%s",token);
            }
        }
        return MediaSourceResult.error("rtspConnection fail");
    }

    /**
     * @param key 支持url.name(name是去掉域名和端口的地址后缀)
     * @return
     */
    public RtspSource getRtspSource(String key){
        if(TextUtils.isEmpty(key)){
            return null;
        }
        synchronized (sourceLock){
            RtspSource source = getRtspSourceByURL(key);
            if(source!=null){
                return source;
            }
            return getRtspSourceByName(key);
        }
    }

    public RtspSource getRtspSourceByURL(String url){
        if(TextUtils.isEmpty(url)){
            return null;
        }
        synchronized (sourceLock){
            return mMapRtspSource.get(url);
        }
    }

    public RtspSource getRtspSourceByName(String name){
        if(TextUtils.isEmpty(name)){
            return null;
        }
        synchronized (sourceLock){
            for(String url:mMapRtspSource.keySet()){
                if(name.equals(MediaUtils.parseName(url))
                    ||url.equals(name)
                    ||url.equals(MediaUtils.parseName(name))){
                    return mMapRtspSource.get(url);
                }
            }
        }
        return null;
    }

    public boolean containsRtspSource(String rtsp){
        synchronized (sourceLock){
            return mMapRtspSource.containsKey(rtsp);
        }
    }

    public int sourceCount(){
        return mSourceCount.get();
    }

    public int sessionCount(){
        return mSessionCount.get();
    }

    public static void addManager(MediaListener manager) {
        pluginHelper.addManager(manager);
    }

    public static <T extends MediaListener> Collection<T> getManagers(
            Class<T> cls) {
        return pluginHelper.getManagers(cls);
    }

    public static void removeManager(Object manager){
        pluginHelper.removeManager(manager);
    }

}
