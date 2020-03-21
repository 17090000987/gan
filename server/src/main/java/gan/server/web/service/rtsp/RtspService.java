package gan.server.web.service.rtsp;

import gan.core.event.AndroidEventManager;
import gan.core.event.Event;
import gan.core.event.EventManager;
import gan.media.MediaEvent;
import gan.media.MediaSourceInfo;
import gan.media.utils.MediaUtils;
import gan.web.spring.ListService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class RtspService implements ListService<MediaSourceInfo>,EventManager.OnEventListener {

    private static RtspService sInstance;

    static{
        sInstance = new RtspService();
    }

    ArrayList<MediaSourceInfo> mMediaInfos = new ArrayList<>();
    AndroidEventManager mEventManager = AndroidEventManager.getInstance();

    public static RtspService getInstance() {
        return sInstance;
    }

    private RtspService(){
        mEventManager.addEventListener(MediaEvent.Event_Source_Create,this);
        mEventManager.addEventListener(MediaEvent.Event_Source_Remove,this);
    }

    @Override
    public void onEventRunEnd(Event event) {
        int code = event.getEventCode();
        if(code == MediaEvent.Event_Source_Create){
            MediaSourceInfo info = event.findParam(MediaSourceInfo.class);
            if(info!=null){
               add(info);
            }
        }else if(code == MediaEvent.Event_Source_Remove){
            MediaSourceInfo info = event.findParam(MediaSourceInfo.class);
            if(info!=null){
                remove(info);
            }
        }
    }

    @Override
    public MediaSourceInfo getById(String id) {
        for(MediaSourceInfo info:mMediaInfos){
            if(info.getId().equals(id)){
                return info;
            }
        }
        return null;
    }

    public MediaSourceInfo getByUrl(String url) {
        for(MediaSourceInfo info:mMediaInfos){
            if(url.equals(info.url)
                    ||url.equals(MediaUtils.parseName(info.url))){
                return info;
            }
        }
        return null;
    }

    @Override
    public void add(MediaSourceInfo item) {
        mMediaInfos.add(item);
        onMediaInfoChanged();
    }

    protected void onMediaInfoChanged(){
    }

    public boolean remove(MediaSourceInfo item){
        try{
            return mMediaInfos.remove(item);
        }finally {
            onMediaInfoChanged();
        }
    }

    @Override
    public Collection<MediaSourceInfo> list(int offset) {
        return mMediaInfos;
    }

    @Override
    public int count() {
        return mMediaInfos.size();
    }

    @Override
    public void delete(String id) {
    }

    @Override
    public Collection<MediaSourceInfo> getAll() {
        return Collections.unmodifiableCollection(mMediaInfos);
    }

    public void managerFileProgress(String file){

    }

    public void removeFileProgress(String file){

    }

    public String getFileProgress(String file){
        return null;
    }

}
