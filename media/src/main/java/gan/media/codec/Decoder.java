package gan.media.codec;

public interface Decoder {

    public void init(Object... params);
    public void decode(int track, byte[] buffer, int offset, int len, long timesample, int iframe);
    public void release();

}
