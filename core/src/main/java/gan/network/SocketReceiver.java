package gan.network;

import gan.log.DebugLog;

import java.io.IOException;

public class SocketReceiver implements Runnable {

	final static String Tag = SocketReceiver.class.getName();

	SocketChecker 			mSocket;
	SocketListener			mSocketListener;
	
	public SocketReceiver(SocketChecker socket, SocketListener listener) {
		mSocket = socket;
		mSocketListener = listener;
	}

	public void run() {
		try {
			mSocketListener.onSocketStream(mSocket.getSocket());
		} catch (Throwable e) {
			DebugLog.warn("socket closed");
			try {
				mSocket.close();
			} catch (IOException e1) {
				//ignore
			}
		}finally {
			mSocket.setDataLength(0);
		}
	}

}