package gan.web.base;

public class BaseUser extends IDObject {
	private static final long serialVersionUID = 1L;
	
	public String name;

	public String avatar;

	public BaseUser(String id) {
		super(id);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}
}
