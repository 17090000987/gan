package gan.media.parser;

import gan.log.DebugLog;
import gan.core.StreamHelper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class RtpStreamParser implements StreamParser{

    private final static String Tag = RtpOverTcpStreamParser.class.getName();

    int mBufferSize;
    boolean mInputStreaming;
    PacketListener mPacketListener;

    public RtpStreamParser(int bufferSize,PacketListener packetListener){
        mBufferSize = bufferSize;
        mPacketListener = packetListener;
    }

    @Override
    public void start() {
        DebugLog.info("start");
        mInputStreaming = true;
    }

    @Override
    public void inputStream(InputStream is) throws IOException{
        int bufSize = 1024;
        byte[] buf = new byte[bufSize];
        byte[] b2 = new byte[2];
        ByteBuffer packet = ByteBuffer.allocate(mBufferSize);
        while (mInputStreaming){
            if(read2(is,b2,mInputStreaming)>0){
                short length = ByteBuffer.wrap(b2).getShort();
                short len = length;
                int readLen;
                packet.clear();
                while (mInputStreaming&&len>0){
                    if(len<bufSize){
                        readLen = is.read(buf,0, len);
                    }else{
                        readLen = is.read(buf,0, bufSize);
                    }
                    if(readLen!=-1){
                        len -= readLen;
                        packet.put(buf,0, readLen);
                    }else{
                        stop();
                        throw new IOException("socket read len -1 finish");
                    }
                }
                onTcpPacket((byte)0, packet, 0, length);
            }else{
                stop();
                throw new IOException("socket read len -1 finish");
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void stop() {
        if(mInputStreaming){
            DebugLog.info("stop");
        }
        mInputStreaming = false;
    }

    public boolean isStreaming() {
        return mInputStreaming;
    }

    private int read2(InputStream is, byte[] b, boolean run) throws IOException {
        int len = StreamHelper.read(is,b,run);
        if(len==-1){
            stop();
            throw new IOException("socket closed");
        }
        return len;
    }

    protected void onTcpPacket(byte channel,ByteBuffer packet,int offset,short length){
        if(mPacketListener!=null){
            mPacketListener.onTcpPacket(channel, packet, offset, length);
        }
    }

    public static interface PacketListener{
        public void onTcpPacket(byte channel,ByteBuffer packet,int offset,short length);
    }
}
