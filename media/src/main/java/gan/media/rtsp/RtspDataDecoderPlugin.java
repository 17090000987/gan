package gan.media.rtsp;

import gan.log.FileLogger;
import gan.media.*;
import gan.media.codec.NativeDecoder;
import gan.media.codec.RawDataCallBack;
import gan.core.system.server.ServerPlugin;
import gan.core.system.server.SystemServer;

import java.io.IOException;
import java.nio.ByteBuffer;

public class RtspDataDecoderPlugin extends ServerPlugin<RtspMediaServer> implements RtspMediaServer.OnFrameCallBackPlugin, MediaOutputStream, RawDataCallBack {

    MediaOutputStreamRunnableFrame mMediaOutputStreamRunnable;
    NativeDecoder mDecoder;
    FileLogger mLogger;

    public static RtspDataDecoderPlugin singleInstance(RtspMediaServer server){
        RtspDataDecoderPlugin plugin = (RtspDataDecoderPlugin) server.getIdTag(RtspDataDecoderPlugin.class.getName());
        if(plugin==null){
            server.registerPlugin(plugin = new RtspDataDecoderPlugin());
            server.setIdTag(RtspDataDecoderPlugin.class.getName(),plugin);
        }
        return plugin;
    }

    @Override
    protected void onCreate(RtspMediaServer server) {
        super.onCreate(server);
        mLogger = RtspMediaServerManager.getLogger(server.getUrl());
        MediaOutputInfo outInfo = new MediaOutputInfo(server.getUrl());
        mMediaOutputStreamRunnable = new MediaOutputStreamRunnableFrame(this, outInfo);
        SystemServer.executeThread(mMediaOutputStreamRunnable);
    }

    @Override
    protected void onDestory() {
        super.onDestory();
        mMediaOutputStreamRunnable.close();
    }

    @Override
    public void onFrame(byte channel, ByteBuffer frame, BufferInfo bufferInfo) {
        mMediaOutputStreamRunnable.putPacket(channel,frame.array(), bufferInfo.offset, bufferInfo.length, bufferInfo.time);
    }

    @Override
    public void init() {
        mDecoder = new NativeDecoder();
        mDecoder.setRawDataCallBack(this);
        mDecoder.init();
    }

    @Override
    public void write(byte channel, ByteBuffer packet, BufferInfo bufferInfo) throws IOException {
        if(channel==0){
            mDecoder.decode(packet.array(),bufferInfo.offset,bufferInfo.length, bufferInfo.time, 0);
        }
    }

    @Override
    public void close() {
        mDecoder.release();
    }

    @Override
    public void onRawFrame(byte[] data, int length, long timestamp, int width, int height) {
        mServer.outputPacketStream(MediaOutputStreamRunnable.PacketType_Raw, (byte)0, data, 0, length, timestamp);
    }

}
