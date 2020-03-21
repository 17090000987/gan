package gan.media.rtsp;

import gan.media.MediaSource;

public interface RtspSource extends MediaSource {

    public Sdp getSdp();

    public String getInputSdp();

    public String getOutputSdp();

}
