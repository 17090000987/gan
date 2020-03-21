package gan.media;

import gan.core.system.server.SystemServer;
import gan.web.config.MediaConfig;
import org.springframework.beans.factory.annotation.Autowired;

public class MediaApplication extends SystemServer {

    @Autowired
    private MediaConfig mediaConfig;

    public static MediaConfig getMediaConfig() {
        return ((MediaApplication)getInstance()).mediaConfig;
    }

}
