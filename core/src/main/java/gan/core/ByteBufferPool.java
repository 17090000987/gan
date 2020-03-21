package gan.core;

import java.nio.ByteBuffer;

public class ByteBufferPool extends RecycleObjectPool<ByteBuffer> {

    int capacity;

    public ByteBufferPool(int poolSize,int capacity){
        super(poolSize);
        this.capacity = capacity;
    }

    @Override
    public void recycle(ByteBuffer buf){
        super.recycle(buf);
        buf.clear();
    }

    @Override
    public ByteBuffer poll(){
        ByteBuffer buffer =  mRecycle.poll();
        if(buffer==null){
            buffer = ByteBuffer.allocate(capacity);
        }
        return buffer;
    }
}
