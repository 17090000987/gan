package gan.core;

import java.util.concurrent.ArrayBlockingQueue;

public class RecycleObjectPool<T>{

    protected ArrayBlockingQueue<T> mRecycle;

    public RecycleObjectPool(int poolSize){
        mRecycle = new ArrayBlockingQueue<>(poolSize);
    }

    public void recycle(T o){
        if(o instanceof Recycleable){
            ((Recycleable)o).recycle();
        }
        mRecycle.offer(o);
    }

    public ArrayBlockingQueue<T> getRecycleQueue() {
        return mRecycle;
    }

    public T poll(){
        return mRecycle.poll();
    }

    public void release(){
        if(mRecycle!=null){
            mRecycle.clear();
        }
    }
}
