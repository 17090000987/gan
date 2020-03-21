package gan.network;


import gan.core.system.SystemUtils;

import java.io.Closeable;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketChecker implements Closeable {

	private final static String TAG = SocketChecker.class.getName();

	Socket mSocket;
	AtomicInteger mDataLength = new AtomicInteger(0);
	
	public SocketChecker(Socket socket) {
		mSocket = socket;
	}
	
	public Socket getSocket() {
		return mSocket;
	}
	
	public void setDataLength(int newValue) {
		mDataLength.set(newValue);
	}
	
	public int checkData() {
		if(mDataLength.get()>0) {
			return 0;
		}
		try {
			int available = mSocket.getInputStream().available();
			mDataLength.set(available);
			return mDataLength.intValue();
		} catch (IOException e) {
			return 0;
		}
	}

	@Override
	public void close() throws IOException {
		try{
			mSocket.shutdownInput();
		}catch (Exception e){
			//ignore
		}
		try{
			mSocket.shutdownOutput();
		}catch (Exception e){
			//ignore
		}
		SystemUtils.close(mSocket.getOutputStream());
		SystemUtils.close(mSocket.getInputStream());
		mSocket.close();
	}
	
	public boolean isClosed() {
		return mSocket.isClosed()||!mSocket.isConnected();
	}
	
	public int getPort() {
		return mSocket.getPort();
	}
}
