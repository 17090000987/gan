package gan.media;

import gan.log.DebugLog;
import gan.core.system.SystemUtils;
import gan.core.system.server.BaseServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class MediaServer extends BaseServer {

    final static String Tag = MediaServer.class.getName();

    protected  MediaSession   mMediaSession;
    private Object mTag;
    private HashMap<String, Object> mTags;

    @Override
    protected final void onCreate(Object... parameter) {
        super.onCreate(parameter);
        if(parameter!=null
                &&parameter.length>0){
            onCreateSession((MediaSession)parameter[0]);
        }
    }

    protected void onCreateSession(MediaSession session){
        mMediaSession = session;
        DebugLog.info("onCreate session id:"+ getSessionId());
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        SystemUtils.close(mMediaSession);
        DebugLog.info("onDestory session id:"+ getSessionId());
        mTags = null;
    }

    public String getType(){
        return "";
    }

    public void onSocketStream(InputStream is, OutputStream os) throws IOException {

    }

    public MediaSession getSession() {
        return mMediaSession;
    }

    public String getSessionId(){
        if(mMediaSession!=null){
            return mMediaSession.getSessionId();
        }
        return null;
    }

    @Override
    public void sendMessage(int b) throws IOException{
        mMediaSession.sendMessage(b);
    }

    @Override
    public void sendMessage(byte[] b) throws IOException{
        mMediaSession.sendMessage(b);
    }

    @Override
    public void sendMessage(byte[] b,int off,int len) throws IOException{
        mMediaSession.sendMessage(b,off,len);
    }

    public final Object setTag(Object object){
        return mTag = object;
    }

    public final Object setIdTag(String id,Object tag){
        if(mTags == null){
            mTags = new HashMap<String, Object>();
        }
        return mTags.put(id, tag);
    }

    public final Object getTag(){
        return mTag;
    }

    public final Object getIdTag(String id){
        if(mTags == null){
            return null;
        }
        return mTags.get(id);
    }
}
