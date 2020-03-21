package gan.core;

import gan.core.system.server.SystemServer;

public class Platform {

    public static String getAbsolutePath(String name){
        String os = System.getProperty("os.name");
        if(os.toLowerCase().startsWith("win")){
            return SystemServer.getAssets("/platform/windows"+name);
        }else{
            return SystemServer.getAssets("/platform/linux"+name);
        }
    }

}
