package gan.network;

import java.util.HashMap;

public class NetParamsMap extends HashMap<String,String>{

    @Override
    public String put(String key, String value) {
        return super.put(key.trim().toLowerCase(), value);
    }

    @Override
    public String get(Object key) {
        return super.get(key.toString().trim().toLowerCase());
    }

    @Override
    public boolean containsKey(Object key) {
        return super.containsKey(key.toString().trim().toLowerCase());
    }

}
