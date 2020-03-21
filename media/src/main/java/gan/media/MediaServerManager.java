package gan.media;

import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.core.file.FileHelper;
import gan.media.file.MediaSessionFile;
import gan.media.file.MediaSourceFile;
import gan.media.rtsp.RtspMediaServerManager;
import gan.media.utils.MediaUtils;
import gan.core.system.server.SystemServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class MediaServerManager {

    static {
        sInstance = new MediaServerManager();
    }
    private static MediaServerManager sInstance;

    public static MediaServerManager getInstance() {
        return sInstance;
    }

    private ConcurrentHashMap<String, MediaSource> mMapMediaSource;
    private List<MediaSourceAdapter> mMediaSourceAdapters;

    private MediaServerManager(){
        mMapMediaSource = new ConcurrentHashMap<>();
        mMediaSourceAdapters = new ArrayList<>();
    }

    public void addMediaSourceAdapter(MediaSourceAdapter adapter){
        mMediaSourceAdapters.add(adapter);
    }

    public boolean removeMediaSourceAdapter(MediaSourceAdapter adapter){
        return mMediaSourceAdapters.remove(adapter);
    }

    public void managerMediaSource(String token, MediaSource source){
        mMapMediaSource.put(token,source);
    }

    public void removeMediaSource(String token){
        MediaSource source = mMapMediaSource.remove(token);
        if(source==null){
            token = MediaUtils.parseToken(token);
            mMapMediaSource.remove(token);
        }
    }

    /**
     *
     * @param type 0,默认不走pull,1,走pull,2,其他
     * @return
     */
    public MediaSourceResult getMediaSourceResult(MediaRequest request,int type){
        MediaSourceResult result = MediaSourceResult.error("");
        MediaSource source = mMapMediaSource.get(request.url);
        if(source==null){
            source = mMapMediaSource.get(request.getToken());
        }
        if(source==null){
            String name = MediaUtils.parseName(request.url);
            if(isFileUrl(name)){
                result = getFileMediaSourceResult(name);
            }
            if(!result.ok){
                result = internalGetMediaSourceResult(request, type);
            }
            return result;
        }else{
            return MediaSourceResult.ok(source);
        }
    }

    public MediaSource getMediaSource(MediaRequest request){
        MediaSource source = mMapMediaSource.get(request.url);
        if(source==null){
            source = mMapMediaSource.get(request.getToken());
        }
        if(source==null){
            String name = MediaUtils.parseName(request.url);
            if(isFileUrl(name)){
                source = getFileMediaSource(name);
            }
            if(source==null){
                source = getRtspSource(request, true);
            }
        }
        return source;
    }

    private boolean isFileUrl(String name){
        if(name.startsWith("http")
                ||name.startsWith("rtsp")
                ||name.startsWith("https")){
           name = MediaUtils.parseName(name);
        }
        return name.startsWith("file")
                ||name.startsWith("/file");
    }

    public MediaSource getRtspSource(String url){
        return getRtspSource(MediaRequest.obtainRequest(url));
    }

    public MediaSource getRtspSource(MediaRequest request){
        return getRtspSource(request,true);
    }

    private MediaSourceResult internalGetMediaSourceResult(MediaRequest request,int type){
        boolean byPull = true;
        MediaSource source = RtspMediaServerManager.getInstance()
                .getRtspSource(request.getToken());
        if(source==null){
            MediaSourceResult result = MediaSourceResult.error();
            if(request.isRtspURL()){
                for(MediaSourceAdapter sourceAdapter:mMediaSourceAdapters){
                    if(sourceAdapter.accept(request)){
                        result = sourceAdapter.getMediaSourceResult(request);
                        byPull=type==1;
                    }
                }
            }
            if(!result.ok){
                if(byPull){
                    DebugLog.info(String.format("getRtspSourceByPull :%s", request.toString()));
                    result = RtspMediaServerManager.getInstance().getMediaSourceResultByPull(request);
                }
            }
            dumpSource(result.mediaSource);
            return result;
        }else{
            dumpSource(source);
            return MediaSourceResult.ok(source);
        }
    }

    public MediaSource getRtspSource(MediaRequest request,boolean byPull){
        MediaSource source = RtspMediaServerManager.getInstance()
                .getRtspSource(request.getToken());
        if(source==null){
            if(request.isRtspURL()){
                for(MediaSourceAdapter sourceAdapter:mMediaSourceAdapters){
                    if(sourceAdapter.accept(request)){
                        source = sourceAdapter.getMediaSource(request);
                    }
                }
            }
            if(source==null){
                if(byPull){
                    DebugLog.info(String.format("getRtspSourceByPull :%s", request.toString()));
                    source = RtspMediaServerManager.getInstance().getRtspSourceByPull(request);
                }
            }
        }
        dumpSource(source);
        return source;
    }

    private void dumpSource(MediaSource source){
        if(source!=null){
            FileLogger.getInfoLogger().log(String.format("dumpSource:%s",source.toString()));
        }
    }

    public MediaSource getFileMediaSource(final String uri) {
        String filePath = SystemServer.getPublicPath(uri);
        if(FileHelper.isFileExists(filePath)){
            MediaSource source = SystemServer.startServer(MediaSourceFile.class, new MediaSessionFile(uri))
                    .setOutputEmptyAutoFinish(true);
            ((MediaSourceFile) source).startInput();
            return source;
        }
        return null;
    }

    public MediaSourceResult getFileMediaSourceResult(final String uri){
        MediaSource source = getFileMediaSource(uri);
        if(source!=null){
            return MediaSourceResult.ok(source);
        }
        return MediaSourceResult.error("文件不存在");
    }

}
