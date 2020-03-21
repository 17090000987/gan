package gan.web.spring;

import gan.core.utils.TextUtils;
import gan.core.utils.Encrypter;

import javax.servlet.*;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;

/**
 * 
 * @since 2016年5月30日
 * @comment 跨域过滤器
 */
public class CrossOriginFilter implements Filter {

	public final static String Http_Key = "xbcx2018";
	
    private FilterConfig config = null;

    @Override
    public void init(FilterConfig config) throws ServletException {
        this.config = config;
    }

    @Override
    public void destroy() {
        this.config = null;
    }

    /**
     * 
     * @since 2016/5/30
     * @comment 跨域的设置
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
            FilterChain chain) throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        // 表明它允许"http://xxx"发起跨域请求
        httpResponse.setHeader("Access-Control-Allow-Origin",
                config.getInitParameter("AccessControlAllowOrigin"));
        // 表明在xxx秒内，不需要再发送预检验请求，可以缓存该结果
        httpResponse.setHeader("Access-Control-Allow-Methods",
                config.getInitParameter("AccessControlAllowMethods"));
        // 表明它允许xxx的外域请求
        httpResponse.setHeader("Access-Control-Max-Age",
                config.getInitParameter("AccessControlMaxAge"));
        // 表明它允许跨域请求包含xxx头
        httpResponse.setHeader("Access-Control-Allow-Headers",
                config.getInitParameter("AccessControlAllowHeaders"));
        chain.doFilter(request, response);
        
//        final String app_key = request.getParameter("app_key");
//        final String AesKey = Encrypter.decryptByAes(app_key, Http_Key);
//        final String deviceuuid = request.getParameter("deviceuuid");
//        if(!deviceuuid.equals(AesKey)){
//        	throw new ServletException("不合法的请求");
//        }
        
//        if(!isSigned(request,Http_Key)){
//        	throw new ServletException("不合法的请求");
//        }
    }
    
	public boolean isSigned(ServletRequest request,String httpKey) throws UnsupportedEncodingException{
		Map<String, String[]> params = request.getParameterMap();
		final String sign = request.getParameter("sign");
		List<String> keys = new ArrayList<>();
		keys.addAll(params.keySet());
		keys.add("key");
		Collections.sort(keys, new Comparator<String>() {
			@Override
			public int compare(String key1, String key2) {
				return key1.compareTo(key2);
			}
		});

		StringBuffer sb = new StringBuffer();
		for (String key : keys) {
			if ("sign".equals(key)) {
				continue;
			}
			if("key".equals(key)){
				sb.append(key).append("=")
				.append(httpKey)
				.append("&");
			}else{
				String[] values = params.get(key);
				for (int i = 0; i < values.length; i++) {
					String value = values[i];
					if(!TextUtils.isEmpty(value)){
						String s = URLEncoder.encode(value,"UTF-8");
						while(s.contains("(")){
							s = s.replace("(", "%28");
						}
						while(s.contains(")")){
							s = s.replace(")", "%29");
						}
						while(s.contains("\"")){
							s = s.replace("\"", "%22");
						}
						while(s.contains("!")){
							s = s.replace("!", "%21");
						}
						while(s.contains("'")){
							s = s.replace("'", "%27");
						}
						while(s.contains("*")){
							s = s.replace("*", "%2A");
						}
						sb.append(key).append("=")
						.append(s)
						.append("&");
					}
				}
			}
		}
		final String mySign = Encrypter.encryptByMD5(sb.substring(0, sb.length() - 1));
		return mySign.equals(sign);
    }
    
    public static String buildSign(HashMap<String, String> params,String key){
    	params.put("key", key);
    	List<String> keys = new ArrayList<>();
    	keys.addAll(params.keySet());
    	Collections.sort(keys, new Comparator<String>(){
			@Override
			public int compare(String key1, String key2) {
				return key1.compareTo(key2);
			}
    	});
		StringBuffer sb = new StringBuffer();
		for(String p_key : keys){
			final String value = params.get(p_key);
			if(!TextUtils.isEmpty(value)){
				String s = value;
				while(s.contains("(")){
					s = s.replace("(", "%28");
				}
				while(s.contains(")")){
					s = s.replace(")", "%29");
				}
				while(s.contains("\"")){
					s = s.replace("\"", "%22");
				}
				while(s.contains("!")){
					s = s.replace("!", "%21");
				}
				while(s.contains("'")){
					s = s.replace("'", "%27");
				}
				while(s.contains("*")){
					s = s.replace("*", "%2A");
				}
				sb.append(p_key).append("=")
				.append(s)
				.append("&");
			}
		}
		return Encrypter.encryptByMD5(sb.substring(0, sb.length() - 1));
	}

}