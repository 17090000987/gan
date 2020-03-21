package gan.core;

public class BufferInfoBuilder {

    BufferInfo mBufferInfo = new BufferInfo();

    public BufferInfoBuilder offset(int offset){
        mBufferInfo.offset = offset;
        return this;
    }

    public BufferInfoBuilder length(int length){
        mBufferInfo.length = length;
        return this;
    }

    public BufferInfo build(){
        return mBufferInfo;
    }
}
