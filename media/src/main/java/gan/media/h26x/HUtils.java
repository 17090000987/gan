package gan.media.h26x;

public class HUtils {

    public static int startCodeSize(byte[] data, int offset, int length){
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1]) && (0==data[index + 2]) &&(1 == data[index + 3])) {
                return 4;
            }
            if ((0 == data[index]) && (0 == data[index + 1]) && (1 == data[index + 2])){
                return 3;
            }
        }
        return 0;
    }

    public static int findStartCodeIndex(byte[] data, int offset, int length){
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1])
                    && ((1 == data[index + 2]) || ((0 == data[index + 2])&&(1 == data[index + 3])))) {
                return index;
            }
        }
        return -1;
    }

    public static int findStartCodeOffset(byte[] data,int offset,int length){
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1])
                    && ((1 == data[index + 2]) || ((0 == data[index + 2])&&(1 == data[index + 3])))) {
                return i;
            }
        }
        return -1;
    }

    public static int frameLen(byte[] data, int offset, int length){
        int pos0=-1;
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1])
                    && ((1 == data[index + 2]) || ((0 == data[index + 2])&&(1 == data[index + 3])))) {
                pos0 = index;
                break;
            }
        }
        if(pos0<0){
            return length;
        }
        int pos1=-1;
        for (int i = pos0+4; i < length - 4; i++) {
            if ((0 == data[i]) && (0 == data[i + 1])
                    && ((1 == data[i + 2]) || ((0 == data[i + 2])&&(1 == data[i + 3])))) {
                pos1 = i;
                break;
            }
        }
        if(pos0>=0&&pos1>0
                &&pos1>pos0){
            return pos1-pos0;
        }
        return length;
    }

    public static int find2frameLen(byte[] data, int offset, int length){
        int pos0=-1;
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1])
                    && ((1 == data[index + 2]) || ((0 == data[index + 2])&&(1 == data[index + 3])))) {
                pos0 = index;
                break;
            }
        }
        if(pos0<0){
            return 0;
        }
        int pos1=-1;
        for (int i = pos0+4; i < length - 4; i++) {
            if ((0 == data[i]) && (0 == data[i + 1])
                    && ((1 == data[i + 2]) || ((0 == data[i + 2])&&(1 == data[i + 3])))) {
                pos1 = i;
                break;
            }
        }
        if(pos0>=0&&pos1>0
                &&pos1>pos0){
            return pos1-pos0;
        }
        return 0;
    }

    public static byte getNaluByte(byte[] data, int offset, int length){
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if ((0 == data[index]) && (0 == data[index + 1]) && (0==data[index + 2]) &&(1 == data[index + 3])) {
                return data[index + 4];
            }
            if ((0 == data[index]) && (0 == data[index + 1]) && (1 == data[index + 2])){
                return data[index + 3];
            }
        }
        return 0;
    }

}
