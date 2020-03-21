package gan.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DatagramSocketService {

	String mId;
	String mHost;
	int mPort;
	boolean mIsConnecting;
	byte[] mBuffer;
	DatagramSocket mSocket;
	ReceiveThread mReceiveThread;

	public DatagramSocketService(String host, int port){
		this(host,port,2048);
	}

	public DatagramSocketService(String host, int port, int bufferSize) {
		mHost = host;
		mPort = port;
		mBuffer = new byte[bufferSize];
	}
	
	public String getId() {
		return mId;
	}
	
	public void start() throws Exception {
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
	
	public void stop() {
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
	
	protected void onReceive(DatagramPacket packet) {
	}
	
	public void send(DatagramPacket packet) throws IOException {
		mSocket.send(packet);
	}
	
	private class ReceiveThread extends Thread{
		@Override
		public void run() {
			super.run();
			try {
				if(mSocket == null) {
					mSocket = new DatagramSocket(mPort, InetAddress.getByName(mHost));
				}
				DatagramPacket packet = new DatagramPacket(mBuffer, 1024);
				mIsConnecting = true;
				while (mIsConnecting) {
					mSocket.receive(packet);
					onReceive(packet);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}finally {
				if(mSocket!=null) {
					mSocket.close();
				}
			}
		}
	}
}
