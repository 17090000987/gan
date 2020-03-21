package gan.media.parser;

import gan.media.h264.H264Nalu;
import gan.core.system.SystemUtils;

public class ProgramStreamParser {
    private static final int MAX_BUFFER_LENGTH = 1000*1024;
    private static final int MAX_SEARCH_LENGTH = 128;
    private static final int PS_HEADER_LENGTH = 14;
    private byte[] mBuffer;
    private volatile int mLenght;
    private volatile int mParseOffset;
    private PsParseListener mParseListener;
    private long mPts;
    private String mSsrc;
    private boolean mIsError;
    private long mVideoPts;
    private long mAudioPts;
    private long mLastVideoPts;
    private long mLastAudioPts;
    private Nalu mNalu;

    public ProgramStreamParser(long ssrc){
        this(String.valueOf(ssrc));
    }

    public ProgramStreamParser(String ssrc){
        mSsrc = ssrc;
        mBuffer = new byte[MAX_BUFFER_LENGTH];
    }

    public ProgramStreamParser setParseListener(PsParseListener listener){
        mParseListener = listener;
        return this;
    }

    public PsParseListener getParseListener(){
        return mParseListener;
    }

    public synchronized void write(byte[] buf, int offset, int length){
        if(buf == null || offset < 0 || length < 0 || buf.length < offset+length){
            return;
        }else{
            if(mLenght+length > MAX_BUFFER_LENGTH){
                if(mParseOffset > 0){
                    System.arraycopy(mBuffer, mParseOffset, mBuffer, 0, mLenght - mParseOffset);
                    mLenght -= mParseOffset;
                    mParseOffset = 0;
                }else{
                    if(!mIsError){
                        mIsError = true;
                    }
                    parseProgramStream();
                    return;
                }
            }

            ensureBufferCapacity(mLenght + length);
            try {
                System.arraycopy(buf, offset, mBuffer, mLenght, length);
            } catch (Exception e) {
                e.printStackTrace();
            }

            mLenght += length;
            parseProgramStream();
        }
    }

    private void ensureBufferCapacity(int minCapacity){
        int oldCapacity = mBuffer.length;
        if(oldCapacity <= minCapacity && minCapacity < 10){
            int newCapacity = oldCapacity + (oldCapacity >> 1);
            byte[] newFrameBuffer = new byte[newCapacity];
            System.arraycopy(mBuffer, 0, newFrameBuffer, 0, oldCapacity);
            mBuffer = newFrameBuffer;
        }
    }

