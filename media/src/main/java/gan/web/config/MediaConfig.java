package gan.web.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(
        prefix = "gan.media",
        ignoreUnknownFields = false
)
public class MediaConfig {

    public int rtspMinFrameRate;
    public boolean rtspAutoFrameRate;
    public boolean rtspEnable = true;
    public int  rtspPort = 554;
    public int  rtspMaxConnection = 2000;
    public int  rtspSessionTimeOut = 30000;
    public int  rtpSessionTimeOut = 30000;
    public int  rtspFrameBufferSize = 1024000;

    public void setRtspMinFrameRate(int rtspMinFrameRate) {
        this.rtspMinFrameRate = rtspMinFrameRate;
    }

    public int getRtspMinFrameRate() {
        return rtspMinFrameRate;
    }

    public boolean isRtspAutoFrameRate() {
        return rtspAutoFrameRate;
    }

    public void setRtspAutoFrameRate(boolean rtspAutoFrameRate) {
        this.rtspAutoFrameRate = rtspAutoFrameRate;
    }


    public boolean isRtspEnable() {
        return rtspEnable;
    }

    public void setRtspEnable(boolean rtspEnable) {
        this.rtspEnable = rtspEnable;
    }

    public int getRtspPort() {
        return rtspPort;
    }

    public void setRtspPort(int rtspPort) {
        this.rtspPort = rtspPort;
    }

    public int getRtspMaxConnection() {
        return rtspMaxConnection;
    }

    public void setRtspMaxConnection(int rtspMaxConnection) {
        this.rtspMaxConnection = rtspMaxConnection;
    }

    public int getRtspSessionTimeOut() {
        return rtspSessionTimeOut;
    }

    public void setRtspSessionTimeOut(int rtspSessionTimeOut) {
        this.rtspSessionTimeOut = rtspSessionTimeOut;
    }

    public int getRtpSessionTimeOut() {
        return rtpSessionTimeOut;
    }

    public void setRtpSessionTimeOut(int rtpSessionTimeOut) {
        this.rtpSessionTimeOut = rtpSessionTimeOut;
    }

    public int getRtspFrameBufferSize() {
        return rtspFrameBufferSize;
    }

    public void setRtspFrameBufferSize(int rtspFrameBufferSize) {
        this.rtspFrameBufferSize = rtspFrameBufferSize;
    }
}
