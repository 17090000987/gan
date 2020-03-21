package gan.media.h264;

import gan.media.BufferInfo;
import gan.media.h26x.HUtils;

import java.nio.ByteBuffer;

public class H264Utils {

    public final static int NAL_UNKNOWN = 0;
    public final static int NAL_SLICE = 1;
    public final static int NAL_SLICE_DPA = 2;
    public final static int NAL_SLICE_DPB = 3;
    public final static int NAL_SLICE_DPC = 4;
    public final static int NAL_SLICE_IDR = 5;
    public final static int NAL_SEI = 6;
    public final static int NAL_SPS = 7;
    public final static int NAL_PPS = 8;

    public static int getXPS(byte[] data, int offset, int length, byte[] dataOut, int[] outLen, int type){
        return getXPS(data,offset,length,dataOut,outLen,type, HUtils.startCodeSize(data,offset,length));
    }

    /**
     * 获取sps ，pps
     * @param data
     * @param offset
     * @param length
     * @param dataOut
     * @param outLen
     * @param type 7 sps,8 pps
     * @return
     */
    public static int getXPS(byte[] data, int offset, int length, byte[] dataOut, int[] outLen, int type,int start) {
        int i;
        int pos0;
        int pos1;
        pos0 = -1;
        int index;
        for (i = 0; i<length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1]) && (1 == data[index + 2]) && isNulaType(data[index + 3],type)) {
                pos0 = index;
                break;
            }
        }
        if (-1 == pos0) {
            return -1;
        }
        if (pos0 > 0 && data[pos0-1] == 0){ // 0 0 0 1
            pos0 = pos0-1;
        }
        pos1 = -1;
        for (i = pos0 + 4; i < length - 4; i++) {
            if ((0 == data[i]) && (0 == data[i + 1]) && (1 == data[i + 2])) {
                pos1 = i;
                break;
            }
        }
        int psLen;
        if (-1 == pos1 || pos1 == 0) {
            psLen = length;
        }else{
            if (data[pos1 - 1] == 0) {
                pos1 -= 1;
            }
            psLen = pos1 - pos0;
        }
        if (psLen > outLen[0]) {
            return -3; // 输入缓冲区太小
        }
        dataOut[0] = 0;
        System.arraycopy(data, pos0+start, dataOut, 0, psLen-=start);
        outLen[0] = psLen;
        return pos0;
    }

    public static boolean isSPS(byte[] data,int offset, int length){
        return isFrameType(data,offset,length,NAL_SPS);
    }

    public static boolean isIFrame(ByteBuffer frame,BufferInfo frameInfo){
        return false;
    }

    public static boolean isIFrame(byte[] data,int offset, int length){
        return isFrameType(data,offset,length,NAL_SLICE_IDR);
    }

    public static boolean isFrameType(byte[] data,int offset, int length,int frameType){
        return getFrameType(data,offset,length)==frameType;
    }

    public static int getFrameType(byte[] data,int offset, int length){
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1]) && (0==data[index + 2]) &&(1 == data[index + 3])) {
                return getNulaType(data[index + 4]);
            }
            if ((0 == data[index]) && (0 == data[index + 1]) && (1 == data[index + 2])){
                return getNulaType(data[index + 3]);
            }
        }
        return  NAL_UNKNOWN;
    }

    public static int getNulaType(byte b){
        return 0x1F & (int)b;
    }

    public static boolean isNulaType(byte b,int nulaType){
        return nulaType == getNulaType(b);
    }

    public static boolean isSPSOrPPS(byte[] data,int offset, int length){
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1]) && (1 == data[index + 2])
                    && (isNulaType(data[index + 3],NAL_SPS)||isNulaType(data[index + 3],NAL_PPS))) {
                return true;
            }
        }
        return false;
    }

    public static int offsetSpsPps(ByteBuffer packet, BufferInfo bufferInfo){
        byte[] dataOut = new byte[128];
        int[] outLen = new int[]{128};
        return offsetSpsPps(packet,bufferInfo,dataOut,outLen);
    }

    public static int offsetSpsPps(ByteBuffer packet, BufferInfo bufferInfo,byte[] dataOut,int[] outLen){
        int offset = 0;
        outLen[0]=128;
        int ret = H264Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length,dataOut,outLen,NAL_SPS);
        if(ret>=0){
            int len = outLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
        }
        outLen[0]=128;
        ret = H264Utils.getXPS(packet.array(),bufferInfo.offset,bufferInfo.length,dataOut,outLen,NAL_PPS);
        if(ret>=0){
            int len = outLen[0];
            bufferInfo.offsets(len);
            bufferInfo.length-=len;
            offset+=len;
        }
        return offset;
    }

}
