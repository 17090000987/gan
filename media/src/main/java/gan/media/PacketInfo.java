package gan.media;

import gan.core.Recycleable;

import java.nio.ByteBuffer;

public class PacketInfo implements Recycleable {

    ByteBuffer mByteBuffer;
    BufferInfo mBufferInfo;

    public PacketInfo(ByteBuffer byteBuffer, BufferInfo bufferInfo){
        mByteBuffer = byteBuffer;
        mBufferInfo = bufferInfo;
    }

    public void clear(){
        mByteBuffer.clear();
        mBufferInfo.reset();
    }

    @Override
    public void recycle() {
        clear();
    }

    public PacketInfo bufferInfo(BufferInfo bufferInfo){
        mBufferInfo = bufferInfo;
        return this;
    }

    public PacketInfo offset(int offset){
        mBufferInfo.offset = offset;
        return this;
    }

    public PacketInfo length(int length){
        mBufferInfo.length = length;
        return this;
    }
    public PacketInfo channel(byte channel){
        mBufferInfo.channel = channel;
        return this;
    }

    public PacketInfo time(long time){
        mBufferInfo.time = time;
        return this;
    }

    public ByteBuffer getByteBuffer() {
        return mByteBuffer;
    }

    public BufferInfo getBufferInfo() {
        return mBufferInfo;
    }

    @Override
    public String toString() {
        if(mBufferInfo!=null){
            return mBufferInfo.toString();
        }
        return super.toString();
    }
}
