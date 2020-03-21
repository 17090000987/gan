package gan.core.system.server;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Parameter{

    protected final ConcurrentHashMap<String,String> params = new ConcurrentHashMap<>();

    public Parameter(){
    }

    public Parameter(Parameter parameter){
        params.putAll(parameter.params);
    }

    public Parameter(Map<String,String> source){
        if(source!=null){
            for (Map.Entry<String, String> entry : source.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    public void add(String key,String value){
        put(key,value);
    }

    public void put(String key,String value){
        if (key != null && value != null) {
            params.put(key, value);
        }
    }

    public String get(String key){
        return params.get(key);
    }

    public String remove(String key){
        return params.remove(key);
    }

    public Map<String, String> getParams() {
        return Collections.unmodifiableMap(params);
    }

    public String getParamString() {
        StringBuffer sb = new StringBuffer();
        for(String key:params.keySet()){
            sb.append(key).append("=").append(params.get(key));
            sb.append("&");
        }
        return sb.substring(0,sb.length()-1);
    }

    public void put(String key, long value){
        put(key,String.valueOf(value));
    }
}

