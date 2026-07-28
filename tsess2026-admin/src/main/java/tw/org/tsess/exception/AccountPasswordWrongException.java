package tw.org.tsess.exception;

public class AccountPasswordWrongException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public AccountPasswordWrongException(String message) {
        super(message);
    }

}
