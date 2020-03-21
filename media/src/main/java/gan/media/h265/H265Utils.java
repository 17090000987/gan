package gan.media.h265;

import gan.media.BufferInfo;

import java.nio.ByteBuffer;

public class H265Utils {

    public final static int Type_Unknow = -1;
    public final static int Type_VPS = 32;
    public final static int Type_SPS = 33;
    public final static int Type_PPS = 34;
    public final static int Type_SEI = 35;
    public final static int Type_IDR = 19;
    public final static int Type_02 = 1;

    public static boolean isIFrame(ByteBuffer frame, BufferInfo frameInfo){
        return false;
    }

    public static boolean isIFrame(byte[] data,int offset, int length){
        return isFrameType(data,offset,length,Type_IDR);
    }

    public static boolean isFrameType(byte[] data,int offset, int length,int frameType){
        return getFrameType(data,offset,length)==frameType;
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
        return  Type_Unknow;
    }

    public static boolean isNulaType(byte b,int nulaType){
        return nulaType == getNulaType(b);
    }

    public static int getNulaType(byte b){
        return 0x3F & (b>>1);
    }

    public static boolean isXPS(byte[] data,int offset, int length){
        int index;
        byte nula;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            nula = data[index + 3];
            if ((0 == data[index]) && (0 == data[index + 1]) && (1 == data[index + 2])
                    && (isNulaType(nula, Type_SPS)
                            ||isNulaType(nula, Type_PPS)
                            ||isNulaType(nula,Type_VPS))) {
                return true;
            }
        }
        return false;
    }

}
