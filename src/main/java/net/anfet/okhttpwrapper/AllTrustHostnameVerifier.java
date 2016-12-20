package net.anfet.okhttpwrapper;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * Trust all
 */
class AllTrustHostnameVerifier implements HostnameVerifier {
	private static final AllTrustHostnameVerifier instance = new AllTrustHostnameVerifier();

	public static HostnameVerifier getInstance() {
		return instance;
	}

	@Override
	public boolean verify(String hostname, SSLSession session) {
		return true;
	}
}
