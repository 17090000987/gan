package gan.web.base;

public class HttpPage {

	int 	offset;
	boolean hasmore;
	
	public HttpPage(int offset,boolean hasmore) {
		this.offset = offset;
		this.hasmore = hasmore;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public boolean isHasmore() {
		return hasmore;
	}

	public void setHasmore(boolean hasmore) {
		this.hasmore = hasmore;
	}
}
