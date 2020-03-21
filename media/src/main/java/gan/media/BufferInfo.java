package gan.media;

public class BufferInfo extends gan.core.BufferInfo{

    public byte channel;
    public long time;

    public BufferInfo setChannel(byte channel) {
        this.channel = channel;
        return this;
    }


    public BufferInfo setTime(long time) {
        this.time = time;
        return this;
    }

    public BufferInfo times(long time) {
        this.time += time;
        return this;
    }

    @Override
    public BufferInfo clone(){
        BufferInfo info = new BufferInfo();
        info.channel = channel;
        info.setTime(time)
        .setLength(length).setOffset(offset);
        return info;
    }

    public BufferInfo copy(BufferInfo info){
        super.copy(info);
        channel = info.channel;
        time = info.time;
        return this;
    }

    public void reset(){
        setOffset(0);
        setLength(0);
        time = 0;
    }

    @Override
    public String toString() {
        return super.toString()+",channel:"+channel+",time:"+time;
    }

}
