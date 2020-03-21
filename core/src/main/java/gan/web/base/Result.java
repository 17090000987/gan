package gan.web.base;

import com.fasterxml.jackson.core.JsonProcessingException;
import gan.core.system.server.SystemServer;

import java.util.Collection;
import java.util.HashMap;

public class Result {

	public boolean ok;
	public int errorCode;
	public String message;
	public Collection<?> list;
	public Object data;
	public long servertime;

	@SuppressWarnings("rawtypes")
	public HashMap datas = new HashMap<>();

	public static Result ok(){
		return new Result().asOk();
	}

	public static <T extends Result> T ok(Class<T> clazz){
		try {
			return (T)clazz.newInstance().asOk();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static <T extends Result> T error(Class<T> clazz){
		try {
			return (T)clazz.newInstance().asError();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static Result error(){
		return new Result().asError();
	}
	
	public Result asOk(){
		ok = true;
		errorCode = 0;
		servertime = System.currentTimeMillis();
		return this;
	}
	
	public Result asError(){
		ok = false;
		errorCode = 1;
		servertime = System.currentTimeMillis();
		return this;
	}
	
	public static Result error(Exception e){
		return error(e.getMessage());
	}
	
	public static Result error(String message){
		return error().setMessage(message);
	}
	
	public Result setMessage(String message){
		this.message = message;
		return this;
	}
	
	public Result setList(Collection<?> items){
		list = items;
		return this;
	}
	
	public Result setData(Object data){
		this.data = data;
		return this;
	}
	
	public boolean isResultOk() {
		return ok;
	}

	public void setResultOk(boolean resultOk) {
		this.ok = resultOk;
	}

	public int getResultCode() {
		return errorCode;
	}

	public Result setResultCode(int resultCode) {
		this.errorCode = resultCode;
		return this;
	}

	public String getMessage() {
		return message;
	}

	public Collection<?> getList() {
		return list;
	}

	public Object getData() {
		return data;
	}

	@SuppressWarnings("rawtypes")
	public HashMap getDatas() {
		return datas;
	}
	
	@SuppressWarnings("unchecked")
	public Result addData(String key, Object value){
		datas.put(key, value);
		return this;
	}

	public Result setDatas(@SuppressWarnings("rawtypes") HashMap datas) {
		this.datas = datas;
		return this;
	}

	@Override
	public String toString() {
		try {
			return SystemServer.getObjectMapper().writeValueAsString(this);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
			return super.toString();
		}
	}
}
