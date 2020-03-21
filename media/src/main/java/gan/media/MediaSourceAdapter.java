package gan.media;

import gan.core.BaseListener;

public interface MediaSourceAdapter extends BaseListener {

    public boolean accept(MediaRequest request);

    public MediaSource getMediaSource(MediaRequest request);

    public MediaSourceResult getMediaSourceResult(MediaRequest request);

}
