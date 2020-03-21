package gan.web.base;

import java.awt.*;

public class ResultData {

	public List list;
	
	public Object data;

	public List getList() {
		return list;
	}

	public ResultData setList(List list) {
		this.list = list;
		return this;
	}

	public Object getData() {
		return data;
	}

	public ResultData setData(Object data) {
		this.data = data;
		return this;
	}
}
