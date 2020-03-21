package gan.media;

import android.os.JSONObject;
import gan.core.RecycleObjectPool;
import gan.core.utils.JsonParseUtils;
import gan.media.utils.MediaUtils;

public class MediaRequest {

    private static RecycleObjectPool<MediaRequest> sMediaRequestObjectPool;

    static{
        sMediaRequestObjectPool = new RecycleObjectPool<>(10);
    }


    public String url;
    public boolean hasAudio;

    private MediaRequest(String url){
        this.url = url;
    }

    public boolean setHasAudio(boolean hasAudio) {
        this.hasAudio = hasAudio;
        return true;
    }

    public boolean isHasAudio() {
        return hasAudio;
    }

    public String getToken(){
        return MediaUtils.parseToken(url);
    }

    public boolean isRtspURL(){
        return MediaUtils.isRtspURL(url);
    }

    @Override
    public String toString() {
        return "MediaRequest{" +
                "url='" + url + '\'' +
                ", hasAudio=" + hasAudio +
                '}';
    }

    public static MediaRequest obtainRequest(String url){
        MediaRequest request = sMediaRequestObjectPool.poll();
        if(request==null){
            request = new MediaRequest(url);
        }
        request.url = url;
        return request;
    }

    public final void recycle(){
        this.hasAudio = false;
        sMediaRequestObjectPool.recycle(this);
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void parseFormJSONObject(JSONObject jo){
        JsonParseUtils.parse(jo, this);
    }

}
