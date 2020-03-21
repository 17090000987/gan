package gan.media;

public class MediaType {

    public final static String PS = "PS";
    public final static String ES = "es";

    public static String findMediaType(String media){
        if(media.startsWith("raw")){
            return PS;
        }else if(media.toLowerCase()
                .startsWith(Media.MediaCodec.CODEC_H264)){
            return ES;
        }
        return null;
    }

}
