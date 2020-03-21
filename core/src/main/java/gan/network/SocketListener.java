package gan.network;

import java.io.IOException;
import java.net.Socket;

public interface SocketListener {

	public void onSocketCreate(Socket socket);
	public void onSocketStream(Socket socket)throws IOException;
	public void onSocketClosed(Socket socket);
	
}
