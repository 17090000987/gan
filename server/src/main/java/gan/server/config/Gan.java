package gan.server.config;

import gan.log.DebugLog;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
@ConfigurationProperties(prefix = "gan")
@PropertySource("classpath:application.yml")
public class Gan {

    public boolean debug;
    public String license;
    public int logLevel = DebugLog.INFO;

    public boolean deviceEnable;
    public String deviceServerIp;
    public int deviceServerHttpPort = 89;
    public int deviceServerPort = 6609;
    public String deviceStreamServerIp;
    public int deviceStreamServerPort = 6609;
    public int deviceFrameTimeOut = 60000;

    public boolean rtmpEnable;
    public int rtmpPort;
    public String rtmpServerIp;

    public boolean streamConsoleEnable;
    public String streamConsoleHost;
    public int streamConsolePort;
    public long sum_bandwidth;

    public int httpRequestTimeout = 10;
    public int waitVideoDataTimeout = 10;

    public boolean gb28181Enable;
    public int gb28181Port = 555;
    public String sipServerIp;
    public String sipServerPort;
    public boolean savePs;
    public boolean saveRtp;
    public boolean nvrMultiPlaybackEnable;

    public boolean jt1078Enable;
    public int jt1078LivePort = 556;
    public int jt1078PlaybackPort = 557;
    public String hostPublicIp;
    public String jt1078ServerIp;
    public String jt1078ServerPort;
    public String ftpPath;
    public int ftpPort = 21;
    public String ftpUser;
    public String ftpPassword;

    public String getLicense() {
        return license;
    }

    public int getDeviceStreamServerPort() {
        return deviceStreamServerPort;
    }

    public void setDeviceStreamServerPort(int deviceStreamServerPort) {
        this.deviceStreamServerPort = deviceStreamServerPort;
    }

    public boolean isRtmpEnable() {
        return rtmpEnable;
    }

    public void setRtmpEnable(boolean rtmpEnable) {
        this.rtmpEnable = rtmpEnable;
    }

    public int getRtmpPort() {
        return rtmpPort;
    }

    public void setRtmpPort(int rtmpPort) {
        this.rtmpPort = rtmpPort;
    }

    public String getRtmpServerIp() {
        return rtmpServerIp;
    }

    public void setRtmpServerIp(String rtmpServerIp) {
        this.rtmpServerIp = rtmpServerIp;
    }

    public boolean isSavePs() {
        return savePs;
    }

    public boolean isSaveRtp() {
        return saveRtp;
    }

    public boolean isNvrMultiPlaybackEnable() {
        return nvrMultiPlaybackEnable;
    }

    public String getStreamConsoleHost() {
        return streamConsoleHost;
    }

    public void setStreamConsoleHost(String streamConsoleHost) {
        this.streamConsoleHost = streamConsoleHost;
    }

    public int getStreamConsolePort() {
        return streamConsolePort;
    }

    public void setStreamConsolePort(int streamConsolePort) {
        this.streamConsolePort = streamConsolePort;
    }

    public boolean isStreamConsoleEnable() {
        return streamConsoleEnable;
    }

    public void setStreamConsoleEnable(boolean streamConsoleEnable) {
        this.streamConsoleEnable = streamConsoleEnable;
    }

    public long getSum_bandwidth() {
        return sum_bandwidth;
    }

    public void setSum_bandwidth(long sum_bandwidth) {
        this.sum_bandwidth = sum_bandwidth;
    }

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public boolean isDeviceEnable() {
        return deviceEnable;
    }

    public void setDeviceEnable(boolean deviceEnable) {
        this.deviceEnable = deviceEnable;
    }

    public String getDeviceStreamServerIp() {
        return deviceStreamServerIp;
    }

    public void setDeviceStreamServerIp(String deviceStreamServerIp) {
        this.deviceStreamServerIp = deviceStreamServerIp;
    }

    public String getDeviceServerIp() {
        return deviceServerIp;
    }

    public void setDeviceServerIp(String deviceServerIp) {
        this.deviceServerIp = deviceServerIp;
    }

    public boolean isGb28181Enable() {
        return gb28181Enable;
    }

    public void setGb28181Enable(boolean enable) {
        gb28181Enable = enable;
    }

