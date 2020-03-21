package gan.core.event;

public final class EventDelegateCanceller implements EventCanceller {
	
	private Event mEvent;
	
	public EventDelegateCanceller(Event delegateEvent){
		delegateEvent.setCanceller(this);
	}
	
	void setExecuteEvent(Event e){
		mEvent = e;
	}

	@Override
	public void cancelEvent(Event e) {
		AndroidEventManager.getInstance().cancelEvent(mEvent);
	}
}
