package gan.media.h264;

import gan.log.DebugLog;
import gan.media.MediaOutputStreamRunnableFrame;
import gan.media.h26x.HUtils;

public class H264InterceptPacketListener implements MediaOutputStreamRunnableFrame.InterceptPacketListener {

    private final static String TAG = H264InterceptPacketListener.class.getName();

    @Override
    public boolean onInterceptPacket(byte channel, byte[] packet, int offset, int len, long pts) {
        if(channel==0){
            byte nula = HUtils.getNaluByte(packet, offset, 10);
            int frameType = H264Utils.getNulaType(nula);
            if (frameType != H264Utils.NAL_SLICE_IDR
                    && frameType != H264Utils.NAL_SLICE
                    && frameType != H264Utils.NAL_SPS
                    && frameType != H264Utils.NAL_PPS) {
                //其他帧数据，合成的视频在google浏览器上边播放中途会断开
                DebugLog.debug( "exception frameType:" + frameType);
                return true;
            }
        }
        return false;
    }

}
