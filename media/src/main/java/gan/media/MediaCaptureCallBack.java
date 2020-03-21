package gan.media;

public interface MediaCaptureCallBack {
    public void onCapture(byte[] data,int offset,int length);
}
