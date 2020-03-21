package gan.core.file;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gan.core.system.server.SystemServer;
import gan.core.utils.TextUtils;
import gan.core.system.SystemUtils;

import java.io.*;
import java.util.HashMap;

public class SharePerference{

    HashMap<String,String> mMapValues = new HashMap<>();

    String mFileName;
    String mFilePath;

    public SharePerference(String fileName) throws IOException {
        mFileName = fileName;
        mFilePath = SystemServer.getRootPath("/generate/SharePerference/"+mFileName);
        if(FileHelper.checkOrCreateDirectory(mFilePath)){
            File file = new File(mFilePath);
            file.createNewFile();
        }
        if(FileHelper.isFileExists(mFilePath)){
            BufferedReader br = new BufferedReader(new FileReader(mFilePath));
            String json = FileHelper.readFileToString(mFilePath);
            SystemUtils.close(br);
            if(!TextUtils.isEmpty(json)){
                ObjectMapper objectMapper = new ObjectMapper();
                mMapValues.putAll(objectMapper.readValue(json, HashMap.class));
            }
        }
    }

    public SharePerference putString(String key, String value){
        mMapValues.put(key,value);
        return this;
    }

    public SharePerference remove(String key){
        mMapValues.remove(key);
        return this;
    }

    public String getString(String key){
        return mMapValues.get(key);
    }

    public SharePerference putObject(String key, Object object) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mMapValues.put(key,mapper.writeValueAsString(object));
        return this;
    }

    public <T> T getObject(String key,Class<T> cls){
        try {
            final String json = mMapValues.get(key);
            if(TextUtils.isEmpty(json)){
                return null;
            }
            ObjectMapper mapper = new ObjectMapper();
            return (T)mapper.readValue(json,cls);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void commit(){
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(mFilePath);
            ObjectMapper mapper = new ObjectMapper();
            fos.write(mapper.writeValueAsString(mMapValues).getBytes("UTF-8"));
            fos.flush();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(fos!=null){
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