    public void setGb28181Port(int gb28181Port) {
        this.gb28181Port = gb28181Port;
    }

    public int getGb28181Port() {
        return gb28181Port;
    }

    public String getSipServerIp() {
        return sipServerIp;
    }

    public void setSipServerIp(String sipServerIp) {
        this.sipServerIp = sipServerIp;
    }

    public String getSipServerPort() {
        return sipServerPort;
    }

    public void setSipServerPort(String sipServerPort) {
        this.sipServerPort = sipServerPort;
    }

    public void setHttpRequestTimeout(int httpRequestTimeout) {
        this.httpRequestTimeout = httpRequestTimeout;
    }

    public int getHttpRequestTimeout() {
        return httpRequestTimeout;
    }

    public void setWaitVideoDataTimeout(int waitVideoDataTimeout) {
        this.waitVideoDataTimeout = waitVideoDataTimeout;
    }

    public int getWaitVideoDataTimeout() {
        return waitVideoDataTimeout;
    }

    public void setSavePs(boolean saveps) {
        this.savePs = saveps;
    }

    public boolean getSavePs() {
        return savePs;
    }

    public void setSaveRtp(boolean saveRtp) {
        this.saveRtp = saveRtp;
    }

    public boolean getSaveRtp() {
        return saveRtp;
    }

    public void setNvrMultiPlaybackEnable(boolean enable) {
        this.nvrMultiPlaybackEnable = enable;
    }

    public boolean getNvrMultiPlaybackEnable() {
        return nvrMultiPlaybackEnable;
    }

    public boolean isJt1078Enable() {
        return jt1078Enable;
    }

    public void setJt1078Enable(boolean enable) {
        jt1078Enable = enable;
    }

    public void setJt1078LivePort(int jt1078LivePort) {
        this.jt1078LivePort = jt1078LivePort;
    }

    public int getJt1078LivePort() {
        return jt1078LivePort;
    }

    public void setJt1078PlaybackPort(int jt1078PlaybackPort) {
        this.jt1078PlaybackPort = jt1078PlaybackPort;
    }

    public int getJt1078PlaybackPort() {
        return jt1078PlaybackPort;
    }

    public void setHostPublicIp(String hostPublicIp) {
        this.hostPublicIp = hostPublicIp;
    }

    public String getHostPublicIp() {
        return hostPublicIp;
    }

    public void setJt1078ServerIp(String jt1078ServerIp) {
        this.jt1078ServerIp = jt1078ServerIp;
    }

    public String getJt1078ServerIp() {
        return jt1078ServerIp;
    }

    public void setJt1078ServerPort(String jt1078ServerPort) {
        this.jt1078ServerPort = jt1078ServerPort;
    }

    public String getJt1078ServerPort() {
        return jt1078ServerPort;
    }

    public void setFtpPath(String ftpPath) {
        this.ftpPath = ftpPath;
    }

    public String getFtpPath() {
        return ftpPath;
    }

    public void setFtpPort(int ftpPort) {
        this.ftpPort = ftpPort;
    }

    public int getFtpPort() {
        return ftpPort;
    }

    public void setFtpUser(String ftpUser) {
        this.ftpUser = ftpUser;
    }

    public String getFtpUser() {
        return ftpUser;
    }

    public void setFtpPassword(String ftpPassword) {
        this.ftpPassword = ftpPassword;
    }

    public String getFtpPassword() {
        return ftpPassword;
    }

    public void setDeviceServerHttpPort(int deviceServerHttpPort) {
        this.deviceServerHttpPort = deviceServerHttpPort;
    }

    public int getDeviceServerHttpPort() {
        return deviceServerHttpPort;
    }

    public void setLicense(String license) {
        this.license = license;
    }

    public int getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    public int getDeviceServerPort() {
        return deviceServerPort;
    }

    public void setDeviceServerPort(int deviceServerPort) {
        this.deviceServerPort = deviceServerPort;
    }

    public int getDeviceFrameTimeOut() {
        return deviceFrameTimeOut;
    }

    public void setDeviceFrameTimeOut(int deviceFrameTimeOut) {
        this.deviceFrameTimeOut = deviceFrameTimeOut;
    }

}
