package gan.media.parser;

import gan.log.DebugLog;
import gan.media.MediaApplication;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

public class PSOverTcpStreamParser extends RtpOverTcpStreamParser implements PsPacketParser.OnFrameListener {

    FrameListener mH264FrameListener;
    private AtomicBoolean runing = new AtomicBoolean(false);
    PSRtpParser mPSRtpParser;

    public PSOverTcpStreamParser(FrameListener listener) {
        super(MediaApplication.getMediaConfig().rtspFrameBufferSize, null);
        mH264FrameListener = listener;
        mPSRtpParser = new PSRtpParser();
        mPSRtpParser.setParserFrameListener(this);
    }

    @Override
    public void start() {
        super.start();
        runing.set(true);
        DebugLog.info("start");
        mPSRtpParser.init();
    }

    @Override
    public void stop() {
        if(runing.getAndSet(false)){
            DebugLog.info("stop");
        }
        super.stop();
        mPSRtpParser.stop();
    }

    @Override
    protected void onTcpPacket(byte channel, ByteBuffer packet, int offset, int length) {
        if(!runing.get()){
            return ;
        }
        mPSRtpParser.parse(channel, packet, offset, length);
    }

    protected void onFrame(byte channel,byte[] frame, int offset, int length, long timeSample){
        if(mH264FrameListener!=null){
            mH264FrameListener.onFrame(channel,frame, offset, length, timeSample);
        }
    }

    @Override
    public void onPsFrame(byte channel, byte[] data, int offset, int length, long pts) {
        onFrame(channel, data, offset, length, pts);
    }

    public static interface FrameListener {
        public void onFrame(byte channel, byte[] frame, int offset, int length, long timeSample);
    }

}
