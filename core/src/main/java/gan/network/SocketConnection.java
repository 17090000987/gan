package gan.network;

import gan.core.system.SystemUtils;

import java.io.IOException;
import java.net.Socket;

public class SocketConnection {

	String			mId;
	String 			mHost;
	int	   			mPort;
	boolean 		mIsConnecting;
	ReceiveThread	mReceiveThread;
	Socket			mSocket;
	SocketListener  mSocketListener;

	public SocketConnection(String host, int port,SocketListener listener) {
		mHost = host;
		mPort = port;
		mSocketListener = listener;
	}
	
	public String getId() {
		return mId;
	}
	
	public synchronized void connect() throws Exception {
		if(mReceiveThread!=null) {
			throw new Exception("start is runing");
		}
		mReceiveThread = new ReceiveThread();
		mReceiveThread.start();
		mId = generateId();
	}
	
	public static String generateId() {
		return "connect_"+System.currentTimeMillis();
	}

	public synchronized void reconnect() throws Exception {
		disconnect();
		connect();
	}

	public void disconnect() {
		mIsConnecting = false;
		if(mReceiveThread!=null) {
			Thread t = mReceiveThread;
			mReceiveThread = null;
			try {
				t.interrupt();
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void send(byte[] b) throws IOException {
		mSocket.getOutputStream().write(b);
	}
	
	private class ReceiveThread extends Thread{
		@Override
		public void run() {
			super.run();
			try {
				mSocket = new Socket(mHost,mPort);
				mSocketListener.onSocketCreate(mSocket);
				SocketChecker socketChecker = new SocketChecker(mSocket);
				SocketReceiver socketReceiver = new SocketReceiver(socketChecker,mSocketListener);
				mIsConnecting = true;
				while (mIsConnecting) {
					if(socketChecker.checkData()>0){
						socketReceiver.run();
					}
					Thread.sleep(1);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				SystemUtils.close(mSocket);
				mSocketListener.onSocketCreate(mSocket);
				mSocket = null;
			}
		}
	}
}
