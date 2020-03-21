package gan.media;

import gan.core.event.AndroidEventManager;

public class MediaEventListener implements MediaSesstionObserver{

    AndroidEventManager mEventManager = AndroidEventManager.getInstance();

    @Override
    public void onSourceSessionCreate(MediaSourceInfo sourceSession) {
        mEventManager.pushEvent(MediaEvent.Event_Source_Create, sourceSession);
    }

    @Override
    public void onSourceSessionDestory(MediaSourceInfo sourceSession) {
        mEventManager.pushEvent(MediaEvent.Event_Source_Remove, sourceSession);
    }

    @Override
    public void onOutputSessionCreate(MediaSourceInfo info,MediaOutputInfo outputSession) {
        mEventManager.pushEvent(MediaEvent.Event_Looker_Join, info, outputSession);
    }

    @Override
    public void onOutputSessionDestory(MediaSourceInfo info,MediaOutputInfo outputSession) {
        mEventManager.pushEvent(MediaEvent.Event_Looker_Leave, info, outputSession);
    }

}
