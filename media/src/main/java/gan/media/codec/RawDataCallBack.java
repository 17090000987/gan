package gan.media.codec;

public interface RawDataCallBack {
    public void onRawFrame(byte[] data, int length, long timestamp, int width, int height);
}