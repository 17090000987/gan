package gan.core.utils;

import android.os.JSONArray;
import android.os.JSONException;
import android.os.JSONObject;
import gan.web.base.IDObject;
import gan.core.system.SystemUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.Map.Entry;

public class JsonParseUtils {
	
	public static void parse(JSONObject jo, Object item){
		parse(jo, item, item.getClass());
	}
	
	public static void parse(JSONObject jo, Object item, Class<?> c){
		internalParse(c.getDeclaredFields(), jo, item);
	}
	
	/**
	 * 慎用，如果在父类调用此函数，子类声明的成员变量不能赋默认值
	 * </br>因为声明默认值在父类构造函数之后执行
	 */
	public static Object parseAll(JSONObject jo, Object item){
		for(Class<?> clazz = item.getClass();clazz != null;clazz = clazz.getSuperclass()){
			internalParse(clazz.getDeclaredFields(), jo, item);
		}
		onJsonParseEnd(item, jo);
		return item;
	}
	
	private static void internalParse(Field[] fs, JSONObject jo, Object item){
		for(Field f : fs){
			parseField(f, jo, item);
		}
	}
	
	public static void parseField(Field f, JSONObject jo, Object item){
		String name = f.getName();
		if(jo.has(name)){
			try{
				f.setAccessible(true);
				if(!internalParsePrimaryClass(null,f, item, jo, name)){
					JsonAnnotation a = f.getAnnotation(JsonAnnotation.class);
					if(a != null){
						final Class<?> itemClazz = a.listItem();
						if(itemClazz != Void.class){
							internalParseCustomClass(itemClazz, f, item, jo, name);
						}
					}
				}
			}catch(Exception e){
				//e.printStackTrace();
			}
		}else{
			try{
				JsonAnnotation a = f.getAnnotation(JsonAnnotation.class);
				if(a != null){
					final String method = a.getJsonKeyMethod();
					String keys = "";
					if(TextUtils.isEmpty(method)){
						keys = a.jsonKey();
					}else{
						Method m = SystemUtils.getMethod(item.getClass(), method, JSONObject.class);
						if(m != null){
							keys = (String)m.invoke(item, jo);
						}
					}
					if(TextUtils.isEmpty(keys)){
						Class<?> itemClazz = a.listItem();
						if(!List.class.isAssignableFrom(f.getType())){
							if(itemClazz != null && a.buildItem()){
								f.setAccessible(true);
								f.set(item, buildObject(itemClazz, jo));
							}
						}
					}else{
						String arrayKeys[] = keys.split(",");
						for(String key : arrayKeys){
							if(jo.has(key)){
								f.setAccessible(true);
								Class<?> itemClazz = a.listItem();
								if(itemClazz == Void.class){
									internalParsePrimaryClass(a,f, item, jo, key);
								}else{
									internalParseCustomClass(itemClazz, f, item, jo, key);
								}
								break;
							}
						}
					}
				}
			}catch(Exception e){
				e.printStackTrace();
			}
		}
	}
	
	private static boolean internalParsePrimaryClass(
			JsonAnnotation a,
			Field f,
			Object item,
			JSONObject jo,
			String jsonKey) throws Exception{
		final Class<?> clazz = f.getType();
		if(a != null){
			if(a.checkFieldNull()){
				if(clazz.equals(String.class)){
					if(f.get(item) != null){
						return true;
					}
				}
			}
		}
		if(jo.isNull(jsonKey)){
			if(clazz.equals(String.class)){
				f.set(item, "");
			}
		}else{
			if(clazz.equals(String.class)){
				f.set(item, jo.getString(jsonKey));
			}else if(clazz.equals(int.class)){
				f.set(item, jo.getInt(jsonKey));
			}else if(clazz.equals(boolean.class)){
				final String value = jo.getString(jsonKey);
				f.set(item, "1".equals(value) || "true".equals(value));
			}else if(clazz.equals(long.class)){
				f.set(item, jo.getLong(jsonKey));
			}else if(clazz.equals(double.class)){
				f.set(item, jo.getDouble(jsonKey));
			}else if(clazz.equals(float.class)){
				f.set(item, (float)jo.getDouble(jsonKey));
			}else{
				return false;
			}
		}
		return true;
	}
	
