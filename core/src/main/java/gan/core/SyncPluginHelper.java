package gan.core;

import java.util.Collection;

public class SyncPluginHelper<BaseInterface> extends PluginHelper<BaseInterface> {

	@Override
	public void addManager(BaseInterface manager) {
		synchronized (this) {
			super.addManager(manager);
		}
	}
	
	@Override
	public void addManagerAtHead(BaseInterface manager) {
		synchronized (this) {
			super.addManagerAtHead(manager);
		}
	}
	
	@Override
	public <T extends BaseInterface> Collection<T> getManagers(Class<T> cls) {
		synchronized (this) {
			return super.getManagers(cls);
		}
	}
	
	@Override
	public void removeManager(Object manager) {
		synchronized (this) {
			super.removeManager(manager);
		}
	}
	
	@Override
	public <T extends BaseInterface> void interceptPlugin(Class<T> plugin, Class<?> wrapClass, BaseInterface manager) {
		synchronized (this) {
			super.interceptPlugin(plugin, wrapClass, manager);
		}
	}
}
