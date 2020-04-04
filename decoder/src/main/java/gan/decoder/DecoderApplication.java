package gan.decoder;

import android.os.Looper;
import gan.decoder.config.DecoderConfig;
import gan.log.FileLogger;
import gan.core.utils.TextUtils;
import gan.media.MediaApplication;
import gan.media.rtsp.RtspMediaServerManager;
import gan.core.system.server.SystemServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.ApplicationContext;

import java.net.URI;
import java.net.URLEncoder;

@SpringBootApplication(
        scanBasePackages = {"gan.web", "gan.decoder.config", "gan.decoder.web"},
        exclude = DataSourceAutoConfiguration.class)
public class DecoderApplication extends MediaApplication {

    @Autowired
    DecoderConfig decoderConfig;

    public static void main(String[] args) {
        Thread.currentThread().setName("main");
        Looper.prepareMainLooper();
        ApplicationContext context = SpringApplication.run(DecoderApplication.class, args);
        SystemServer.getInstance().create(context);
        Looper.loop();
    }

    @Override
    protected void onCreate(ApplicationContext context) {
        super.onCreate(context);
        FileLogger.getInfoLogger().setLogcat(isDebug());
        RtspMediaServerManager.getInstance().initServer();
    }

    public static DecoderConfig getDecoderConfig() {
        return ((DecoderApplication)getInstance()).decoderConfig;
    }

    public static FileLogger getLogger(String url){
        try{
            URI uri = URI.create(url);
            String host = uri.getHost();
            if(!TextUtils.isEmpty(host)){
                return FileLogger.getInstance("/rtsp/"+URLEncoder.encode(host))
                        .setLogcat(getDecoderConfig().debug);
            }
        }catch (Exception e){
        }
        return FileLogger.getInfoLogger();
    }

    @Override
    public boolean isDebug() {
        return decoderConfig.debug;
    }

}