	private static void internalParseCustomClass(Class<?> itemClazz,
			Field f,
			Object item,
			JSONObject jo,
			String jsonKey) throws Exception{
		if(List.class.isAssignableFrom(f.getType())){
			f.set(item,parseArrays(jo, jsonKey, itemClazz));
		}else{
			f.set(item, buildObject(itemClazz, getJSONObject(jo,jsonKey)));
		}
	}
	
	public static <T> List<T> parseArrays(JSONObject jo, List<T> items, String fieldName, Class<T> clazz){
		try{
			JSONArray ja = getJSONArray(jo, fieldName);
			return parseArrays(ja, items, clazz);
		}catch(Exception e){
			//e.printStackTrace();
		}
		return Collections.emptyList();
	}
	
	public static <T> List<T> parseArrays(JSONObject jo, String fieldName, Class<T> clazz){
		try{
			JSONArray ja = getJSONArray(jo, fieldName);
			return parseArrays(ja, clazz);
		}catch(Exception e){
			//e.printStackTrace();
		}
		return Collections.emptyList();
	}
	
	public static <T> List<T> parseArrays(JSONArray ja, Class<T> clazz){
		return parseArrays(ja, new ArrayList<T>(), clazz);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> List<T> parseArrays(JSONArray ja, List<T> items, Class<T> clazz){
		if(clazz.equals(String.class)){
			return (List<T>)parseStringArray(ja, (List<String>)items);
		}else{
			try{
				int length = ja.length();
				for(int index = 0;index < length;++index){
					try{
						Object obj = ja.get(index);
						if(obj instanceof JSONObject){
							items.add(buildObject(clazz, (JSONObject)obj));
						}else{
							Constructor<T> c = clazz.getDeclaredConstructor(String.class);
							c.setAccessible(true);
							items.add(c.newInstance(ja.getString(index)));
						}
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			}catch(Exception e){
				//e.printStackTrace();
			}
			return items;
		}
	}
	
	public static <T> T buildObject(Class<T> clazz,JSONObject jo) throws Exception{
		T item = null;
		try{
			Constructor<T> c = clazz.getDeclaredConstructor(JSONObject.class);
			c.setAccessible(true);
			item = c.newInstance(jo);
		}catch(Exception e){
			if(IDObject.class.isAssignableFrom(clazz)){
				Constructor<T> c = clazz.getDeclaredConstructor(String.class);
				c.setAccessible(true);
				String jsonKey = "id";
				for(Class<?> cz = clazz;cz != null;cz = cz.getSuperclass()){
					JsonImplementation ji = cz.getAnnotation(JsonImplementation.class);
					if(ji != null){
						jsonKey = ji.idJsonKey();
						break;
					}
				}
				String arrayKeys[] = jsonKey.split(",");
				for(String key : arrayKeys){
					if(jo.has(key)){
						item = c.newInstance(jo.getString(key));
						parseAll(jo, item);
						break;
					}
				}
				if(item == null){
					item = c.newInstance(jo.getString(jsonKey));
					parseAll(jo, item);
				}
			}else{
				Constructor<T> c = clazz.getDeclaredConstructor();
				c.setAccessible(true);
				item = c.newInstance();
				parseAll(jo, item);
			}
		}
		return item;
	}
	
	private static void onJsonParseEnd(Object item,JSONObject jo){
		String method = "onJsonParseEnd";
		for(Class<?> c = item.getClass();c != null;c = c.getSuperclass()){
			try{
				Method m = c.getDeclaredMethod(method,JSONObject.class);
				m.setAccessible(true);
				m.invoke(item,jo);
				break;
			}catch(Exception e){
			}
		}
	}
	
	public static List<String> parseStringArray(JSONObject jo, String fieldName){
		List<String> items = new ArrayList<String>();
		return parseStringArray(jo, fieldName, items);
	}
	
	public static List<String> parseStringArray(JSONObject jo, String fieldName, List<String> items){
		try{
			JSONArray ja = getJSONArray(jo, fieldName);
			return parseStringArray(ja, items);
		}catch(Exception e){
			//e.printStackTrace();
		}
		return items;
	}
	
	public static List<String> parseStringArray(JSONArray ja){
		return parseStringArray(ja, new ArrayList<String>());
	}
	
	public static List<String> parseStringArray(JSONArray ja, List<String> items){
		try{
			int length = ja.length();
			for(int index = 0;index < length;++index){
				items.add(ja.getString(index));
			}
		}catch(Exception e){
			//e.printStackTrace();
		}
		return items;
	}
	
	public static JSONArray getJSONArray(JSONObject jo, String key) throws JSONException {
		Object o = jo.get(key);
		if(o instanceof JSONArray){
			return (JSONArray)o;
		}
		return new JSONArray(o.toString());
	}
	
	public static JSONObject getJSONObject(JSONObject jo, String key) throws JSONException {
		Object o = jo.get(key);
		if(o instanceof JSONObject){
			return (JSONObject)o;
		}
		return new JSONObject(o.toString());
	}
	
	public static JSONObject safePutMapToJsonObject(JSONObject jo,
                                                    HashMap<String, String> values) {
		if(jo == null){
			jo = new JSONObject();
		}
		try {
			for (Entry<String, String> e : values.entrySet()) {
				if (!"null".equals(e.getValue())) {
					jo.put(e.getKey(), e.getValue());
				}
			}
		} catch (Exception e) {
		}
		return jo;
	}
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, String> safePutJsonObjectToMap(HashMap<String, String> values,
			JSONObject jo) {
		if(values == null){
			values = new HashMap<String, String>();
		}
		Iterator<String> it = jo.keys();
		while (it.hasNext()) {
			final String key = it.next();
			try {
				if(!jo.isNull(key)){
					values.put(key, jo.getString(key));
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return values;
	}
	
	@SuppressWarnings("rawtypes")
	public static JSONObject itemToJSONObject(Object item){
		JSONObject jo = new JSONObject();
		for (Class<?> cls = item.getClass(); cls != null; cls = cls
				.getSuperclass()) {
			for (Field f : cls.getDeclaredFields()) {
				try{
					f.setAccessible(true);
					String key = null;
					JsonAnnotation a = f
							.getAnnotation(JsonAnnotation.class);
					if (a == null) {
						key = f.getName();
					} else {
						String jsonKeys = a.jsonKey();
						if (TextUtils.isEmpty(jsonKeys)) {
							key = f.getName();
						} else {
							String keys[] = jsonKeys.split(",");
							if (keys != null && keys.length > 0) {
								key = keys[0];
							} else {
								key = f.getName();
							}
						}
					}
					if (!TextUtils.isEmpty(key)) {
						if (f.getType() == List.class) {
							JSONArray ja = new JSONArray();
							List list = (List) f.get(item);
							if (list != null) {
								for (Object l : list) {
									if (l instanceof String) {
										ja.put(l);
									} else {
										ja.put(itemToJSONObject(l));
									}
								}
							}
							jo.put(key, ja);
						} else {
							Object o = f.get(item);
							if (o != null) {
								jo.put(key, o.toString());
							}
						}
					}
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		return jo;
	}
	
	public static boolean safeGetBoolean(JSONObject jo, String key){
		try {
			if(jo.isNull(key)){
				return false;
			}
			final String value = jo.getString(key);
			return "1".equals(value) || "true".equals(value);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
