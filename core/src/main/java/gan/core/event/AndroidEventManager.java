package gan.core.event;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import gan.core.utils.TextUtils;
import gan.core.XThreadFactory;
import org.apache.commons.collections4.map.MultiValueMap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AndroidEventManager extends EventManager{
	
	public static AndroidEventManager getInstance(){
		return sInstance;
	}
	
	static{
		sInstance = new AndroidEventManager();
	}
	
	private static AndroidEventManager sInstance;
	
	private static final int WHAT_EVENT_NOTIFY 	= 1;
	private static final int WHAT_EVENT_PUSH	= 2;
	private static final int WHAT_EVENT_END		= 3;
	private static final int WHAT_EVENT_PROGRESS= 4;
	
	private ExecutorService	mExecutorService;
	
	private MultiValueMap<String,OnEventRunner>		mMapCodeToEventRunner = new MultiValueMap<String, OnEventRunner>();
	
	private MultiValueMap<String, OnEventListener> 	mMapCodeToEventListener = new MultiValueMap<String, EventManager.OnEventListener>();
	private MultiValueMap<String, OnEventListener>		mMapCodeToEventListenerAddCache = new MultiValueMap<String, EventManager.OnEventListener>();
	private MultiValueMap<String, OnEventListener>		mMapCodeToEventListenerRemoveCache = new MultiValueMap<String, EventManager.OnEventListener>();
	private boolean 									mIsMapListenerLock = false;
	private MultiValueMap<String, OnEventProgressListener> mMapEventCodeToProgressListeners = new MultiValueMap<String, OnEventProgressListener>();
	
	private ConcurrentHashMap<Event, Event> 			mMapRunningEvent = new ConcurrentHashMap<Event, Event>();
	
	private static Handler mHandler = new Handler(Looper.getMainLooper()){
		@Override
		public void handleMessage(Message msg) {
			final int nWhat = msg.what;
			if(nWhat == WHAT_EVENT_PROGRESS){
				final Event e = (Event)msg.obj;
				final List<OnEventProgressListener> listeners = e.getProgressListeners();
				if(listeners != null){
					for(OnEventProgressListener listener : listeners){
						listener.onEventProgress(e, e.getProgress());
					}
				}
			}else if(nWhat == WHAT_EVENT_END){
				sInstance.onEventRunEnd((Event)msg.obj);
			}else if(nWhat == WHAT_EVENT_PUSH){
				final Event event = (Event)msg.obj;
				sInstance.mExecutorService.execute(new Runnable() {
					@Override
					public void run() {
						sInstance.processEvent(event);
						if(!event.mReRun){
							mHandler.sendMessage(mHandler.obtainMessage(WHAT_EVENT_END, event));
						}
					}
				});
			}else if(nWhat == WHAT_EVENT_NOTIFY){
				sInstance.doNotify((Event)msg.obj);
			}
		}
	};

	private AndroidEventManager(){
		mExecutorService = Executors.newCachedThreadPool(new XThreadFactory("EventManager"));
	}

	public Event 	pushEvent(int eventCode, Object... params) {
		return pushEventInternal(new Event(eventCode, params), 0);
	}
	
	public Event 	pushEvent(String code, Object... params) {
		return pushEventInternal(new Event(code, params), 0);
	}
	
	public Event	pushEventDelayed(int eventCode,long delayMillis,Object... params){
		return pushEventInternal(new Event(eventCode, params), delayMillis);
	}
	
	public Event	pushEventCheckRunning(int eventCode,Object... params){
		return pushEventCheckRunning(String.valueOf(eventCode), params);
	}
	
	public Event	pushEventCheckRunning(String code,Object... params){
		Event e = getRuningEvent(code, params);
		if(e == null){
			return pushEvent(code, params);
		}else{
			return e;
		}
	}
	
	public Event 	pushEventEx(int eventCode,OnEventListener listener,Object... params){
		return pushEventEx(String.valueOf(eventCode), listener, params);
	}
	
	public Event 	pushEventEx(String code,OnEventListener listener,Object... params){
		Event event = new Event(code, params);
		if(listener != null){
			event.addEventListener(listener);
		}
		return pushEventInternal(event, 0);
	}
	
	public Event 	pushEventRepeatable(String code,OnEventListener listener,Object... params){
		Event event = new Event(code, params);
		if(listener != null){
			event.addEventListener(listener);
		}
		return pushEventInternalRepeatable(event, 0);
	}
	
	private Event	pushEventInternal(Event event,long delayMillis){
		Event runEvent = mMapRunningEvent.putIfAbsent(event, event);
		if(runEvent == null){
			mHandler.sendMessageDelayed(
					mHandler.obtainMessage(WHAT_EVENT_PUSH, event),
					delayMillis);
			return event;
		}else{
			runEvent.addAllEventListener(event.getEventListeners());
			runEvent.addAllProgressListener(event.getProgressListeners());
			return runEvent;
		}
	}
	
	private Event	pushEventInternalRepeatable(Event event,long delayMillis){
		Event runEvent = mMapRunningEvent.putIfAbsent(event, event);
		if(runEvent != null){
			runEvent.mReRun = true;
			event.addAllEventListener(runEvent.getEventListeners());
			event.addAllProgressListener(runEvent.getProgressListeners());
			runEvent.clearEventListener();
			runEvent.clearProgressListener();
		}
		mHandler.sendMessageDelayed(
				mHandler.obtainMessage(WHAT_EVENT_PUSH, event),
				delayMillis);
		return event;
	}
	
	public Event 	runEvent(int eventCode, Object... params) {
		return runEventEx(eventCode, null, null,params);
	}
	
	public Event 	runEvent(String code, Object... params) {
		return runEventEx(code, null, null,params);
	}
	
	public Event 	runEventEx(int eventCode,EventDelegateCanceller canceller,EventProgressDelegate progress,Object... params){
		return runEventEx(String.valueOf(eventCode), canceller, progress, params);
	}
	
	public Event 	runEventEx(String code,EventDelegateCanceller canceller,EventProgressDelegate progress,Object... params){
		final Event event = new Event(code, params);
		if(canceller != null){
			canceller.setExecuteEvent(event);
		}
		if(progress != null){
			progress.setExecuteEvent(event);
		}
		final Event runEvent = mMapRunningEvent.putIfAbsent(event, event);
		if(runEvent == null){
			processEvent(event);
			mMapRunningEvent.remove(event);
			mHandler.sendMessage(mHandler.obtainMessage(WHAT_EVENT_END, event));
			return event;
		}else{
			processEvent(event);
			mHandler.sendMessage(mHandler.obtainMessage(WHAT_EVENT_END, event));
			return event;
		}
	}
	
	public Event 	runEventCheckRunning(String code,EventDelegateCanceller canceller,EventProgressDelegate progress,Object... params){
		final Event event = new Event(code, params);
		if(canceller != null){
			canceller.setExecuteEvent(event);
		}
		if(progress != null){
			progress.setExecuteEvent(event);
		}
		final Event runEvent = mMapRunningEvent.putIfAbsent(event, event);
		if(runEvent == null){
			processEvent(event);
			mMapRunningEvent.remove(event);
			mHandler.sendMessage(mHandler.obtainMessage(WHAT_EVENT_END, event));
			return event;
		}else{
			final Object sync = new Object();
			runEvent.addEventListener(new OnEventListener() {
				@Override
				public void onEventRunEnd(Event event) {
					synchronized (sync) {
						sync.notify();
					}
				}
			});
			synchronized (sync) {
				try{
					sync.wait(60000);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
			return runEvent;
		}
	}
	
	public void 	pushEvent(Event event){
		Event newEvent = pushEvent(event.getStringCode(),event.getParams());
		newEvent.addAllEventListener(event.getEventListeners());
		newEvent.addAllProgressListener(event.getProgressListeners());
	}
	
	public void		cancelEvent(Event e){
		if(e != null){
			e.cancel();
		}
	}
	
	public void		notifyEvent(int eventCode,Object... params){
		Event e = new Event(eventCode, params);
		e.setSuccess(true);
		mHandler.sendMessage(mHandler.obtainMessage(WHAT_EVENT_END, e));
	}
	
	public void		cancelAllEvent(){
		mMapRunningEvent.clear();
		mHandler.removeMessages(WHAT_EVENT_PUSH);
		
		mExecutorService.shutdownNow();
		mExecutorService = Executors.newCachedThreadPool();
	}
	
	public void 	registerEventRunner(int eventCode, OnEventRunner runner) {
		registerEventRunner(String.valueOf(eventCode), runner);
	}
	
	public void 	registerEventRunner(String code, OnEventRunner runner) {
		mMapCodeToEventRunner.remove(code);
		mMapCodeToEventRunner.put(code, runner);
	}
	
	public void		removeEventRunner(int eventCode, OnEventRunner runner){
		removeEventRunner(String.valueOf(eventCode), runner);
	}
	
	public void 	removeEventRunner(String code, OnEventRunner runner){
		mMapCodeToEventRunner.removeMapping(code, runner);
	}
	
	public void 	clearAllRunners(){
		mMapCodeToEventRunner.clear();
	}
	
	public boolean	hasEventRunning(int eventCode){
		return hasEventRunning(String.valueOf(eventCode));
	}
	
	public boolean	hasEventRunning(String code){
		for(Event e : new ArrayList<Event>(mMapRunningEvent.keySet())){
			if(TextUtils.equals(e.getStringCode(), code)){
				return true;
			}
		}
		return false;
	}
	
	public boolean	hasEventRunning(int eventCode,Object...params){
		return hasEventRunning(String.valueOf(eventCode), params);
	}
	
	public boolean	hasEventRunning(String code,Object...params){
		final int paramLength = params == null ? 0 : params.length;
		for(Event e : new ArrayList<Event>(mMapRunningEvent.keySet())){
			if(TextUtils.equals(e.getStringCode(), code)){
				final Object otherParams[] = e.getParams();
				int length = Math.min(otherParams == null ? 0 : otherParams.length, paramLength);
				boolean equal = true;
				for(int index = 0;index < length;++index){
					final Object p1 = otherParams[index];
					final Object p2	= params[index];
					if(p1 == null && p2 == null){
						continue;
					}
					if(p1 == null || !p1.equals(p2)){
						equal = false;
						break;
					}
				}
				if(equal){
					return true;
				}
			}
		}
		return false;
	}
	
	public Collection<Event> getRuningEvents(int eventCode){
		return getRuningEvents(String.valueOf(eventCode));
	}
	
	public Collection<Event> getRuningEvents(String code){
		List<Event> events = new ArrayList<Event>();
		for(Event e : new ArrayList<Event>(mMapRunningEvent.keySet())){
			if(TextUtils.equals(e.getStringCode(), code)){
				events.add(e);
			}
		}
		return events;
	}
	
	public void cancelRunningEvent(int eventCode){
		cancelRunningEvent(String.valueOf(eventCode));
	}
	
	public void cancelRunningEvent(String code){
		for(Event e : new ArrayList<Event>(mMapRunningEvent.keySet())){
			if(TextUtils.equals(e.getStringCode(), code)){
				e.cancel();
			}
		}
	}
	
	public boolean	isEventRunning(Event e){
		final Event runEvent = mMapRunningEvent.get(e);
		return runEvent != null && !runEvent.isCancel();
	}
	
	public boolean	isEventRunning(int eventCode,Object... params){
		return isEventRunning(new Event(eventCode, params));
	}
	
	public boolean	isEventRunning(String code,Object... params){
		return isEventRunning(new Event(code, params));
	}
	
	public Event	getRuningEvent(int eventCode,Object... params){
		return mMapRunningEvent.get(new Event(eventCode, params));
	}
	
	public Event	getRuningEvent(String code,Object... params){
		return mMapRunningEvent.get(new Event(code, params));
	}
	
	public	void 	addEventListener(int eventCode,OnEventListener listener){
		addEventListener(String.valueOf(eventCode), listener);
	}
	
	public	void 	addEventListener(String code,OnEventListener listener){
		if(mIsMapListenerLock){
			addToListenerMap(mMapCodeToEventListenerAddCache, code, listener);
		}else{
			addToListenerMap(mMapCodeToEventListener, code, listener);
		}
	}
	
	public void		addEventListenerOnce(int eventCode,final OnEventListener listener){
		addEventListener(eventCode, new OnEventListener() {
			@Override
			public void onEventRunEnd(Event event) {
				removeEventListener(event.getStringCode(), this);
				if(listener != null){
					listener.onEventRunEnd(event);
				}
			}
		});
	}
	
	public  void	removeEventListener(int eventCode,OnEventListener listener){
		removeEventListener(String.valueOf(eventCode), listener);
	}
	
	public void		removeEventListener(String code,OnEventListener listener){
		if(mIsMapListenerLock){
			addToListenerMap(mMapCodeToEventListenerRemoveCache, code, listener);
		}else{
			mMapCodeToEventListener.removeMapping(code, listener);
		}
	}
	
	public void		removeAllEventListeners(MultiValueMap<String, OnEventListener> map){
		if(mIsMapListenerLock){
			mMapCodeToEventListenerRemoveCache.putAll(map);
		}else{
			internalRemoveAllEventListeners(map);
		}
	}
	
	private void	internalRemoveAllEventListeners(MultiValueMap<String, OnEventListener> map){
		for(String code : map.keySet()){
			Collection<OnEventListener> listeners = map.getCollection(code);
			if(listeners != null){
				for(OnEventListener listener : listeners){
					mMapCodeToEventListener.removeMapping(code, listener);
				}
			}
		}
	}
	
	public void		clearEventListenerEx(Event e){
		e.clearEventListener();
	}
	
	private void	addToListenerMap(MultiValueMap<String, OnEventListener> map,
			String code,OnEventListener listener){
		map.put(code, listener);
	}
	
	public void		addEventProgressListener(Event e,OnEventProgressListener listener){
		e.addProgressListener(listener);
	}
	
	public void		removeEventProgressListener(Event e,OnEventProgressListener listener){
		e.removeProgressListener(listener);
	}
	
	public void		addEventProgressListener(int code,OnEventProgressListener listener){
		addEventProgressListener(String.valueOf(code), listener);
	}
	
	public void		addEventProgressListener(String code,OnEventProgressListener listener){
		mMapEventCodeToProgressListeners.put(code, listener);
	}
	
	public void		removeEventProgressListener(int code,OnEventProgressListener listener){
		removeEventProgressListener(String.valueOf(code), listener);
	}
	
	public void		removeEventProgressListener(String code,OnEventProgressListener listener){
		mMapEventCodeToProgressListeners.removeMapping(code, listener);
	}
	
	void			notifyEventProgress(Event e){
		mHandler.sendMessage(mHandler.obtainMessage(WHAT_EVENT_PROGRESS, e));
	}
	
	protected boolean processEvent(Event event){		
		event.addProgressListener(mProgressListener);
		
		try {
			Collection<OnEventRunner> runners = mMapCodeToEventRunner.getCollection(event.getStringCode());
			if(runners != null){
				for(OnEventRunner runner : runners){
					runner.onEventRun(event);
				}
			}
			if(event.isSuccess()){
				event.setProgress(100);
			}
		} catch (Exception e) {
			e.printStackTrace();
			event.setFailException(e);
		} 
		
		return true;
	}
	
	protected void	onEventRunEnd(Event event){
		mMapRunningEvent.remove(event);
		doNotify(event);
	}
	
	private void	doNotify(Event event){
		final List<OnEventListener> eventListeners = event.getEventListeners();
		if(eventListeners != null && eventListeners.size() > 0){
			try{
				for(OnEventListener listener : eventListeners){
					listener.onEventRunEnd(event);
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		mIsMapListenerLock = true;
		Collection<OnEventListener> list = mMapCodeToEventListener.getCollection(event.getStringCode());
		if(list != null){
			for(OnEventListener listener : list){
				try{
					listener.onEventRunEnd(event);
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		mIsMapListenerLock = false;
		
		if(mMapCodeToEventListenerAddCache.size() > 0){
			mMapCodeToEventListener.putAll(mMapCodeToEventListenerAddCache);
			mMapCodeToEventListenerAddCache.clear();
		}
		if(mMapCodeToEventListenerRemoveCache.size() > 0){
			internalRemoveAllEventListeners(mMapCodeToEventListenerRemoveCache);
			mMapCodeToEventListenerRemoveCache.clear();
		}
	}

	private OnEventProgressListener mProgressListener = new OnEventProgressListener() {
		@Override
		public void onEventProgress(Event e, int progress) {
			Collection<OnEventProgressListener> listeners = mMapEventCodeToProgressListeners.getCollection(e.getStringCode());
			if(listeners != null){
				for(OnEventProgressListener listener : listeners){
					listener.onEventProgress(e, progress);
				}
			}
		}
	};
}
