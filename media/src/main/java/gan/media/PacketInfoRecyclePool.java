package gan.media;

import gan.core.RecycleObjectPool;

import java.nio.ByteBuffer;

public class PacketInfoRecyclePool extends RecycleObjectPool<PacketInfo> {

    int capacity;

    public PacketInfoRecyclePool(int poolSize,int capacity) {
        super(poolSize);
        this.capacity = capacity;
    }

    @Override
    public PacketInfo poll() {
        PacketInfo obj =  super.poll();
        if(obj==null){
            obj = new PacketInfo(ByteBuffer.allocate(capacity),new BufferInfo());
        }
        return obj;
    }

}
