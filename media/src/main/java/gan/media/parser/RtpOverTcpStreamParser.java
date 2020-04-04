package gan.media.parser;

import gan.core.StreamHelper;
import gan.core.system.SystemUtils;
import gan.log.DebugLog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class RtpOverTcpStreamParser implements StreamParser{

    int mBufferSize;
    boolean mInputStreaming;
    PacketListener mPacketListener;

    public RtpOverTcpStreamParser(int bufferSize,PacketListener packetListener){
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
        byte[] b1 = new byte[1];
        byte[] b2 = new byte[2];
        ByteBuffer packet = ByteBuffer.allocate(mBufferSize);
        while (mInputStreaming){
            if(read2(is,b1,mInputStreaming)>0){
                byte $ = b1[0];
                if($==0x24){
                    if(read2(is,b1,mInputStreaming)>0){
                        byte channel = b1[0];
                        if(read2(is,b2,mInputStreaming)>0){
                            int length = SystemUtils.byteToUnsignInt16(b2,0);
                            int len = length;
                            int readLen = 0;
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
                            onTcpPacket(channel, packet, 0, length);
                        }
                    }
                }
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

    protected void onTcpPacket(byte channel,ByteBuffer packet,int offset,int length){
        if(mPacketListener!=null){
            mPacketListener.onTcpPacket(channel, packet, offset, length);
        }
    }

    public static interface PacketListener{
        public void onTcpPacket(byte channel,ByteBuffer packet,int offset,int length);
    }
}