    private synchronized void parseProgramStream(){
        int streamType = -1;
        if(mLenght < mParseOffset + PS_HEADER_LENGTH){
            //ps header包未接收完，需等待包完整后解析
            return;
        }else if(isPSStartCode(mBuffer, mParseOffset)){
            mParseOffset += PS_HEADER_LENGTH;
            while(mBuffer[mParseOffset]!=0 || mBuffer[mParseOffset+1]!=0){
                ++mParseOffset;
            }
        }else if(isSHStartCode(mBuffer, mParseOffset)){
            int SystemHeaderLength = SystemUtils.byteToUnsignInt16(mBuffer, mParseOffset+4);
            int shRealLength = SystemHeaderLength + 4 + 2;
            if((mParseOffset + shRealLength) < mLenght){
                mParseOffset += shRealLength;
            }else{
                //system header包未接收完，需等待包完整后解析
                return;
            }
        }else if(isPSMStartCode(mBuffer, mParseOffset)){
            int PSMLength = SystemUtils.byteToUnsignInt16(mBuffer, mParseOffset+4);
            int psmRealLength = PSMLength + 4 + 2;
            if((mParseOffset + psmRealLength) < mLenght){
                mParseOffset += psmRealLength;
            }else{
                //ps map包未接收完，需等待包完整后解析
                return;
            }
        }else if((streamType = isPESStartCode(mBuffer, mParseOffset)) > 0){
            int pesLength = SystemUtils.byteToUnsignInt16(mBuffer, mParseOffset+4);
            int pesRealLength = pesLength + 4 + 2;
            int pesHeaderLength = SystemUtils.byteToUnsignInt8(mBuffer, mParseOffset+8);
            int pesRealHeaderLength = pesHeaderLength + 9;
            if(mParseOffset + pesRealLength < mLenght){
                byte[] pesdata = new byte[pesRealLength];
                System.arraycopy(mBuffer, mParseOffset, pesdata, 0, pesRealLength);

                //10b为有pts 11b有pts和dts 00为无pts和dts 01b是不允许的
                int ptsFlag = (mBuffer[mParseOffset+7] & 0xC0) >>> 6;
                if(ptsFlag == 2){
                    mPts = decodePts(mBuffer, mParseOffset+9);
                }
                int payloadLenght = pesRealLength - pesRealHeaderLength;

                //h264视频
                if(streamType == 1){
                    int startCodeLength = getH264StartCodeLength(pesdata, pesRealHeaderLength);
                    if(startCodeLength > 0){
                        int searchLength = (pesRealLength > MAX_SEARCH_LENGTH) ? MAX_SEARCH_LENGTH : pesRealLength;
                        int offset = pesRealHeaderLength;

                        for (int i = offset; i < searchLength; i++) {
                            int startCodeLength2 = getH264StartCodeLength(pesdata, i);
                            if(startCodeLength2 > 0){
                                byte naluHeader = pesdata[i+startCodeLength2];
                                int naluType = ((int)naluHeader) & 0x1F;
                                int length = i - offset;

                                if(mNalu == null){
                                    mNalu = new Nalu(naluType);
                                }

                                Nalu naluTail = findNaluTail();
                                if(naluTail.data != null){
                                    naluTail.isDataOk = true;

                                    Nalu nextNalu = new Nalu(naluType);
                                    naluTail.next = nextNalu;
                                }else if(length > 0){
                                    byte[] data = new byte[length];
                                    System.arraycopy(pesdata, offset, data, 0, length);
                                    naluTail.data = data;
                                    naluTail.length = length;
                                    naluTail.isDataOk = true;

                                    Nalu nextNalu = new Nalu(naluType);
                                    naluTail.next = nextNalu;
                                }

                                offset = i;

                                //跳过当前startcode
                                i += startCodeLength2;
                            }

                            if(i == searchLength-1){
                                int length = pesRealLength - offset;
                                Nalu naluTail = findNaluTail();
                                if(naluTail.data == null){
                                    naluTail.data = new byte[length];
                                    naluTail.length = length;
                                    System.arraycopy(pesdata, offset, naluTail.data, 0, length);
                                }else{
                                    int newLength = naluTail.length + length;
                                    byte[] temp = new byte[newLength];
                                    System.arraycopy(naluTail.data, 0, temp, 0, naluTail.length);
                                    System.arraycopy(pesdata, offset, temp, naluTail.length, length);
                                    naluTail.data = temp;
                                    naluTail.length = newLength;
                                }
                            }
                        }
                    }else{
                        Nalu nalu = findNaluTail();
                        int newLength = nalu.length + payloadLenght;
                        byte[] temp = new byte[newLength];
                        System.arraycopy(nalu.data, 0, temp, 0, nalu.length);
                        System.arraycopy(pesdata, pesRealHeaderLength, temp, nalu.length, payloadLenght);
                        nalu.data = temp;
                        nalu.length = newLength;
                    }


                    while(mNalu!=null && mNalu.isDataOk){
                        if(isAcceptType(mNalu.type)){
                            onParsed(mNalu.data, 0, mNalu.length, 1, mPts, mNalu.type == H264Nalu.IDR_SLICE);
                        }
                        mNalu = mNalu.next;
                    }
                }else{
                    onParsed(pesdata, pesRealHeaderLength, payloadLenght, streamType, mPts, false);
                }
                mParseOffset += pesRealLength;
            }else{
                //pes包未接收完，需等待包完整后解析
                return;
            }
        }else if(isPrivateStreamStartCode(mBuffer, mParseOffset)){
            int length = SystemUtils.byteToUnsignInt16(mBuffer, mParseOffset+4);
            int realLength = length + 4 + 2;
            if((mParseOffset + realLength) < mLenght){
                mParseOffset += realLength;
            }else{
                //private stream包未接收完，需等待包完整后解析
                return;
            }
        }else{
            mParseOffset++;
        }

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        parseProgramStream();
    }

