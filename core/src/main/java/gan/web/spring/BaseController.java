package gan.web.spring;

import gan.web.base.StringException;
import org.springframework.beans.factory.annotation.Autowired;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BaseController {
	
	@Autowired
	protected HttpServletRequest mRequest;
	@Autowired
	protected HttpServletResponse mResponse;

	public String checkEmpty(String... key)throws StringException {
		return checkNull(key);
	}

	/**
	 * 名字不达意 deprecated insteal of checkEmpty
	 * @param key
	 * @return
	 * @throws StringException
	 */
	@Deprecated
	public String checkNull(String... key) throws StringException {
		String value = getParameter(key);
		if(value == null
				||"".equals(value)){
			throw new StringException("paramter:"+ key[0] +" is null");
		}
		return value;
	}
	
	public static void checkJson(String key,String json) throws StringException {
		try {
		} catch (Exception e) {
			throw new StringException(key +" json format error");
		}
	}
	
	public String getParameter(String... keys){
		for(int i=0;i<keys.length;i++){
			String key = keys[i];
			String value = mRequest.getParameter(key);
			if(value!=null){
				return value;
			}
		}
		return null;
	}

	public String getUser(){
		return getParameter("user");
	}
	
	public String checkUser() throws StringException {
		return checkNull("user");
	}
}
