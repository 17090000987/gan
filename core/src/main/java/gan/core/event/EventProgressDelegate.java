package gan.core.event;

public class EventProgressDelegate implements OnEventProgressListener{
	
	private Event	mDelegateEvent;
	
	public EventProgressDelegate(Event delegateEvent){
		mDelegateEvent = delegateEvent;
	}
	
	public void setExecuteEvent(Event e){
		e.addProgressListener(this);
	}

	@Override
	public void onEventProgress(Event e, int progress) {
		mDelegateEvent.setProgress(progress);
	}
}
