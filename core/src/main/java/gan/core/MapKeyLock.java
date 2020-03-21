package gan.core;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class MapKeyLock<T> implements KeyLock<T>{

    ConcurrentHashMap<T,ArrayList<Object>> mMapLock;

    public MapKeyLock(){
        mMapLock = new ConcurrentHashMap<>();
    }

    @Override
    public Object ifWait(T key){
        if(mMapLock.containsKey(key)){
            Object lock=null;
            synchronized (this){
                ArrayList<Object> locks  =mMapLock.get(key);
                if(locks==null){
                    locks = new ArrayList<>();
                    mMapLock.put(key,locks);
                }
                locks.add(lock = new Object());
            }
            if(lock!=null){
                synchronized (lock){
                    try {
                        lock.wait(20000);
                    } catch (InterruptedException e) {
                    }
                }
            }
            return lock;
        }else{
            synchronized (this){
                if(mMapLock.containsKey(key)){
                }else{
                    mMapLock.put(key,new ArrayList<>());
                }
            }
            return null;
        }
    }

    @Override
    public void notifyAll(Object lock) {
        if(lock!=null){
            synchronized (lock){
                lock.notifyAll();
            }
        }
    }

    @Override
    public void notify(Object lock) {
        if(lock!=null){
            synchronized (lock){
                lock.notify();
            }
        }
    }

    @Override
    public void removeNotify(T key) {
        ArrayList<Object> locks = mMapLock.remove(key);
        if(locks!=null){
            for(Object lock:locks){
                synchronized (lock){
                    lock.notifyAll();
                }
            }
            locks.clear();
        }
    }

}
