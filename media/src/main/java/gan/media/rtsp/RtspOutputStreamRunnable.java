package gan.media.rtsp;

import gan.log.DebugLog;
import gan.media.*;

import java.nio.ByteBuffer;
import java.util.Vector;

public class RtspOutputStreamRunnable extends MediaOutputStreamRunnable1 {

    public RtspOutputStreamRunnable(MediaOutputStream out, MediaOutputInfo mediaSession) {
        super(out, mediaSession,1500, MediaOutputStreamRunnable.PacketType_Rtp);
        setPacketBufferMaxCount(5);
    }

    @Override
    protected void discardPacketInCache(Vector<PacketInfo> packetInfos, PacketInfoRecyclePool pool) {
        super.discardPacketInCache(packetInfos, pool);
        PacketInfo info = packetInfos.get(0);
        if(info!=null&&isIgnorePacket(info)){
            DebugLog.debug("remove packetInfos index 0");
            packetInfos.remove(0);
            info.clear();
            pool.recycle(info);
        }
    }

    @Override
    protected boolean onInterceptPacket(byte channel, byte[] packet, int offset, int len, long time) {
        if(isBufferLarge()){
            return isIgnorePacket(channel,packet,offset,len);
        }
        if(isBufferLarge(20)){
            return true;
        }
        return super.onInterceptPacket(channel, packet, offset, len, time);
    }

    private boolean isIgnorePacket(PacketInfo info){
        return isIgnorePacket(info.channel(), info.array(), info.offset(), info.length());
    }

    private boolean isIgnorePacket(byte channel, byte[] packet, int offset, int len){
        if(channel==0){
            byte FUindicator = packet[offset+12];
            byte NRI = (byte)((FUindicator<<1)>>6);
            return NRI<=1;
        }else{
            return false;
        }
    }

}
