package gan.media.rtsp;

public class Sdp {

    public static final String SDP = "v=0\r\n"
            + "o=- 2205756022 2205756022 IN IP4 127.0.0.1\r\n"
            + "s=Gan\r\n"
            + "i=Gan\r\n"
            + "c=IN IP4 127.0.0.1\r\n"
            + "t=0 0\r\n"
            + "a=x-qt-text-nam:Gan\r\n"
            + "a=x-qt-text-inf:Gan\r\n"
            + "a=x-qt-text-cmt:source application::Gan\r\n"
            + "a=x-qt-text-aut:\r\n"
            + "a=x-qt-text-cpy:\r\n"
            + "m=video 0 RTP/AVP 96\r\n"
            + "a=rtpmap:96 H264/90000\r\n"
            + "a=fmtp:96 packetization-mode=1;sprop-parameter-sets=\r\n"
            + "a=control:trackID=1\r\n"
            + "m=audio 0 RTP/AVP 97\r\n"
            + "a=rtpmap:97 MPEG4-GENERIC/8000/1\r\n"
            + "a=fmtp:97 streamtype=5;profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3;config=1588\r\n"
            + "a=control:trackID=2";

    public static final String SDP_OnlyVideo = "v=0\r\n"
            + "o=- 2205756022 2205756022 IN IP4 127.0.0.1\r\n"
            + "s=Gan\r\n"
            + "i=Gan\r\n"
            + "c=IN IP4 127.0.0.1\r\n"
            + "t=0 0\r\n"
            + "a=x-qt-text-nam:Gan\r\n"
            + "a=x-qt-text-inf:Gan\r\n"
            + "a=x-qt-text-cmt:source application::Gan\r\n"
            + "a=x-qt-text-aut:\r\n"
            + "a=x-qt-text-cpy:\r\n"
            + "m=video 0 RTP/AVP 96\r\n"
            + "a=rtpmap:96 H264/90000\r\n"
            + "a=fmtp:96 packetization-mode=1;sprop-parameter-sets=\r\n"
            + "a=control:trackID=1";


    public String sdp;

    public int vPlayLoad;
    public int aPlayLoad;

    public String vCodec;
    public String aCodec;

    public int vTrackId;
    public int aTrackId;

    public boolean isVCodec(String key){
        if(vCodec!=null){
            return vCodec.toLowerCase().startsWith(key.toLowerCase());
        }
        return false;
    }

    public boolean isACodec(String key){
        if(aCodec!=null){
            return aCodec.toLowerCase().startsWith(key.toLowerCase());
        }
        return false;
    }

    public boolean isATrack(int trackId){
        return aTrackId == trackId;
    }

    public boolean isVTrack(int trackId){
        return vTrackId == trackId;
    }

    @Override
    public String toString() {
        return "Sdp{" +
                "sdp='" + sdp + '\'' +
                ", vPlayLoad=" + vPlayLoad +
                ", aPlayLoad=" + aPlayLoad +
                ", vCodec='" + vCodec + '\'' +
                ", aCodec='" + aCodec + '\'' +
                ", vTrackId=" + vTrackId +
                ", aTrackId=" + aTrackId +
                '}';
    }
}
