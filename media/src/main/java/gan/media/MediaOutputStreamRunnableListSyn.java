package gan.media;

import gan.log.DebugLog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MediaOutputStreamRunnableListSyn extends MediaOutputStreamRunnableList{

    private final static String Tag = MediaOutputStreamRunnableListSyn.class.getName();

    @Override
    public void add(MediaOutputStreamRunnable runnable) {
        synchronized (this){
            super.add(runnable);
        }
    }

    @Override
    public boolean remove(MediaOutputStreamRunnable runnable) {
        synchronized (this){
            return super.remove(runnable);
        }
    }

    @Override
    public void clear() {
        synchronized (this){
            super.clear();
        }
    }

    @Override
    public void removeAll(Collection<MediaOutputStreamRunnable> runnables) {
        synchronized (this){
            super.removeAll(runnables);
        }
    }

    public void iteratorExecute(String packetType,IteratorExecutor executor){
        synchronized (this){
            Collection<MediaOutputStreamRunnable> runnables = get(packetType);
            if(runnables.size()>0){
                List<MediaOutputStreamRunnable> errors = new ArrayList<>();
                for(MediaOutputStreamRunnable runnable:runnables){
                    try {
                        executor.execute(runnable);
                    } catch (Exception e) {
                        //ignore
                        DebugLog.debug("iteratorExecute exception");
                        e.printStackTrace();
                        runnable.close();
                        errors.add(runnable);
                    }
                }
                mOutputStreamRunnables.removeAll(errors);
            }
        }
    }

    public static interface IteratorExecutor{
        public void execute(MediaOutputStreamRunnable runnable)throws Exception;
    }

}
