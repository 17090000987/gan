package gan.web.base;

public class PageResult extends Result {
	
	public boolean 	hasmore;
	public int 		offset;
	
	public PageResult(boolean hasmore, int offset) {
		super();
		this.hasmore = hasmore;
		this.offset = offset;
	}
	public boolean isHasmore() {
		return hasmore;
	}
	public PageResult setHasmore(boolean hasmore) {
		this.hasmore = hasmore;
		return this;
	}
	public int getOffset() {
		return offset;
	}
	public PageResult setOffset(int offset) {
		this.offset = offset;
		return this;
	}
	
}
