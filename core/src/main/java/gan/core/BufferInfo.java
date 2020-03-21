package gan.core;

public class BufferInfo implements Cloneable{

    public int   offset;
    public int   length;

    @Override
    protected BufferInfo clone() throws CloneNotSupportedException {
        BufferInfo info = new BufferInfo();
        info.offset = offset;
        info.length = length;
        return info;
    }

    public BufferInfo copy(BufferInfo bufferInfo){
        offset = bufferInfo.offset;
        length = bufferInfo.length;
        return this;
    }

    public BufferInfo setOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public BufferInfo offsets(int offset){
        this.offset+=offset;
        return this;
    }

    public BufferInfo setLength(int length) {
        this.length = length;
        return this;
    }

    public BufferInfo lengths(int length){
        this.length+=length;
        return this;
    }

    public void reset(){
        offset = length = 0;
    }

    @Override
    public String toString() {
        return "offset:"+offset+",length:"+length;
    }
}
