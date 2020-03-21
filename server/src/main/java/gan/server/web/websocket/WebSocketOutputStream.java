package gan.server.web.websocket;

import gan.log.DebugLog;
import gan.media.MediaConfig;
import gan.media.MediaSession;
import gan.media.MediaSourceInfo;
import gan.media.h264.H264SPS;
import gan.media.mp4.Mp4DataCallBack;
import gan.media.mp4.Mp4MeidaOutputStream;
import gan.core.system.SystemUtils;
import gan.server.web.service.rtsp.RtspService;

import java.io.FileOutputStream;

public class WebSocketOutputStream extends Mp4MeidaOutputStream implements Mp4DataCallBack {

    String mSessionId;
    MediaSession mSession;

    public WebSocketOutputStream(String url, MediaSession session){
        this(url,session,MediaConfig.defaultConfig());
    }

    public WebSocketOutputStream(String url, MediaSession session,MediaConfig config) {
        super(url, null, config);
        mSession = session;
        mSessionId = session.getSessionId();
    }

    FileOutputStream fos;
    @Override
    public void onMp4(byte[] data, int length) {
        super.onMp4(data, length);
        try {
            mSession.sendMessage(data);
        } catch (Exception e) {
            e.printStackTrace();
            SystemUtils.close(mSession);
        }
    }

    @Override
    protected void onTimeError() {
        super.onTimeError();
        SystemUtils.close(mSession);
    }

    @Override
    protected void onEncoderChanged(int newEncoder) {
       super.onEncoderChanged(newEncoder);
        DebugLog.info(String.format("onEncoderChanged encoder:%s",newEncoder));
        SystemUtils.close(mSession);
    }

    @Override
    protected void onSpsChanged(H264SPS h264SPS) {
        super.onSpsChanged(h264SPS);
        SystemUtils.close(mSession);
    }

    @Override
    protected void onVideoSize(int width, int height) {
        super.onVideoSize(width, height);
        DebugLog.info(String.format("width:%s,height:%s",width,height));
        MediaSourceInfo source = RtspService.getInstance().getByUrl(getUrl());
        if(source!=null){
            source.setWidth(width);
            source.setHeight(height);
        }
    }

    @Override
    public void close() {
        super.close();
        SystemUtils.close(mSession);
        SystemUtils.close(fos);
    }

}
