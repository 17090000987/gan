package gan.server;

import android.os.Looper;
import gan.core.Platform;
import gan.core.file.FileHelper;
import gan.core.file.SharePerference;
import gan.core.system.SystemUtils;
import gan.core.system.server.SystemServer;
import gan.core.utils.TextUtils;
import gan.log.DebugLog;
import gan.log.FileLogger;
import gan.media.MediaApplication;
import gan.media.rtsp.RtspMediaServerManager;
import gan.web.config.MediaConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import gan.server.config.FFmepg;
import gan.server.config.Gan;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

@SpringBootApplication(
        scanBasePackages = {"gan.web", "gan.server.config", "gan.server.web"},
        exclude = DataSourceAutoConfiguration.class)
public class GanServer extends MediaApplication {

    public static void main(String[] args) {
        Logger.getLogger(GanServer.class.getName()).info("main start");
        Thread.currentThread().setName("main");
        Looper.prepareMainLooper();
        try{
            ApplicationContext context = new SpringApplicationBuilder(GanServer.class)
                    .run(args);
            SystemServer.getInstance().create(context);
        }finally {
            Looper.loop();
        }
    }

    @Autowired
    private Gan gan;
    @Autowired
    FFmepg  ffmpeg;

    public static GanServer getInstance() {
        return (GanServer) sInstance;
    }

    @Override
    protected void onCreate(ApplicationContext context) {
        FileLogger.getInfoLogger().setLogcat(gan.debug);
        FileLogger.getDebugLogger().setLogcat(gan.debug);
        if(gan.debug){
            DebugLog.setLevel(gan.logLevel);
        }else{
            DebugLog.setLevel(DebugLog.INFO);
        }
        super.onCreate(context);
        String rtmpPid = getSharePerference().getString("rtmp_pid");
        if(!TextUtils.isEmpty(rtmpPid)){
            try{
                String name = getSharePerference().getString("rtmp_name");
                SystemUtils.killProcessByName(name);
            }catch (Exception e){
            }finally {
                getSharePerference().remove("rtmp_pid").remove("rtmp_name").commit();
            }
        }

        MediaConfig config = getMediaConfig();
        if(config.rtspEnable){
            DebugLog.info( "rtsp_port:"+config.rtspPort);
            addManager(RtspMediaServerManager.getInstance());
            RtspMediaServerManager.getInstance().initServer();
        }

        if(gan.gb28181Enable){
        }

        if(gan.jt1078Enable){
        }

        if(gan.rtmpEnable){
            getMainHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    startRtmp();
                }
            },1000);
        }

        if(gan.deviceEnable){
        }
    }

    private void startRtmp(){
        try {
            DebugLog.info( "rtmp_port:"+gan.rtmpPort);
            Runtime runtime = Runtime.getRuntime();
            String os = System.getProperty("os.name");
            String cmd = null;
            String dir = null;
            if(os.toLowerCase().startsWith("win")){
                dir = Platform.getAbsolutePath("/nginx-rtmp");
                cmd = Platform.getAbsolutePath("/nginx-rtmp/nginx");
                FileHelper.checkOrCreateFolder(Platform.getAbsolutePath("/nginx-rtmp/logs/"));
            }else{
                dir = Platform.getAbsolutePath("/nginx");
                cmd = Platform.getAbsolutePath("/nginx/sbin/nginx");
                FileHelper.checkOrCreateFolder(Platform.getAbsolutePath("/nginx/logs/"));
                FileHelper.checkOrCreateFolder(Platform.getAbsolutePath("/nginx/temp/hls/"));
            }

            DebugLog.debug( "rtmp server cmd:"+cmd);
            Process process = runtime.exec(cmd,null,new File(dir));
            getSharePerference().putString("rtmp_pid", SystemUtils.getProcessId(process)+"")
                    .putString("rtmp_name","nginx").commit();
            DebugLog.info( "rtmpServer start");
        } catch (IOException e) {
            DebugLog.warn( "rtmpServer start fail:"+e.getMessage());
            e.printStackTrace();
        }
    }

    public static Gan getGan() {
        return getInstance().gan;
    }

    public static FFmepg getFFmpeg() {
        return getInstance().ffmpeg;
    }

    public SharePerference getSharePerference() {
        return super.getSharePerference("gan");
    }

    public static String getPublicPath(String path){
        if(path.startsWith("/")){
            return getRootPath("/public"+path);
        }
        return getRootPath("/public/"+path);
    }

    public boolean isDebug(){
        return getGan().debug;
    }

}
