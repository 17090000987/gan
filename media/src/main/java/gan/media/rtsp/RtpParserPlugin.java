package gan.media.rtsp;

import gan.core.system.server.ServerPlugin;
import gan.media.BufferInfo;
import gan.media.h264.H264RtpParser;
import gan.media.h265.H265RtpParser;
import gan.media.parser.AACRtpParser;
import gan.media.parser.RtpParser;

import java.nio.ByteBuffer;

public class RtpParserPlugin extends ServerPlugin<RtspMediaServer> implements RtpParser.OnParserListener, RtpParser.OnParserFrameListener ,RtspMediaServer.OnStreamStateListenerPlugin {

    RtpParser mVideoRtpParser;
    RtpParser mAudioRtpParser;

    public static RtpParserPlugin get(RtspMediaServer server){
        RtpParserPlugin plugin = (RtpParserPlugin) server.getIdTag(RtpParserPlugin.class.getName());
        if(plugin==null){
            server.registerPlugin(plugin = new RtpParserPlugin());
            server.setIdTag(RtpParserPlugin.class.getName(),plugin);
        }
        return plugin;
    }

    @Override
    public void onStreamStarted() {
        if(mVideoRtpParser!=null){
            mVideoRtpParser.init();
        }
        if(mAudioRtpParser!=null){
            mAudioRtpParser.init();
        }
    }

    @Override
    public void onStreamStop() {
        if(mVideoRtpParser!=null){
            mVideoRtpParser.stop();
        }
        if(mAudioRtpParser!=null){
            mAudioRtpParser.stop();
        }
    }

    public void initParser(Sdp sdp){
        if(sdp.isVCodec("h264")){
            H264RtpParser videoRtpParser = new H264RtpParser();
            videoRtpParser.setParserListenner(this);
            videoRtpParser.setParserFrameListener(this);
            mVideoRtpParser = videoRtpParser;
        }else if(sdp.isVCodec("h265")){
            H265RtpParser videoRtpParser = new H265RtpParser();
            videoRtpParser.setParserListenner(this);
            videoRtpParser.setParserFrameListener(this);
            mVideoRtpParser = videoRtpParser;
        }

        if(sdp.isACodec("MPEG4-GENERIC")){
            AACRtpParser parser = new AACRtpParser();
            parser.setParserListenner(this);
            parser.setParserFrameListener(this);
            mAudioRtpParser = parser;
        }
    }

    public void parseVideoFrame(byte channel,ByteBuffer packet, int offset, short length){
        if(mVideoRtpParser!=null){
            mVideoRtpParser.parse(channel,packet,offset,length);
        }
    }

    public void parseAudioFrame(byte channel,ByteBuffer packet,int offset,short length){
        if(mAudioRtpParser!=null){
            mAudioRtpParser.parse(channel,packet,offset,length);
        }
    }

    @Override
    public void onParsedPacket(byte channel, byte[] packet, int offset, int length, long time) {
        mServer.receivePacket(channel,packet,offset,length,time);
    }

    @Override
    public void onParsedFrame(byte channel, ByteBuffer frame, BufferInfo frameInfo) {
        mServer.onFrame(channel,frame,frameInfo);
    }
}
