package gan.media.parser;

import gan.core.file.FileHelper;
import gan.core.system.SystemUtils;
import gan.core.system.server.SystemServer;
import gan.log.DebugLog;
import gan.media.MediaApplication;
import gan.media.PacketInfo;
import gan.media.h26x.HUtils;

import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class PsPacketParser implements PacketParser{

    OnFrameListener mOnFrameListener;
    PacketInfo mPsPacketInfo;
    PacketInfo mTempPacketInfo;
    PacketInfo mVideoDataPacketInfo;
    List<PesPacket> mPesPacket;
    long fileTime;
    public String mVideoId;
    private boolean resetFlag;
    private boolean mIsCheckData;

    public PsPacketParser(){
        fileTime = System.currentTimeMillis();
        mPesPacket =  new ArrayList<>();
        int bufferSize = MediaApplication.getMediaConfig().rtspFrameBufferSize;
        mPsPacketInfo = new PacketInfo(bufferSize);
        mTempPacketInfo = new PacketInfo(bufferSize);
        mVideoDataPacketInfo = new PacketInfo(bufferSize);
    }

    public void setVideoId(String videoId) {
        this.mVideoId = videoId;
    }

    public String formatFileName(){
        if(mVideoId!=null){
            return String.format("%s_%s",mVideoId,fileTime);
        }
        return String.valueOf(fileTime);
    }

    public void setIsCheckData(boolean isCheckData) {
        this.mIsCheckData = isCheckData;
    }

    int parseIndex;
    @Override
    public void parse(ByteBuffer packet, int offset, int length) {
        appendPsData(packet, offset, length);
        try {
            parseIndex = parsePsPacket(parseIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    FileOutputStream fos_ps;
    public void appendPsData(ByteBuffer packet, int offset, int length){
//        if(SystemServer.IsDebug()){
//            try{
//                if(fos_ps==null){
//                    final String fileName = String.format("/logs/ps/ps_%s.ps",formatFileName());
//                    fos_ps = FileHelper.createFileOutputStream(SystemServer.getRootPath(fileName));
//                }
//                fos_ps.write(packet.array(), offset, length);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }
        mPsPacketInfo.putAndCopy(packet.array(), offset, length);
    }

    @Override
    public void stop() {
        SystemUtils.close(fos);
        SystemUtils.close(fos_ps);
    }

    FileOutputStream fos;
    public int parsePsPacket(int start)throws Exception{
        mPsPacketInfo.offset(start);
        if(mPsPacketInfo.offsetLength()<0){
            parsePesPayloadFrame();
            return start;
        }

        if(resetFlag){
            if(checkPsBuffer()){
                mPesPacket.clear();
                resetFlag = false;
                start = parseIndex;
            }else{
                return start;
            }
        }

        try {
            parsePesPayloadFrame();
            resetPsPacketInfo(start);
            parsePsPacket(mPsPacketInfo);
            parseIndex = mPsPacketInfo.offset();
            parsePesPayloadFrame();
            return parseIndex;
        } catch (Exception e) {
            e.printStackTrace();
        }

        mPesPacket.clear();
        mTempPacketInfo.clear();
        mPsPacketInfo.clear();
        mTempPacketInfo.clear();
        return parseIndex = 0;
    }

    private void resetPsPacketInfo(int start){
        mPsPacketInfo.offset(start);
        if(mPsPacketInfo.offsetLength()>0){
            mTempPacketInfo.clear();
            mTempPacketInfo.putAndCopy(mPsPacketInfo.array(), mPsPacketInfo.offset(), mPsPacketInfo.offsetLength());
            mPsPacketInfo.clear();
            mPsPacketInfo.putAndCopy(mTempPacketInfo.array(), 0, mTempPacketInfo.length());
            mTempPacketInfo.clear();
            mPsPacketInfo.offset(parseIndex = 0);
        }else if(mPsPacketInfo.offsetLength()==0){
            mPsPacketInfo.clear();
            mPsPacketInfo.offset(parseIndex = 0);
        }
    }

    public boolean checkPsBuffer(){
        int startIndex = findPsStartCodeIndex(mPsPacketInfo.array(),mPsPacketInfo.offset(),mPsPacketInfo.offsetLength());
        if(startIndex>=0){
            parseIndex = mPsPacketInfo.offsets(startIndex);
            resetPsPacketInfo(parseIndex);
            mPesPacket.clear();
            return true;
        }
        return false;
    }

    public void bufferPesData(){
        int dataIndex;
        do{
            if(mPesPacket.size()>0){
                PesPacket pesPacket = mPesPacket.get(0);
                dataIndex = pesPacket.payloadOffset+pesPacket.payloadLen;
                if(mPsPacketInfo.dataLength()>=dataIndex){
                    try{
                        if(pesPacket.payloadType==1){
                            mVideoDataPacketInfo.channel((byte) 0);
                            if(pesPacket.pts>mVideoDataPacketInfo.time()){
                                mVideoDataPacketInfo.time(pesPacket.pts);
                            }

//                            if(SystemServer.IsDebug()){
//                                try{
//                                    if(fos==null){
//                                        final String fileName = String.format("/logs/h264/h264_%s.h264",formatFileName());
//                                        fos = FileHelper.createFileOutputStream(SystemServer.getRootPath(fileName));
//                                    }
//                                    fos.write(mPsPacketInfo.array(),pesPacket.payloadOffset,pesPacket.payloadLen);
//                                }catch (Exception e){
//                                    e.printStackTrace();
//                                }
//                            }

                            if(mIsCheckData){//不建议开启。只在一些特殊情况。
                                PacketInfo packet = PacketInfo.wrap(mPsPacketInfo.array(), pesPacket.payloadOffset, pesPacket.payloadLen);
                                int startIndex;
                                while ((startIndex = findStartCodeIndex(packet.array(), packet.offset(), packet.offsetLength()))>=0) {
                                    int offset = packet.offsets(startIndex);
                                    int flag = SystemUtils.byte2UnsignInt8(packet.get(offset + 3));
                                    if (flag >= 0xE0 && flag <= 0xEF) {//视频
                                        DebugLog.info("video data find pes");
                                        //PesPacket pesPacket1 = parsePesVideoData(packet);
                                        //if(pesPacket1!=null){
                                            //mVideoDataPacketInfo.channel((byte) 0);
                                            //if(pesPacket.pts>mVideoDataPacketInfo.time()){
                                               //mVideoDataPacketInfo.time(pesPacket1.pts);
                                            //}
                                            //mVideoDataPacketInfo.putAndCopy(packet.array(), pesPacket1.payloadOffset, pesPacket1.payloadLen);
                                        //}
                                        resetFlag = true;
                                        return;
                                    }else if(flag >= 0xC0 && flag <= 0xDF){
                                        DebugLog.info("video data find pes");
                                        //packet.offsets(4);
                                        //packet.get(b2);
                                        //int pesLength=SystemUtils.byteToUnsignInt16(b2, 0);
                                        //packet.offsets(pesLength);
                                        resetFlag = true;
                                        return;
                                    }else{
                                        if(isPsType(flag)){
                                            resetFlag = true;
                                            return;
                                        }
                                    }

                                    if(packet.offsetLength()<=0){
                                        return;
                                    }
                                }
                                mVideoDataPacketInfo.putAndCopy(mPsPacketInfo.array(), pesPacket.payloadOffset, pesPacket.payloadLen);
                            }else{
                                mVideoDataPacketInfo.putAndCopy(mPsPacketInfo.array(), pesPacket.payloadOffset, pesPacket.payloadLen);
                            }
                        }
                    }finally {
                        mPesPacket.remove(0);
                    }
                }else{
                    break;
                }
            }else{
                break;
            }
        }while (mPsPacketInfo.dataLength()>=dataIndex);
    }

    public static int findPsLen(byte[] data, int offset, int length){
        int pos0=-1;
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if (isStartCodeIndex(data, index, 0xba)) {
                pos0 = index;
                break;
            }
        }
        if(pos0<0){
            return 0;
        }
        int pos1=-1;
        for (int i = pos0+4; i < length - 4; i++) {
            if ((isStartCodeIndex(data, i, 0xba))) {
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

    long lastVideoTime;
    public void parsePesPayloadFrame(){
        bufferPesData();

        PacketInfo packet = mVideoDataPacketInfo;
        packet.offset(0);
        int startIndex = HUtils.findStartCodeOffset(packet.array(), 0, packet.length());
        if(startIndex>=0){
            packet.offsets(startIndex);
        }else{
            return;
        }

        int packetLength;
        while((packetLength = packet.offsetLength())>0) {
            int frameLen = HUtils.frameLen(packet.array(), packet.offset(), packetLength);
            if(frameLen>=packetLength){
                startIndex= HUtils.findStartCodeOffset(packet.array(), packet.offset(), packet.offsetLength());
                if(startIndex>0){
                    packet.offsets(startIndex);
                }
                mTempPacketInfo.clear();
                mTempPacketInfo.putAndCopy(packet.array(), packet.offset(), packet.offsetLength());
                mTempPacketInfo.time(mVideoDataPacketInfo.time());
                mVideoDataPacketInfo.clear();
                mVideoDataPacketInfo.putAndCopy(mTempPacketInfo.array(), 0, mTempPacketInfo.length());
                mVideoDataPacketInfo.time(lastVideoTime = mTempPacketInfo.time());
                mTempPacketInfo.clear();
                break;
            }
            onFrame(packet.channel(), packet.array(), packet.offset(), frameLen, fixVideoTime(packet.time()));
            packet.offsets(frameLen);
        }
    }

    public long fixVideoTime(long time){
        long timeSample = time;
        if(lastVideoTime<time){
            timeSample = lastVideoTime;
        }
        lastVideoTime = time;
        return timeSample;
    }

    byte[] b2 = new byte[2];
    private void parsePsPacket(PacketInfo packet) throws Exception {
        int startIndex;
        while ((startIndex = findStartCodeIndex(packet.array(), packet.offset(), packet.offsetLength()))>=0){
            int offset = packet.offsets(startIndex);
            int flag = SystemUtils.byte2UnsignInt8(packet.get(offset+3));
            if(flag >= 0xE0 && flag <= 0xEF){//视频
                PesPacket pesPacket = parsePesVideoData(packet);
                if(pesPacket!=null){
                    mPesPacket.add(pesPacket);
                }
            }else if(flag >= 0xC0 && flag <= 0xDF){//语音
                packet.offsets(4);
                PesPacket pesPacket = new PesPacket();
                pesPacket.payloadType = 2;
                packet.get(b2);
                int pesLength=SystemUtils.byteToUnsignInt16(b2, 0);
                int parseIndex = packet.offset()+pesLength;
                pesPacket.payloadOffset=packet.offset();
                pesPacket.payloadLen=parseIndex-pesPacket.payloadOffset;
                mPesPacket.add(pesPacket);
                packet.offset(parseIndex);
            }else{
                if(isPsType(flag)){
                    if(parsePsHeader(packet)){
                    }else{
                        packet.offsets(4);
                    }
                }else{
                    packet.offsets(4);
                }
            }

            if(packet.offsetLength()<=0){
                break;
            }
        }
    }

    private PesPacket parsePesVideoData(PacketInfo packet){
        packet.offsets(4);
        int offset;
        PesPacket pesPacket = new PesPacket();
        pesPacket.payloadType = 1;
        packet.get(b2);
        int pesLength=SystemUtils.byteToUnsignInt16(b2, 0);
        int parseIndex = packet.offset()+pesLength;
        if(pesLength<=0){
            DebugLog.info("pesLength<=0");
            parseIndex = packet.dataLength();
            packet.offset(parseIndex);
            return null;
        }

        byte stream_flag = packet.get();
        if(((stream_flag & 0xC0) !=0X80)){//因为pes加扰控制位总是设置为10，所以如果与出来的结果不为0x80，则表明此段流为不标准的，不解析
            DebugLog.info("stream_flag !=0X80");
            packet.offset(parseIndex);
            return null;
        }

        /**
         * 值为'10'时，PTS 字段应出现在PES 分组标题中；当值为'11'时，PTS 字段和DTS 字段都应出现在PES 分组标题中；当值为'00'时，PTS 字段和DTS 字段都不出现在PES分组标题中。值'01'是不允许的。
         */
        byte pts_dts = packet.get();
        int exLength = SystemUtils.byte2UnsignInt8(packet.get());
        long pts;
        if((pts_dts&0x80)==0x80
                ||(pts_dts&0xc0)==0xc0){//10//11
            offset = packet.offset();
            pts = decodePts(packet.array(), offset);
        }else if((pts_dts&0xff)==0x00){//00
            pts = 0;
        }else{
            DebugLog.info("pts_dts !=0X80 0xc0 0x00");
            packet.offset(parseIndex);
            return null;
        }
        offset = packet.offsets(exLength);
        pesPacket.pts = pts;
        pesPacket.payloadOffset = offset;
        pesPacket.payloadLen = parseIndex-offset;
        packet.offset(parseIndex);
        return pesPacket;
    }

    public static long decodePts(byte[] buf, int offset){
        return (((long)buf[offset] & 0xEL) << 29) +
                (((long)buf[offset+1] & 0xFFL) << 22) +
                (((long)buf[offset+2] & 0xFEL) << 14) +
                (((long)buf[offset+3] & 0xFFL) << 7) +
                (((long)buf[offset+4] & 0xFEL) >>> 1);
    }

    public boolean isPsType(int type){
        return type == 0xba||type == 0xbb||type == 0xbc||type == 0xbd;
    }

    public boolean parsePsType(PacketInfo packet,int type){
        if(type == 0xba){
            //如果offset后边不是紧跟着 startCode,重新计算数据开始位置，和长度
            packet.offsets(10);
            return true;
        }

        int headerLength;
        if(type == 0xbb){
            packet.get(b2);
            headerLength = SystemUtils.byteToUnsignInt16(b2,0);
            packet.offsets(headerLength);
            return true;
        }

        if(type == 0xbc){
            packet.get(b2);
            headerLength = SystemUtils.byteToUnsignInt16(b2,0);
            packet.offsets(headerLength);
            return true;
        }

        if(type == 0xbd){
            packet.get(b2);
            headerLength = SystemUtils.byteToUnsignInt16(b2,0);
            packet.offsets(headerLength);
            return true;
        }

        return false;
    }

    public boolean parsePsHeader(PacketInfo packet){
        boolean handle = false;
        int startIndex = findPsStartCodeIndex(packet.array(), packet.offset(), packet.offsetLength());
        if(startIndex>=0){
            //如果offset后边不是紧跟着 startCode,重新计算数据开始位置，和长度
            packet.offsets(startIndex+14);
            handle = true;
        }

        int headerLength;
        startIndex = findPsSystemHeaderCodeIndex(packet.array(), packet.offset(), 10);
        if(startIndex>=0){
            packet.offsets(startIndex+4);
            packet.get(b2);
            headerLength = SystemUtils.byteToUnsignInt16(b2,0);
            packet.offsets(headerLength);
            handle = true;
        }

        startIndex = findPSMCodeIndex(packet.array(), packet.offset(), 10);
        if(startIndex>=0){
            packet.offsets(startIndex+4);
            packet.get(b2);
            headerLength = SystemUtils.byteToUnsignInt16(b2,0);
            packet.offsets(headerLength);
            handle = true;
        }

        startIndex = findPrivateCodeIndex(packet.array(), packet.offset(), 10);
        if(startIndex>=0){
            packet.offsets(startIndex+4);
            packet.get(b2);
            headerLength = SystemUtils.byteToUnsignInt16(b2,0);
            packet.offsets(headerLength);
            handle = true;
        }

        return handle;
    }

    FileOutputStream frame_fos;
    public void onFrame(byte channel, byte[] data,int offset, int length, long pts){
//        if(SystemServer.IsDebug()){
//            try{
//                if(frame_fos==null){
//                    final String fileName = String.format("/logs/h264/frame_%s.h264", formatFileName());
//                    frame_fos = FileHelper.createFileOutputStream(SystemServer.getRootPath(fileName));
//                }
//                frame_fos.write(data,offset,length);
//            }catch (Exception e){
//                e.printStackTrace();
//            }
//        }

        if(mOnFrameListener!=null){
            mOnFrameListener.onPsFrame(channel, data, offset, length, pts);
        }
    }

    public PsPacketParser setOnFrameListener(OnFrameListener onFrameListener) {
        this.mOnFrameListener = onFrameListener;
        return this;
    }

    public static interface OnFrameListener{
        public void onPsFrame(byte channel, byte[] data, int offset, int length, long pts);
    }

    public static int findPsStartCodeIndex(byte[] data, int offset, int length){
        return findStartCodeIndex(data,offset, length, 0xba);
    }

    public static int findPsSystemHeaderCodeIndex(byte[] data, int offset, int length){
        return findStartCodeIndex(data,offset, length,  0xbb);
    }

    public static int findPSMCodeIndex(byte[] data, int offset, int length){
        return findStartCodeIndex(data,offset, length, 0xbc);
    }

    public static int findPrivateCodeIndex(byte[] data, int offset, int length){
        return findStartCodeIndex(data,offset, length, 0xbd);
    }

    public static int findStartCodeIndex(byte[] data, int offset, int length){
        try{
            int index;
            for (int i = 0; i < length - 4; i++) {
                index = i+offset;
                if ((0 == data[index])
                        && (0 == data[index + 1])
                        && (1 == data[index + 2])) {
                    return i;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean isStartCodeIndex(byte[] data,int index,int type){
        if((0 == data[index])
                && (0 == data[index + 1])
                && (1 == data[index + 2])){
            return SystemUtils.byte2UnsignInt8(data[index+3])==type;
        }
        return false;
    }

    public static int findStartCodeIndex(byte[] data,int offset,int length,int type){
        int index;
        for (int i = 0; i < length - 4; i++) {
            index = i+offset;
            if (isStartCodeIndex(data,index,type)) {
                return i;
            }
        }
        return -1;
    }

}
