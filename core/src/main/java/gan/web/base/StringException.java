package gan.web.base;

public class StringException extends Exception {
	private static final long serialVersionUID = 1L;

	String message;
	
	public StringException(String message) {
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		return message;
	}
}
