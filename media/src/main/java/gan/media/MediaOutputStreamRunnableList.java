package gan.media;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

public class MediaOutputStreamRunnableList {

    HashMap<String,WeakReference<ArrayList<MediaOutputStreamRunnable>>> mTypeOutputRunnables;
    ArrayList<MediaOutputStreamRunnable> mOutputStreamRunnables;

    public MediaOutputStreamRunnableList(){
        if(mTypeOutputRunnables == null){
            mTypeOutputRunnables = new HashMap<>();
        }
        if(mOutputStreamRunnables==null){
            mOutputStreamRunnables = new ArrayList<>();
        }
    }

    public void add(MediaOutputStreamRunnable runnable){
        mOutputStreamRunnables.add(runnable);
        ArrayList<MediaOutputStreamRunnable> runnables = getOutputStreamRunnables(runnable.getPacketType());
        if(runnables == null){
            mTypeOutputRunnables.put(runnable.getPacketType(), new WeakReference<>(runnables = new ArrayList<>()));
        }
        runnables.add(runnable);
    }

    public boolean remove(MediaOutputStreamRunnable runnable){
        mOutputStreamRunnables.remove(runnable);
        ArrayList<MediaOutputStreamRunnable> runnables= getOutputStreamRunnables(runnable.getPacketType());
        if(runnables!=null){
            return runnables.remove(runnable);
        }
        return false;
    }

    public void removeAll(Collection<MediaOutputStreamRunnable> runnables){
        if(runnables==null||runnables.isEmpty()){
            return;
        }
        mOutputStreamRunnables.removeAll(runnables);
        for(MediaOutputStreamRunnable runnable:runnables){
            ArrayList<MediaOutputStreamRunnable> temps= getOutputStreamRunnables(runnable.getPacketType());
            if(temps!=null){
                temps.remove(runnable);
            }
        }
    }

    public void clear(){
        mOutputStreamRunnables.clear();
        mTypeOutputRunnables.clear();
    }

    public Collection<MediaOutputStreamRunnable> getAll(){
        return Collections.unmodifiableCollection(mOutputStreamRunnables);
    }

    public int size(){
        return mOutputStreamRunnables.size();
    }

    public int size(String packetType){
        return get(packetType).size();
    }

    public Collection<MediaOutputStreamRunnable> get(String packetType){
        ArrayList<MediaOutputStreamRunnable> runnables= getOutputStreamRunnables(packetType);
        if(runnables!=null){
            return Collections.unmodifiableCollection(runnables);
        }else{
            return Collections.emptyList();
        }
    }

    private ArrayList<MediaOutputStreamRunnable> getOutputStreamRunnables(String packetType){
        WeakReference<ArrayList<MediaOutputStreamRunnable>> reference= mTypeOutputRunnables.get(packetType);
        if(reference!=null){
            ArrayList<MediaOutputStreamRunnable> runnables = reference.get();
            if(runnables==null){
                mTypeOutputRunnables.put(packetType, new WeakReference<>(runnables = new ArrayList<>()));
                for(MediaOutputStreamRunnable runnable:Collections.unmodifiableCollection(mOutputStreamRunnables)){
                    if(packetType.equals(runnable.getPacketType())){
                        runnables.add(runnable);
                    }
                }
            }
            return runnables;
        }
        return null;
    }

}
