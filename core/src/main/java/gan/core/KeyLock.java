package gan.core;

public interface KeyLock<T> {

    public Object ifWait(T key) throws InterruptedException;

    public void notifyAll(Object lock);

    public void notify(Object lock);

    public void removeNotify(T key);
}
