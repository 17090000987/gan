package gan.network;

import gan.log.DebugLog;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;

public class SocketGroupManager extends Thread {

	private final static String TAG = SocketGroupManager.class.getName();
	private ServerSocket mServerSocket;
	private int mPort;
	private Vector<SocketGroup> mSocketGroups =  new Vector<>();
	private ArrayBlockingQueue<SocketGroup> mCacheSocketGroup = new ArrayBlockingQueue<>(10);
	private SocketListener mSocketListener;

	public SocketGroupManager(int port, SocketListener listener) throws IOException{
		setPriority(Thread.MAX_PRIORITY);
		mPort = port;
		mServerSocket = new ServerSocket(port);
		mSocketListener = listener;
	}

	public int getPort() {
		return mPort;
	}
	
	public void run() {
		try {
			while (!isInterrupted()) {
				Socket socket = mServerSocket.accept();
				if(socket!=null&&!socket.isClosed()&&!hasSocket(socket)){
					SocketGroup group = getSocketGroup();
					group.addSocket(socket);
				}
				Thread.sleep(1);
			}
		} catch (Throwable e) {
			e.printStackTrace();
		}finally {
			closeSocketServer();
			for(SocketGroup group:mSocketGroups) {
				group.close();
			}
			mCacheSocketGroup.clear();
		}
	}

	public synchronized SocketGroup getSocketGroup(){
		for(SocketGroup group:mSocketGroups) {
			if(!group.isFull()){
				return group;
			}
		}
		DebugLog.debug("getSocketGroup form CacheSocketGroup size:"+mCacheSocketGroup.size());
		SocketGroup group= mCacheSocketGroup.poll();
		if(group==null) {
			DebugLog.info("new SocketGroup"+mSocketGroups.size());
			group = new SocketGroup(500,mSocketListener);
			group.setName(String.format("socketgroup_%s", mSocketGroups.size()));
			group.setSocketGroupFinishListener(new SocketGroup.SocketGroupFinishListener() {
				@Override
				public void onFinish(SocketGroup group) {
					removeSocketGroup(group);
				}
			});
		}
		mSocketGroups.add(group);
		return group;
	}

	public synchronized boolean hasSocket(Socket socket){
		for(SocketGroup group:mSocketGroups) {
			if(group.hasSocket(socket)){
				return true;
			}
		}
		return false;
	}

	public void close() {
		closeSocketServer();
		interrupt();
	}

	private void closeSocketServer() {
		try {
			if (null != mServerSocket && !mServerSocket.isClosed()) {
				mServerSocket.close();
				mServerSocket = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public synchronized void removeSocketGroup(SocketGroup socketGroup){
		mSocketGroups.remove(socketGroup);
		mCacheSocketGroup.offer(socketGroup);
		DebugLog.debug(String.format("SocketGroups size:%s",mSocketGroups.size()));
	}
}
