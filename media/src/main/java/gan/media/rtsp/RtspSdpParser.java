package gan.media.rtsp;

import gan.core.system.SystemUtils;
import gan.core.utils.TextUtils;
import gan.log.DebugLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.WeakHashMap;

public class RtspSdpParser {

    private String mSdp;
    private String mServerIp;
    private WeakHashMap<String,String> mMapValues = new WeakHashMap<>();
    private ArrayList<String> trackIds = new ArrayList<>();
    private ArrayList<String> medias = new ArrayList<>();

    public void parserSdp(String sdpStr)throws IOException {
        mSdp = sdpStr;
        BufferedReader br = new BufferedReader(new StringReader(mSdp));
        try{
            String line;
            StringBuffer sb=null;
            while((line=br.readLine())!=null){
                if(line.startsWith("m=")){
                    if(sb!=null){
                        String media = sb.toString().trim();
                        DebugLog.info("media:"+media);
                        medias.add(media);
                    }
                    sb = new StringBuffer();
                }
                if(sb!=null){
                    sb.append(line).append("\r\n");
                }

                parseServerIp(line);
                parseTrack(line);
            }
            if(sb!=null){
                String media = sb.toString().trim();
                DebugLog.info("media:"+media);
                medias.add(media);
            }
        } finally {
            SystemUtils.close(br);
        }
    }

    public String getServerIp() {
        return mServerIp;
    }

    public void parseServerIp(String line){
        try{
            if(line.startsWith("o=")){
                String key = "IN IP4";
                int index = line.indexOf(key);
                if(index>0){
                    mServerIp = line.substring(index+key.length(),
                            line.length()).trim();
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public void parseTrack(String line){
        try{
            String key = "track";
            int index = line.indexOf(key);
            if(index>0){
                trackIds.add(line.substring(index,line.length()).trim());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    public Sdp parserSdp(String sdpStr,Sdp sdp)throws IOException{
        if(sdp == null){
            sdp = new Sdp();
        }
        sdp.sdp = sdpStr;
        parserSdp(sdpStr);
        sdp.vCodec = parseCodec("m=video");
        sdp.aCodec = parseCodec("m=audio");
        sdp.vPlayLoad = parsePlayLoad("m=video");
        sdp.aPlayLoad = parsePlayLoad("m=audio");
        sdp.vTrackId = parseTrackId("m=video");
        sdp.aTrackId = parseTrackId("m=audio");
        return sdp;
    }

    public String getValue(String key){
         String value = mMapValues.get(key);
         if(value!=null){
             return value;
         }
         return findValue(key,mSdp);
    }

    public String getValueInLine(String line,String... keys){
       for(String key:keys){
           String value = getValueInLine(line,key);
           if(value!=null){
               return value;
           }
       }
       return null;
    }

    public String getValueInLine(String line,String key){
        if(line!=null){
            try{
                int index = line.indexOf(key);
                if(index>0){
                    int beginIndex = index+key.length();
                    String value = line.substring(beginIndex).trim();
                    if(value.contains(" ")){
                        return value.split(" ")[0];
                    }
                    return value;
                }
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        return null;
    }

    public String getMedia(String key){
        for(String media:medias){
            if (media.contains(key.trim())) {
                return media;
            }
        }
        return null;
    }

    public String getMediaLineByKey(String mediaKey, String key){
        return getMediaLine(getMedia(mediaKey),key);
    }

    public String getMediaLine(String media, String key){
        if(!TextUtils.isEmpty(media)){
            BufferedReader br = new BufferedReader(new StringReader(media));
            try{
                String line;
                while((line=br.readLine())!=null){
                    if(line.startsWith(key)){
                        return line;
                    }
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                SystemUtils.close(br);
            }
        }
        return null;
    }

    public String findValue(String key,String values){
        BufferedReader br = new BufferedReader(new StringReader(values));
        try{
            String line;
            while((line=br.readLine())!=null){
                if(line.startsWith(key)){
                    mMapValues.put(key,line);
                    return line;
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            SystemUtils.close(br);
        }
        return null;
    }

    public ArrayList<String> getTrackIds(){
        return trackIds;
    }

    public int parsePlayLoad(String mediaKey){
        try{
            int value = Integer.valueOf(getValueInLine(
                    getMediaLineByKey(mediaKey,"a=rtpmap"),
                    "rtpmap:"));
            return value;
        }catch (Exception e){
            return -1;
        }
    }

    public int parseTrackId(String mediaKey){
        String line = getMediaLineByKey(mediaKey,"a=control");
        if(line!=null){
            try{
                line = line.toLowerCase();
                String key = "trackid=";
                int index = line.indexOf(key);
                if(index>0){
                    int beginIndex = index+key.length();
                    return Integer.valueOf(line.substring(beginIndex, line.length()));
                }
            }catch (Exception e){
            }
        }
        return -1;
    }

    public String parseCodec(String mediaKey){
        try{
            String mediaLine = getMediaLineByKey(mediaKey,"a=rtpmap");
            if(!TextUtils.isEmpty(mediaLine)){
                if(mediaLine.contains(" ")){
                    String[] infos = mediaLine.split(" ");
                    if(infos.length>0){
                        return infos[1];
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
