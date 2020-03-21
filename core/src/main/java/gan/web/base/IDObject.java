package gan.web.base;

import java.io.Serializable;

public class IDObject implements Serializable{
	private static final long serialVersionUID = 1L;
	
	protected String id;

	public IDObject(String id){
		this.id = id;
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this){
			return true;
		}
		if(o != null && getClass().isInstance(o)){
			return getId().equals(((IDObject)o).getId());
		}
		return false;
	}

	@Override
	public int hashCode() {
		final String id = getId();
		return id == null ? super.hashCode() : id.hashCode();
	}

	public String getId(){
		return id;
	}
	
	public void setId(String id){
		this.id = id;
	}
}
