package gan.network;

import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.*;

public class SocketGroup implements Runnable{

	private int mSize;
	private String mName;
	ConcurrentHashMap<Integer, SocketChecker> mMapSocket;
	ExecutorService mExecutorService;
	SocketListener mSocketListener;
	SocketGroupFinishListener mSocketGroupFinishListener;
	volatile boolean mRuning;
	
	public SocketGroup(int size,SocketListener listener) {
		mSize = size;
		mMapSocket = new ConcurrentHashMap<>();
		mSocketListener = listener;
		mExecutorService = new ThreadPoolExecutor(0, size,
                60L, TimeUnit.SECONDS,
                new SynchronousQueue<Runnable>());
	}

	public void setName(String name) {
		this.mName = name;
	}

	public String getName() {
		return mName;
	}

	Thread mThread;
	public synchronized void start(){
		if(mThread!=null
				||mRuning){
			return;
		}
		mThread = new Thread(this);
		mThread.setName(mName);
		mThread.setPriority(Thread.MAX_PRIORITY);
		mThread.start();
		mRuning = true;
	}

	public void setSocketGroupFinishListener(SocketGroupFinishListener socketGroupFinishListener) {
		this.mSocketGroupFinishListener = socketGroupFinishListener;
	}

	public synchronized void addSocket(Socket socket) {
		mSocketListener.onSocketCreate(socket);
		mMapSocket.put(socket.getPort(), new SocketChecker(socket));
		if(mMapSocket.size()<=1){
			start();
		}
	}

	public boolean hasSocket(Socket socket){
		return mMapSocket.containsKey(socket.getPort());
	}
	
	public void close() {
		mRuning = false;
		clearSocket();
		if(mThread!=null){
			mThread.interrupt();
			mThread=null;
		}
	}

	public synchronized void clearSocket(){
		for(SocketChecker socketChecker:mMapSocket.values()){
			closeSocket(socketChecker);
		}
		mMapSocket.clear();
	}
	
	public void closeSocket(SocketChecker socket){
		try {
			socket.close();
		} catch (Throwable e) {
		}finally {
			SocketChecker socketChecker = mMapSocket.remove(socket.getPort());
			if(socketChecker!=null){
				mSocketListener.onSocketClosed(socketChecker.mSocket);
			}
			synchronized (this){
				if(mMapSocket.size()<=0){
					close();
				}
			}
		}
	}

	public synchronized boolean isFull(){
		return mMapSocket.size()>=mSize;
	}

	@Override
	public void run() {
		try{
			mRuning = true;
			ArrayList<SocketChecker> socketClosed = new ArrayList<>();
			while (mRuning) {
				socketClosed.clear();
				for(SocketChecker socket:mMapSocket.values()) {
					if(socket.isClosed()) {
						socketClosed.add(socket);
					}else if(socketAvailable(socket)) {
						mExecutorService.execute(new SocketReceiver(socket,mSocketListener));
					}
				}
				for(SocketChecker socket:socketClosed) {
					closeSocket(socket);
				}
				try{
					Thread.sleep(1);
				}catch (Throwable e){
				}
			}
		}catch (Throwable e){
			e.printStackTrace();
		}finally {
			try{
				clearSocket();
			}finally {
				if(mSocketGroupFinishListener!=null){
					mSocketGroupFinishListener.onFinish(this);
				}
			}
		}
	}
	
	private final boolean socketAvailable(SocketChecker socket) {
		return socket.checkData()>0;
	}

	public static interface SocketGroupFinishListener{
		public void onFinish(SocketGroup group);
	}
}
