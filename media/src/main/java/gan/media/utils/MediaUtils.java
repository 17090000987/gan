package gan.media.utils;

import gan.core.utils.TextUtils;
import gan.media.MediaApplication;
import gan.network.NetParamsMap;
import gan.web.config.MediaConfig;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.*;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class MediaUtils {

    final static String end = "\r\n";

    public static String parseContent(BufferedReader br, int content_len) throws IOException {
        StringBuffer sb = new StringBuffer();
        int len=0;
        while (len<content_len){
            sb.append(br.readLine()).append(end);
            len=sb.toString().getBytes("UTF-8").length;
        }
        return sb.toString();
    }

    public static long getUnsignedIntt (int data){     //将int数据转换为0~4294967295 (0xFFFFFFFF即DWORD)。
        return data&0x0FFFFFFFFl;
    }

    public static boolean isLocalURL(String url){
        try{
            URI uri = URI.create(url);
            String host = uri.getHost();
            if(isLocalIP(host)){
                return true;
            }
            try {
                InetAddress addr = InetAddress.getByName(host);
                return isLocalIP(addr.getHostAddress());
            } catch (UnknownHostException e) {
            }
        }catch (Exception e){
            return false;
        }
        return false;
    }

    public static boolean isLocalIP(String ip){
        if(TextUtils.isEmpty(ip)){
            return false;
        }
        if(ip.equals("127.0.0.1")||ip.equals("localhost")||ip.equals("0:0:0:0:0:0:0:1")){
            return true;
        }
        try {
            InetAddress addr = InetAddress.getLocalHost();
            if(ip.equals(addr.getHostAddress())){
                return true;
            }
            addr = getLocalHostLANAddress();
            return ip.equals(addr.getHostAddress());
        } catch (UnknownHostException e) {
        }
        return false;
    }

    // 正确的IP拿法，即优先拿site-local地址
    @SuppressWarnings("rawtypes")
    public static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    public static Map<String,String> getUrlParamsMap(String url){
        String paramsStr = getUrlParamsStr(url);
        if(TextUtils.isEmpty(paramsStr)){
            return Collections.EMPTY_MAP;
        }

        HashMap<String,String> paramsMap = new NetParamsMap();
        if(paramsStr.contains("&")){
            for(String param:paramsStr.split("&")){
                if(param.contains("=")){
                    String[] p = param.split("=");
                    paramsMap.put(p[0],p[1]);
                }
            }
        }else{
            if(paramsStr.contains("=")){
                String[] p = paramsStr.split("=");
                paramsMap.put(p[0],p[1]);
            }
        }
        return paramsMap;
    }

    public static String getUrlParamsStr(String url){
        if(url.contains("?")){
            return url.substring(url.indexOf("?")+1);
        }
        return null;
    }

    /**
     * 解析token作为连接识别（部分摄像头，地址不停变化）
     * @param url
     * @return
     */
    public static String parseToken(String url){
        try{
            Map<String, String> params = getUrlParamsMap(url);
            String gToken = params.get("gToken");
            if(!TextUtils.isEmpty(gToken)){
                return gToken;
            }
        }catch (Exception e){
        }
        return url;
    }

    /**
     * 解析url作为网络url（部分摄像头，地址不停变化）
     * @param url
     * @return
     */
    public static String parseUrl(String url){
        try{
            Map<String, String> params = getUrlParamsMap(url);
            String gUrl = params.get("gUrl");
            if(!TextUtils.isEmpty(gUrl)){
                return gUrl;
            }
            String gToken = params.get("gToken");
            if(!TextUtils.isEmpty(gToken)){
                if(url.contains("?")){
                    return url.substring(0, url.indexOf("?"));
                }
            }
        }catch (Exception e){
        }
        return url;
    }

    public static int safeParseInt(String s){
        return safeParseInt(s,0);
    }

    public static int safeParseInt(String s,int defaultValue){
        try{
            return Integer.parseInt(s);
        }catch(Exception e){
        }
        return defaultValue;
    }

    public static String generateRtspURL(String rtsp){
        MediaConfig gan = MediaApplication.getMediaConfig();
        String host = null;
        try {
            host = Inet4Address.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if(TextUtils.isEmpty(host)){
            host = "192.168.80.96";
        }
        return "rtsp://"+host+":"+gan.rtspPort+"/"+rtsp;
    }

    public static String parseName(String rtsp){
        try{
            if(rtsp.contains("://")){
                final String temp = rtsp.substring(rtsp.indexOf("://")+3);
                return temp.substring(temp.indexOf("/")+1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return rtsp;
    }

    public static boolean isRtspURL(String url){
        if(!TextUtils.isEmpty(url)){
            return url.startsWith("rtsp://");
        }
        return false;
    }

}
