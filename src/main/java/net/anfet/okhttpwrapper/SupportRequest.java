package net.anfet.okhttpwrapper;


import junit.framework.Assert;

import net.anfet.okhttpwrapper.abstractions.IRequestListener;
import net.anfet.tasks.Runner;
import net.anfet.tasks.Tasks;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Запрос поддержки для okhttp
 */
public class SupportRequest {
	private static OkHttpClient httpClient = null;

	private IRequestListener listener;
	private Request.Builder builder;

	private Request request;
	private Response response;

	private Long fetch;
	private Long process;


	public SupportRequest() {
		builder = new Request.Builder();
		listener = null;
		response = null;
	}


	private static SupportRequest newRequest(String method, String url, RequestBody data) {
		SupportRequest request = new SupportRequest();
		request.setMethod(method, url, data);
		return request;
	}

	public static SupportRequest get(String url) {
		return newRequest("GET", url, null);
	}

	public static SupportRequest post(String url, RequestBody postBody) {
		return newRequest("POST", url, postBody);
	}

	private SupportRequest setMethod(String method, String url, RequestBody data) {
		builder.method(method, data);
		HttpUrl httpUrl = HttpUrl.parse(url);
		if (httpUrl == null) {
			throw new IllegalArgumentException("Malformed URL: " + url);
		}

		builder.url(httpUrl);
		return this;
	}

	public static SupportRequest delete(String url, RequestBody postBody) {
		return newRequest("DELETE", url, postBody);
	}

	public static SupportRequest put(String url, RequestBody postBody) {
		return newRequest("PUT", url, postBody);
	}

	public static void init(int readTimeout, int connectTimeout, HttpLoggingInterceptor.Level logLevel) {
		Assert.assertNull(httpClient);
		HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(logLevel);
		httpClient = new OkHttpClient.Builder()
							   .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
							   .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS)
							   .addInterceptor(logging).build();
	}

	public static void setOkHttpClient(OkHttpClient okHttpClient) {
		SupportRequest.httpClient = okHttpClient;
	}

	public static OkHttpClient.Builder getUnsafeOkHttpClient() throws NoSuchAlgorithmException, KeyManagementException {

		// Create a trust manager that does not validate certificate chains
		final TrustManager[] trustAllCerts = new TrustManager[]{
				new X509TrustManager() {
					@Override
					public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
					}

					@Override
					public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
					}

					@Override
					public java.security.cert.X509Certificate[] getAcceptedIssuers() {
						return new java.security.cert.X509Certificate[]{};
					}
				}
		};

		// Install the all-trusting trust manager
		final SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
		// Create an ssl socket factory with our all-trusting manager
		final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();

		OkHttpClient.Builder builder = new OkHttpClient.Builder();
		builder.sslSocketFactory(sslSocketFactory);
		builder.hostnameVerifier(new HostnameVerifier() {
			@Override
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		});

		return builder;

	}

	public SupportRequest setListener(IRequestListener listener) {
		this.listener = listener;
		return this;
	}

	public SupportRequest addHeader(String name, String value) {
		builder.addHeader(name, value);
		return this;
	}

	public void queue(Object owner) {
		Tasks.execute(new Runner(owner) {

			Object result;

			@Override
			protected void onPreExecute() throws Exception {
				super.onPreExecute();

				request = builder.build();
				builder = null;

				if (listener != null) {
					listener.publishPreExecute(SupportRequest.this, request);
				}
			}

			@Override
			protected void doInBackground() throws Exception {
				response = httpClient.newCall(request).execute();
				try {
					fetch = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
					if (listener != null) {
						result = listener.publishProcessResult(SupportRequest.this, request, response);
					}
				} finally {
					response.close();
				}
			}

			@Override
			protected void onPostExecute() {
				super.onPostExecute();

				if (listener != null) {
					listener.publishPostProcess(SupportRequest.this, result);
				}
			}

			@Override
			protected void onFinished() {
				super.onFinished();
				if (listener != null) {
					listener.publishComplete(SupportRequest.this);
				}

				result = null;
				builder = null;
				request = null;
				response = null;
				listener = null;
			}

			@Override
			protected void onError(Throwable throwable) {
				super.onError(throwable);
				if (listener != null) {
					listener.publishError(SupportRequest.this, throwable);
				}
			}
		});
	}


	public void execute() {
		try {
			try {
				//билдим запрос
				request = builder.build();

				//запускаем на выполение
				response = httpClient.newCall(request).execute();
				try {
					fetch = response.receivedResponseAtMillis() - response.sentRequestAtMillis();

					if (listener != null) {
						Object result = listener.publishProcessResult(this, request, response);
						listener.publishPostProcess(this, result);
					}
				} finally {
					response.close();
				}
			} catch (Exception e) {
				if (listener != null) {
					listener.publishError(this, e);
				}
			}
		} finally {
			if (listener != null) {
				listener.publishComplete(this);
			}

			builder = null;
			request = null;
			response = null;
			listener = null;
		}
	}

}
