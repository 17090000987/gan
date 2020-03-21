package gan.core.system.server;

import java.io.IOException;
import java.io.InputStream;

public interface ServerMessageHandler extends ServerListener {
    public String getType();
    public void onReceiveMessage(InputStream is)throws IOException;
}
