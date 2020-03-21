package gan.core;

import java.util.*;

public class PluginHelper<BaseInterface> {
	
	private ArrayList<BaseInterface> 		registeredManagers;
	private Map<Class<? extends BaseInterface>, Collection<?>> 	registeredManagersCache;
	
	private HashMap<Class<?>, HashMap<Class<?>, BaseInterface>> mapInterceptPluginManagers;
	@SuppressWarnings("rawtypes")
	private ArrayList 							wrapTemps;
	
	public void addManager(BaseInterface manager) {
		if(manager != null){
			checkNull();
			if(!registeredManagers.contains(manager)){
				registeredManagers.add(manager);
				registeredManagersCache.clear();
			}
		}
	}
	
	public void addManagerAtHead(BaseInterface manager){
		if(manager != null){
			checkNull();
			if(!registeredManagers.contains(manager)){
				registeredManagers.add(0,manager);
				registeredManagersCache.clear();
			}
		}
	}
	
	public void removeManager(Object manager){
		if(manager != null){
			if(registeredManagers != null){
				if(registeredManagers.remove(manager)){
					registeredManagersCache.clear();
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends BaseInterface> Collection<T> getManagers(
			Class<T> cls) {
		checkNull();
		Collection<T> collection = (Collection<T>) registeredManagersCache.get(cls);
		if (collection == null) {
			collection = new ArrayList<T>();
			for (Object manager : registeredManagers)
				if (cls.isInstance(manager))
					collection.add((T) manager);
			collection = Collections.unmodifiableCollection(collection);
			registeredManagersCache.put(cls, collection);
		}
		if(mapInterceptPluginManagers != null){
			HashMap<Class<?>, BaseInterface> map = mapInterceptPluginManagers.get(cls);
			if(map != null){
				if(wrapTemps == null){
					wrapTemps = new ArrayList<BaseInterface>();
				}
				wrapTemps.clear();
				for(T item : collection){
					BaseInterface manager = map.get(item.getClass());
					if(manager == null){
						wrapTemps.add(item);
					}else{
						wrapTemps.add(manager);
					}
				}
				return wrapTemps;
			}
		}
		return collection;
	}
	
	protected void checkNull(){
		if(registeredManagers == null){
			registeredManagers = new ArrayList<BaseInterface>();
		}
		if(registeredManagersCache == null){
			registeredManagersCache = new HashMap<Class<? extends BaseInterface>, Collection<?>>();
		}
	}
	
	public void clear(){
		if(registeredManagers != null){
			registeredManagers.clear();
		}
		if(registeredManagersCache != null){
			registeredManagersCache.clear();
		}
	}
	
	public <T extends BaseInterface> void interceptPlugin(
			Class<T> plugin,Class<?> wrapClass,BaseInterface manager){
		if(mapInterceptPluginManagers == null){
			mapInterceptPluginManagers = new HashMap<Class<?>, HashMap<Class<?>,BaseInterface>>();
		}
		HashMap<Class<?>, BaseInterface> map = mapInterceptPluginManagers.get(plugin);
		if(map == null){
			map = new HashMap<Class<?>, BaseInterface>();
			mapInterceptPluginManagers.put(plugin, map);
		}
		map.put(wrapClass, manager);
	}
}
