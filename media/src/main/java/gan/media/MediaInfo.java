package gan.media;

import gan.web.base.IDObject;

import java.util.UUID;

public class MediaInfo extends IDObject {

    public String url;
    public long  createTime;
    public String name;

    public final static String formatId(String id){
        return id+"_"+UUID.randomUUID();
    }

    public MediaInfo(String url){
       this(formatId(url),url,url);
    }

    public MediaInfo(String id, String url, String name) {
       this(id,url,System.currentTimeMillis(),name);
    }

    public MediaInfo(String id, String url, long createTime, String name) {
        super(id);
        this.url = url;
        this.createTime = createTime;
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
}
