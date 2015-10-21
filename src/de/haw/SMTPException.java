package de.haw;

public class SMTPException extends RuntimeException {
	
	public SMTPException(String messageFromServer) {
		super("SMTP connection failed.\n Server message was: " + messageFromServer);
	}

}
