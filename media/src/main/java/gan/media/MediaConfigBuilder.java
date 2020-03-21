package gan.media;

public class MediaConfigBuilder {

    public MediaConfig mediaConfig;

    public MediaConfigBuilder(){
        this(new MediaConfig());
    }

    public MediaConfigBuilder(MediaConfig mediaConfig){
        this.mediaConfig = mediaConfig;
    }

    public MediaConfigBuilder setVCodec(String videoCodec){
        this.mediaConfig.videoCodec = videoCodec;
        return this;
    }


    public MediaConfigBuilder setACodec(String audioCodec){
        this.mediaConfig.audioCodec = audioCodec;
        return this;
    }

    public MediaConfig build(){
        return mediaConfig;
    }

}
