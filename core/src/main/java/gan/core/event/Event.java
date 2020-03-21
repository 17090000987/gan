package gan.core.event;

import gan.core.utils.TextUtils;
import gan.core.event.EventManager.OnEventListener;
import gan.core.system.SystemUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class Event {
	
	protected final String 			mEventCode;

	/**
	 * 兼任老代码
	 */
	private	int						mIntCode;

	private final int				mHashCode;
	
	private volatile boolean		mIsSuccess = false;
	
	private Exception				mFailException;

	private Object 					mParams[];
	private HashMap<String, Object> mMapTags;
	
	private List<Object>			mReturnParams;
	private HashMap<String, Object> mMapReturnParams;
	
	private volatile boolean		mIsCancel;
	
	protected boolean				mReRun;
	
	private Object							mEventListenersSync = new Object();
	private List<OnEventListener>			mEventListeners;
	private EventCanceller					mCanceller;
	
	private Object							mProgressListenersSync = new Object();
	private List<OnEventProgressListener>	mProgressListeners;
	private volatile int					mProgress;
	
	private Object							mEventCancelListenersSync = new Object();
	private List<EventCancelListener> 		mEventCancelListeners;
	
	public Event(int eventCode,Object params[]){
		this(String.valueOf(eventCode),params);
		mIntCode = eventCode;
	}
	
	public Event(String eventCode,Object params[]){
		mEventCode = eventCode;
		mParams = params;
		
		int hashCode = getStringCode().hashCode();
		if(mParams != null){
			for(Object obj : mParams){
				if(obj != null){
					hashCode = hashCode * 29 + obj.hashCode();
				}
			}
		}
		mHashCode = hashCode;
	}
	
	public boolean 	isEventCode(String code){
		return TextUtils.equals(code, getStringCode());
	}

	public int 		getEventCode(){
		if(mIntCode > 0){
			return mIntCode;
		}
		return mIntCode = SystemUtils.safeParseInt(mEventCode);
	}
	
	public String	getStringCode(){
		return mEventCode;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this){
			return true;
		}
		if(o != null && o instanceof Event){
			final Event other = (Event)o;
			return mHashCode == other.mHashCode;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return mHashCode;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer("code=");
		sb.append(mEventCode);
		sb.append("{");
		try{
			for(Object obj : mParams){
				if(obj != null){
					sb.append(obj.toString()).append(",");
				}
			}
		}catch(Exception e){
			e.printStackTrace();
		}
		sb.append("}");
		return sb.toString();
	}

	public void			setSuccess(boolean bSuccess){
		if(mIsCancel){
			if(bSuccess){
				mIsSuccess = false;
			}
		}else{
			mIsSuccess = bSuccess;
		}
	}
	
	public boolean		isSuccess(){
		return mIsSuccess;
	}
	
	public void			setCanceller(EventCanceller canceller){
		mCanceller = canceller;
	}
	
	public void			addEventCancelListener(EventCancelListener l){
		synchronized (mEventCancelListenersSync) {
			if(mEventCancelListeners == null){
				mEventCancelListeners = new ArrayList<EventCancelListener>();
			}
			mEventCancelListeners.add(l);
		}
	}
	
	void				cancel(){
		mIsCancel = true;
		mIsSuccess = false;
		if(mCanceller != null){
			mCanceller.cancelEvent(this);
		}
		synchronized (mEventCancelListenersSync) {
			if(mEventCancelListeners != null){
				for(EventCancelListener l : mEventCancelListeners){
					l.onEventCanceled(this);
				}
			}
		}
	}
	
	public boolean		containEventListener(OnEventListener listener){
		synchronized (mEventListenersSync) {
			if(mEventListeners == null){
				return false;
			}
			return mEventListeners.contains(listener);
		}
	}

	public void			addEventListener(OnEventListener listener){
		synchronized (mEventListenersSync) {
			if(mEventListeners == null){
				mEventListeners = new ArrayList<EventManager.OnEventListener>();
			}
			if(!mEventListeners.contains(listener)){
				mEventListeners.add(listener);
			}
		}
	}
	
	public void			addEventListener(int pos,OnEventListener listener){
		synchronized (mEventListenersSync) {
			if(mEventListeners == null){
				mEventListeners = new ArrayList<EventManager.OnEventListener>();
			}
			if(!mEventListeners.contains(listener)){
				mEventListeners.add(pos,listener);
			}
		}
	}
	
	public void			addAllEventListener(Collection<OnEventListener> listeners){
		if(listeners == null){
			return;
		}
		synchronized (mEventListenersSync) {
			if(mEventListeners == null){
				mEventListeners = new ArrayList<EventManager.OnEventListener>();
			}
			for(OnEventListener l : listeners){
				if(!mEventListeners.contains(l)){
					mEventListeners.add(l);
				}
			}
		}
	}
	
	public void			removeEventListener(OnEventListener listener){
		synchronized (mEventListenersSync) {
			if(mEventListeners != null){
				mEventListeners.remove(listener);
			}
		}
	}
	
	void				clearEventListener(){
		mEventListeners = null;
	}
	
	List<OnEventListener>	getEventListeners(){
		if(mEventListeners == null){
			return null;
		}
		return new ArrayList<OnEventListener>(mEventListeners);
	}
	
	void				addProgressListener(OnEventProgressListener listener){
		synchronized (mProgressListenersSync) {
			if(mProgressListeners == null){
				mProgressListeners = new ArrayList<OnEventProgressListener>();
			}
			mProgressListeners.add(listener);
		}
	}
	
	void				addAllProgressListener(Collection<OnEventProgressListener> listeners){
		if(listeners == null){
			return;
		}
		synchronized (mProgressListenersSync) {
			if(mProgressListeners == null){
				mProgressListeners = new ArrayList<OnEventProgressListener>();
			}
			mProgressListeners.addAll(listeners);
		}
	}
	
	void				removeProgressListener(OnEventProgressListener listener){
		synchronized (mProgressListenersSync) {
			if(mProgressListeners != null){
				mProgressListeners.remove(listener);
			}
		}
	}
	
	public List<OnEventProgressListener> getProgressListeners(){
		synchronized (mProgressListenersSync) {
			if(mProgressListeners == null){
				return null;
			}
			return new ArrayList<OnEventProgressListener>(mProgressListeners);
		}
	}
	
	void				clearProgressListener(){
		synchronized (mProgressListenersSync) {
			mProgressListeners = null;
		}
	}
	
	public void			setProgress(int progress){
		if(mProgress != progress){
			mProgress = progress;
			if(mProgressListeners != null){
				AndroidEventManager.getInstance().notifyEventProgress(this);
			}
		}
	}
	
	public int			getProgress(){
		return mProgress;
	}
	
	public boolean		isCancel(){
		return mIsCancel;
	}
	
	public Object[]		getParams(){
		return mParams;
	}
	
	public Object		getParamAtIndex(int index){
		if(mParams != null && mParams.length > index){
			return mParams[index];
		}
		return null;
	}
	
	public void			setFailException(Exception e){
		mFailException = e;
	}
	
	public String		getFailMessage(){
		return mFailException == null ? null : mFailException.getMessage();
	}
	
	public Exception	getFailException(){
		return mFailException;
	}
	
	public void			addTag(String id,Object tag){
		if(mMapTags == null){
			mMapTags = new HashMap<String, Object>();
		}
		mMapTags.put(id, tag);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T		getTag(String id){
		if(mMapTags == null){
			return null;
		}
		return (T)mMapTags.get(id);
	}
	
	public void			addReturnParam(Object obj){
		if(mReturnParams == null){
			mReturnParams = new ArrayList<Object>();
		}
		mReturnParams.add(obj);
	}
	
	public Object		getReturnParamAtIndex(int index){
		if(mReturnParams == null || index >= mReturnParams.size()){
			return null;
		}
		return mReturnParams.get(index);
	}
	
	public void 		addMapReturnParam(String key,Object param){
		if(mMapReturnParams == null){
			mMapReturnParams = new HashMap<String, Object>();
		}
		mMapReturnParams.put(key, param);
	}
	
	@SuppressWarnings("unchecked")
	public <T> T		getMapReturnParam(String key){
		if(mMapReturnParams == null){
			return null;
		}
		return (T)mMapReturnParams.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public <T>T			findParam(Class<T> c){
		if(mParams != null){
			for(Object obj : mParams){
				if(c.isInstance(obj)){
					return (T)obj;
				}
			}
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T		findReturnParam(Class<T> c){
		if(mReturnParams != null){
			for(Object obj : mReturnParams){
				if(c.isInstance(obj)){
					return (T)obj;
				}
			}
		}
		return null;
	}
	
	public void 		clearReturnParam(){
		mReturnParams = null;
	}
}
