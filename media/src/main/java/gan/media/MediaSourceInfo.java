package gan.media;

import java.util.ArrayList;
import java.util.List;

public class MediaSourceInfo extends MediaInfo {

    public int watchNum;
    public int type;//0，在线视频，1.本地视频
    public int width;
    public int height;
    public List<MediaOutputInfo> mediaOutputInfos;

    public MediaSourceInfo(String id, String url, long createTime, String name) {
        super(id,url, createTime, name);
    }

    public MediaSourceInfo(String id, String url, String name){
        this(id,url,System.currentTimeMillis(),name);
    }

    public synchronized void addMediaOutputInfo(MediaOutputInfo info){
        if(mediaOutputInfos==null){
            mediaOutputInfos = new ArrayList<>();
        }
        mediaOutputInfos.add(info);
        watchNum++;
    }

    public synchronized boolean removeMediaOutputInfo(MediaOutputInfo info){
        if(mediaOutputInfos!=null){
            if(mediaOutputInfos.remove(info)){
                watchNum--;
                return true;
            }
        }
        return false;
    }

    public void setType(int type) {
        this.type = type;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
