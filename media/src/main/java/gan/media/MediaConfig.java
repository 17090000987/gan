package gan.media;

import gan.media.rtsp.Sdp;

public class MediaConfig {

    public String outputFile;
    public String audioCodec;
    public String videoCodec;

    public MediaConfig setOutputFile(String outputFile) {
        this.outputFile = outputFile;
        return this;
    }

    public boolean isVideoCodec(String codec) {
        return videoCodec!=null&&videoCodec.startsWith(codec);
    }

    public boolean isAudioCodec(String codec) {
        return audioCodec!=null&&audioCodec.startsWith(codec);
    }

    public static MediaConfig defaultConfig(){
        MediaConfig config = new MediaConfig();
        config.videoCodec = Media.MediaCodec.CODEC_H264;
        config.audioCodec = Media.MediaCodec.CODEC_AAC;
        return config;
    }

    public static MediaConfig createConfig(Sdp sdp){
        return createConfig(sdp,true);
    }

    public static MediaConfig createConfig(Sdp sdp, boolean hasAudio){
        if(sdp!=null){
            MediaConfig config = new MediaConfig();
            if(sdp.isVCodec("h265")){
                config.videoCodec = Media.MediaCodec.CODEC_H265;
            }else{
                config.videoCodec = Media.MediaCodec.CODEC_H264;
            }
            if(hasAudio){
                if(sdp.isACodec("aac")
                        ||sdp.isACodec("MPEG4-GENERIC")){
                    config.audioCodec = Media.MediaCodec.CODEC_AAC;
                }
            }
            return config;
        }
        return defaultConfig();
    }

    public static MediaConfig createConfigH264(){
        MediaConfig config = new MediaConfig();
        config.videoCodec = Media.MediaCodec.CODEC_H264;
        return config;
    }

}
