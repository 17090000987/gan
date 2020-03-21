package gan.network;

import java.util.HashMap;
import java.util.Map;

public class MapValueBuilder {

	private HashMap<String, String> mValues = new HashMap<String, String>();
	
	public MapValueBuilder put(String key, String value){
		mValues.put(key, value);
		return this;
	}
	
	public MapValueBuilder put(String key, int value){
		mValues.put(key, String.valueOf(value));
		return this;
	}
	
	public MapValueBuilder put(String key, long value){
		mValues.put(key, String.valueOf(value));
		return this;
	}
	
	public MapValueBuilder put(String key, boolean value){
		mValues.put(key, value ? "1" : "0");
		return this;
	}
	
	public MapValueBuilder put(String key, float value){
		mValues.put(key, String.valueOf(value));
		return this;
	}
	
	public MapValueBuilder put(String key, double value){
		mValues.put(key, String.valueOf(value));
		return this;
	}
	
	public MapValueBuilder putAll(Map<String,String> values){
		mValues.putAll(values);
		return this;
	}
	
	public HashMap<String, String> build(){
		return mValues;
	}
}
