package gan.media.parser;

import gan.log.DebugLog;
import gan.media.MediaApplication;
import gan.core.system.SystemUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class PSOverTcpStreamParser extends RtpOverTcpStreamParser{

    FrameListener mH264FrameListener;
    ProgramStreamParser mProgramStreamParser;
    private AtomicBoolean runing = new AtomicBoolean(false);

    public PSOverTcpStreamParser(FrameListener listener) {
        super(MediaApplication.getMediaConfig().rtspFrameBufferSize, null);
        mH264FrameListener = listener;
    }

    @Override
    public void start() {
        super.start();
        runing.set(true);
        DebugLog.info("start");
    }

    @Override
    public void stop() {
        if(runing.getAndSet(false)){
            DebugLog.info("stop");
        }
        super.stop();
        if(mProgramStreamParser!=null){
            mProgramStreamParser.stop();
        }
    }

    @Override
    protected void onTcpPacket(byte channel, ByteBuffer packet, int offset, short length) {
        if(!runing.get()){
            return ;
        }

        if(mProgramStreamParser==null){
            long ssrc = SystemUtils.byteToUnsignInt32(packet.array(), 8);
            mProgramStreamParser = new ProgramStreamParser(ssrc).setParseListener(new ProgramStreamParser.PsParseListener() {
                @Override
                public void start() {
                }
                @Override
                public void stop() {
                }
                @Override
                public void onParsed(byte[] data, int offset, int length, int type, long pts) {
                    byte channel = (byte)(type==1?0:2);
                    onFrame(channel,data,offset, length, pts);
                }
            });
            mProgramStreamParser.start();
        }
        offset+=12;
        length-=12;
        mProgramStreamParser.write(packet.array(), offset, length);
    }

    protected void onFrame(byte channel,byte[] frame, int offset, int length, long timeSample){
        if(mH264FrameListener!=null){
            mH264FrameListener.onFrame(channel,frame, offset, length, timeSample);
        }
    }

    public static interface FrameListener {
        public void onFrame(byte channel,byte[] frame, int offset, int length, long timeSample);
    }

}
