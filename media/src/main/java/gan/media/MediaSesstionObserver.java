package gan.media;

public interface MediaSesstionObserver extends MediaListener{
    public void onSourceSessionCreate(MediaSourceInfo sourceSession);
    public void onSourceSessionDestory(MediaSourceInfo sourceSession);
    public void onOutputSessionCreate(MediaSourceInfo sourceInfo,MediaOutputInfo outputSession);
    public void onOutputSessionDestory(MediaSourceInfo sourceInfo,MediaOutputInfo outputSession);
}
