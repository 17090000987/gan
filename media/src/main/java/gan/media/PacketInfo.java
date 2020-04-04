package gan.media;

import gan.core.Recycleable;

import java.nio.ByteBuffer;

public class PacketInfo implements Recycleable {

    ByteBuffer mByteBuffer;
    BufferInfo mBufferInfo;
    private int putIndex;

    public PacketInfo(int capacity){
        this(ByteBuffer.allocate(capacity),new BufferInfo());
    }

    public PacketInfo(ByteBuffer byteBuffer, BufferInfo bufferInfo){
        mByteBuffer = byteBuffer;
        mBufferInfo = bufferInfo;
    }

    public void clear(){
        mByteBuffer.clear();
        mBufferInfo.reset();
        putIndex = 0;
    }

    public static PacketInfo wrap(byte[] array, int offset, int length){
        PacketInfo packetInfo = new PacketInfo(ByteBuffer.wrap(array, offset, length),new BufferInfo());
        return packetInfo;
    }

    public int limit(){
        return mByteBuffer.limit();
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
        mByteBuffer.position(mBufferInfo.offset);
        return this;
    }

    public int offsets(int offset){
        mBufferInfo.offset += offset;
        mByteBuffer.position(mBufferInfo.offset);
        return mBufferInfo.offset;
    }

    public int offset(){
        return mBufferInfo.offset;
    }

    public PacketInfo length(int length){
        mBufferInfo.length = length;
        return this;
    }

    public PacketInfo channel(byte channel){
        mBufferInfo.channel = channel;
        return this;
    }

    public byte channel(){
        return mBufferInfo.channel;
    }

    public PacketInfo time(long time){
        mBufferInfo.time = time;
        return this;
    }

    public long time(){
        return mBufferInfo.time;
    }

    public int length(){
        return mBufferInfo.length;
    }

    public int offsetLength(){
        return mBufferInfo.offsetLength();
    }

    @Override
    public String toString() {
        if(mBufferInfo!=null){
            return mBufferInfo.toString();
        }
        return super.toString();
    }

    public PacketInfo put(byte[] data,int offset,int length){
        mByteBuffer.position(putIndex);
        mByteBuffer.put(data, offset, length);
        mBufferInfo.lengths(length).offsets(length);
        putIndex+=length;
        return this;
    }

    public PacketInfo put(byte[] data,int offset,int length,int putIndex){
        mByteBuffer.position(this.putIndex = putIndex);
        mByteBuffer.put(data, offset, length);
        mBufferInfo.lengths(length).offsets(length);
        this.putIndex+=length;
        return this;
    }

    public PacketInfo putAndCopy(byte[] data,int offset,int length){
        System.arraycopy(data, offset, array(), putIndex, length);
        mBufferInfo.lengths(length).offsets(length);
        putIndex+=length;
        return this;
    }

    public PacketInfo putAndCopy(byte[] data,int offset,int length,int putIndex){
        this.putIndex = putIndex;
        System.arraycopy(data, offset, array(), putIndex, length);
        mBufferInfo.lengths(length).offsets(length);
        this.putIndex+=length;
        return this;
    }

    public byte[] array(){
        return mByteBuffer.array();
    }

    public byte get(){
        byte data = mByteBuffer.get();
        mBufferInfo.offsets(1);
        return data;
    }

    public int dataLength(){
        return putIndex;
    }

    public PacketInfo get(byte[] dst){
        mByteBuffer.get(dst);
        offsets(dst.length);
        return this;
    }

    public byte get(int index){
        return mByteBuffer.get(index);
    }

    public short getShort(){
        short data = mByteBuffer.getShort();
        offsets(2);
        return data;
    }

    public short getShort(int index){
        return mByteBuffer.getShort(index);
    }

}
