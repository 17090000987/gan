package gan.media;

public interface MediaOutputStreamRunnable extends Runnable{

    public final static String PacketType_None  = "packettype_none";
    public final static String PacketType_Raw = "packettype_raw";
    public final static String PacketType_Rtp = "packettype_rtp";
    public final static String PacketType_Frame = "packettype_frame";
    public final static String PacketType_Mp4 = "packettype_mp4";

    public String getPacketType();
    public MediaInfo getMediaInfo();
    public void start();
    public void putPacket(byte channel, byte[] packet, int offset,int len,long time);
    public void close();
}