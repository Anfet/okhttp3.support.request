package net.anfet.okhttpwrapper;

/**
 * ConnectionTimeout exception
 */

public class ConnectionTimeout extends RuntimeException {

	public ConnectionTimeout() {

	}

	public ConnectionTimeout(String message) {
		super(message);
	}
}
