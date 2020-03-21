package android.os;

import java.lang.ref.WeakReference;

public abstract class WeakHandler<T> extends Handler{

    WeakReference<T> mT;

    public WeakHandler(T t){
        mT = new WeakReference<>(t);
    }

    public T get(){
        return mT.get();
    }
}
