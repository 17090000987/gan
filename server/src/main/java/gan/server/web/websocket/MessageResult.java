package gan.server.web.websocket;

import gan.web.base.Result;

public class MessageResult extends Result {

    public String mediacodec;

    public MessageResult setMediacodec(String mediacodec) {
        this.mediacodec = mediacodec;
        return this;
    }

    public String getMediacodec() {
        return mediacodec;
    }
}
