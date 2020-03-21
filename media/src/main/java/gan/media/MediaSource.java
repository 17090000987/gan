package gan.media;

import java.nio.ByteBuffer;
import java.util.Collection;

public interface MediaSource {

    public String getId();

    public String getUri();

    public MediaSourceInfo getMediaInfo();

    public ByteBuffer getIFrame();

    public void addMediaOutputStreamRunnable(MediaOutputStreamRunnable runnable);

    public void removeMediaOutputStreamRunnable(MediaOutputStreamRunnable runnable);

    public Collection<MediaOutputStreamRunnable> getAllMediaOutputStreamRunnable();

    public String getMediaCodec();

    public boolean hasAudio();

    public boolean capture(MediaCaptureCallBack captureCallBack);

}
