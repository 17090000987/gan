package gan.server.http;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RequestParams{

    protected final ConcurrentHashMap<String,String> params = new ConcurrentHashMap<>();
    protected final ConcurrentHashMap<String, FileWrapper> fileParams = new ConcurrentHashMap();

    public RequestParams(){
    }

    public RequestParams(RequestParams requestParams){
        params.putAll(requestParams.params);
        fileParams.putAll(requestParams.fileParams);
    }

    public RequestParams(Map<String,String> source){
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

    public File removeFile(String key){
        FileWrapper fileWrapper = fileParams.remove(key);
        if(fileWrapper!=null){
            return fileWrapper.file;
        }
        return null;
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

    public void put(String key, File file){
        put(key,file, "application/octet-stream");
    }

    public void put(String key, File file, String contentType){
        fileParams.put(key,new FileWrapper(file,contentType));
    }

    public ConcurrentHashMap<String, FileWrapper> getFileParams() {
        return fileParams;
    }

    public void put(String key, long value){
        put(key,String.valueOf(value));
    }

    public static class FileWrapper {
        public final File file;
        public String contentType;
        public FileWrapper(File file, String contentType) {
            this.file = file;
            this.contentType = contentType;
        }
    }
}