    protected void onParsed(byte[] data, int offset, int length, int type, long pts, boolean isKeyFrame){
        if(mParseListener != null){
            if(type == 1){
                if(mLastVideoPts > 0){
                    long time = pts - mLastVideoPts;
                    if(time < 0 || time > 3600*3){
                        mVideoPts += 3600;
                    }else{
                        mVideoPts += time;
                    }
                }
                mParseListener.onParsed(data, offset, length, type, mVideoPts);
                mLastVideoPts = pts;
            }else{
                if(mLastAudioPts > 0){
                    long time = pts - mLastAudioPts;
                    if(time < 0 || time > 10000*2){
                        mAudioPts += 10000;
                    }else{
                        mAudioPts += time;
                    }
                }
                mParseListener.onParsed(data, offset, length, type, mAudioPts);
                mLastAudioPts = pts;
            }
        }
    }

    //Program Header
    private boolean isPSStartCode(byte[] buf, int offset){
        if(offset + 10 < mLenght && buf[offset] == 0 && buf[offset+1] == 0 && buf[offset+2] == 0x1 && SystemUtils.byteToUnsignInt8(buf, offset+3) == 0xBA){
            return true;
        }
        return false;
    }

    //System Header
    private boolean isSHStartCode(byte[] buf, int offset){
        if(offset + 10 < mLenght && buf[offset] == 0 && buf[offset+1] == 0 && buf[offset+2] == 0x1 && SystemUtils.byteToUnsignInt8(buf, offset+3) == 0xBB){
            return true;
        }
        return false;
    }

    //Program Stream Map
    private boolean isPSMStartCode(byte[] buf, int offset){
        if(offset + 10 < mLenght &&  buf[offset] == 0 && buf[offset+1] == 0 && buf[offset+2] == 0x1 && SystemUtils.byteToUnsignInt8(buf, offset+3) == 0xBC){
            return true;
        }
        return false;
    }

    //private_stream_1
    private boolean isPrivateStreamStartCode(byte[] buf, int offset){
        if(offset + 10 < mLenght &&  buf[offset] == 0 && buf[offset+1] == 0 && buf[offset+2] == 0x1 && SystemUtils.byteToUnsignInt8(buf, offset+3) == 0xBD){
            return true;
        }
        return false;
    }

    //Packetized Elementary Stream
    private int isPESStartCode(byte[] buf, int offset){
        if(offset + 10 < mLenght && buf[offset] == 0 && buf[offset+1] == 0 && buf[offset+2] == 0x1){
            int flag = SystemUtils.byteToUnsignInt8(buf, offset+3);
            if(flag >= 0xE0 && flag <= 0xEF){
                //Video
                return 1;
            }else if(flag >= 0xC0 && flag <= 0xDF){
                //Audio
                return 2;
            }
        }
        return -1;
    }

    //返回0则不是h264 startcode
    private int getH264StartCodeLength(byte[] buf, int offset){
        if(buf[offset] == 0 && buf[offset+1] == 0){
            if(buf[offset+2] == 1){
                return 3;
            }else if(buf[offset+2] == 0 && buf[offset+3] == 1){
                return 4;
            }
        }
        return 0;
    }

    public long decodePts(byte[] buf, int offset){
        return (((long)buf[offset] & 0xEL) << 29) +
                (((long)buf[offset+1] & 0xFFL) << 22) +
                (((long)buf[offset+2] & 0xFEL) << 14) +
                (((long)buf[offset+3] & 0xFFL) << 7) +
                (((long)buf[offset+4] & 0xFEL) >>> 1);
    }

    public void start() {
        if(mParseListener != null){
            mParseListener.start();
        }
    }

    public void stop() {
        if(mParseListener != null){
            mParseListener.stop();
        }
    }

    public void onParsed(byte[] buffer, int offset, int length) {
        write(buffer, offset, length);
    }

    private Nalu findNaluTail(){
        Nalu nalu = mNalu;
        while(nalu.next != null){
            nalu = nalu.next;
        }
        return nalu;
    }

    private boolean isAcceptType(int type){
        if ( mNalu.type == H264Nalu.SLICE ||
                mNalu.type == H264Nalu.IDR_SLICE ||
                mNalu.type == H264Nalu.SPS ||
                mNalu.type == H264Nalu.PPS ) {
            return true;
        }
        return false;
    }

    public interface PsParseListener{
        void start();
        void stop();
        //type 1为视频  2为音频
        void onParsed(byte[] data, int offset, int length, int type, long pts);
    }

    public class Nalu {
        public int type;
        public byte[] data;
        public int length;
        public boolean isDataOk;
        public Nalu next;

        public Nalu(int t){
            type = t;
        }
    }
}
