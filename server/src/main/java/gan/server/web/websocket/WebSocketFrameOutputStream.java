package gan.server.web.websocket;

import gan.core.system.SystemUtils;
import gan.log.DebugLog;
import gan.media.BufferInfo;
import gan.media.MediaOutputStream;
import gan.media.h264.H264SPS;
import gan.media.h264.H264SPSPaser;
import gan.media.h265.H265Utils;
import gan.media.h26x.HUtils;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import gan.server.GanServer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class WebSocketFrameOutputStream implements MediaOutputStream {

    H264SPSPaser mH264SPSPaser;
    WebSocketSession mSession;

    public WebSocketFrameOutputStream(WebSocketSession session){
        mSession = session;
    }

    @Override
    public void init() {
        mH264SPSPaser = new H264SPSPaser();
    }

    ByteBuffer mByteBufferTemp;
    boolean vps,sps,pps;
    boolean mFirstStart;

    static int index;
    FileOutputStream fos;
    @Override
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo) throws Exception {
        if(channel==0){
            if(mFirstStart){
                sendWebsocketMessage(packet.array(), bufferInfo.offset, bufferInfo.length);
            }else{
                while(bufferInfo.length>0){
                    int frameLen = HUtils.frameLen(packet.array(),bufferInfo.offset,bufferInfo.length);
                    int frameType = H265Utils.getFrameType(packet.array(), bufferInfo.offset, 5);
                    if((frameType==H265Utils.Type_VPS)&&!vps){
                        gan.web.config.MediaConfig config = GanServer.getMediaConfig();
                        mByteBufferTemp = ByteBuffer.allocate(config.rtspFrameBufferSize);
                        mByteBufferTemp.put(packet.array(), bufferInfo.offset, frameLen);
                        vps = true;
                    }else if(vps&&(frameType==H265Utils.Type_SPS)&&!sps){
                        mByteBufferTemp.put(packet.array(), bufferInfo.offset, frameLen);
                        int startCodeSize = HUtils.startCodeSize(mByteBufferTemp.array(),0,5);
                        if(startCodeSize>0){
                            int spsLen = frameLen-startCodeSize;
                            System.arraycopy(mByteBufferTemp.array(), startCodeSize, mXpsBuffer,0, spsLen);
                            parseSps(mXpsBuffer, spsLen);
                        }else{
                            parseSps(mByteBufferTemp.array(), frameLen);
                        }
                        sps = true;
                    }else if(sps&&(frameType==H265Utils.Type_PPS)&&!pps){
                        mByteBufferTemp.put(packet.array(), bufferInfo.offset, frameLen);
                        pps = true;
                    }else if(sps&&pps&&(frameType==H265Utils.Type_IDR)){
                        mByteBufferTemp.put(packet.array(), bufferInfo.offset, frameLen);
                        sendWebsocketMessage(mByteBufferTemp.array(), 0, mByteBufferTemp.position());
                        DebugLog.info("initVideo length:"+mByteBufferTemp.position());
                        sendWebsocketMessage(packet.array(), bufferInfo.offset, frameLen);
                        mByteBufferTemp.clear();
                        mByteBufferTemp=null;
                        mFirstStart = true;
                    }

                    bufferInfo.offsets(frameLen);
                    bufferInfo.length-=frameLen;
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void sendWebsocketMessage(byte[] array,int offset,int length) throws IOException {
        mSession.sendMessage(new BinaryMessage(array, offset, length, true));
    }

    @Override
    public void close() {
        SystemUtils.close(mSession);
        mFirstStart = false;
        sps = vps = pps = false;

        SystemUtils.close(fos);
    }

    H264SPS mH264SPS;
    H264SPS mH264SPS1=new H264SPS();
    byte[] mXpsBuffer = new byte[128];
    int[] xpsLen = new int[]{128};
    private int offsetSpsPps(ByteBuffer packet, BufferInfo bufferInfo){
        int offset = 0;
        xpsLen[0]=128;
        int startCodeSize = HUtils.startCodeSize(packet.array(),bufferInfo.offset,5);
        int ret = H265Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length, mXpsBuffer, xpsLen,H265Utils.Type_SPS, startCodeSize);
        if(ret>=0){
            int len = startCodeSize+ xpsLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
            parseSps(mXpsBuffer, xpsLen[0]);
        }

        xpsLen[0]=128;
        startCodeSize = HUtils.startCodeSize(packet.array(),bufferInfo.offset, 5);
        ret = H265Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length, mXpsBuffer, xpsLen,H265Utils.Type_PPS, startCodeSize);
        if(ret>=0){
            int len = startCodeSize+ xpsLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
        }

        xpsLen[0]=128;
        startCodeSize = HUtils.startCodeSize(packet.array(),bufferInfo.offset, 5);
        ret = H265Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length, mXpsBuffer, xpsLen,H265Utils.Type_VPS, startCodeSize);
        if(ret>=0){
            int len = startCodeSize+ xpsLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
        }

        return offset;
    }

    protected void parseSps(byte[] buf,int nLen){
        if(mH264SPSPaser.h264_decode_seq_parameter_set(buf, nLen, mH264SPS1)){
            if(mH264SPS==null){
                mH264SPS = mH264SPS1.clone();
                int width = (mH264SPS.pic_width_in_mbs_minus1+1)*16;
                int height = (mH264SPS.pic_height_in_map_units_minus1+1)*16;
                DebugLog.info("width:"+width+";height:"+height);
            }else if(!mH264SPS.equals(mH264SPS1)){
                mH264SPS.copy(mH264SPS1);
//                onSpsChanged(mH264SPS);
            }
        }
    }
}
