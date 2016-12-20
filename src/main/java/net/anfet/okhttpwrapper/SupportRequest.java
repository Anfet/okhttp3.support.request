package net.anfet.okhttpwrapper;


import junit.framework.Assert;

import net.anfet.tasks.Runner;
import net.anfet.tasks.Tasks;

import java.net.SocketTimeoutException;
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

import okhttp3.Cache;
import okhttp3.CacheControl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Запрос поддержки okhttp.
 * <p>
 * Класс выполняет рутинные действия инициализации и запроса для okhttp.
 * {@link WorkerThreadListener} слушатели предоставляют полную state-machine для каждого запроса.
 * <p>
 * Изначально инициализируется {@link #init(int, int, Cache, Interceptor...)}} для установки okhttp контекста.
 */
public class SupportRequest {

	/**
	 * контекст okhttp
	 */
	private static OkHttpClient okHttpClient = null;
	/**
	 * контроль кэша
	 */
	private static CacheControl cacheControl = null;

	/**
	 * слушатель
	 */
	private WorkerThreadListener listener;
	/**
	 * билдер запроса
	 */
	private Request.Builder builder;
	/**
	 * ответ
	 */
	private Response response;
	/**
	 * запрос
	 */
	private Request request;

	/**
	 * время выборки
	 */
	private Long fetch;

//	private int hashCode = 0;


//	private static int hashCode(String method, String url, RequestBody body) {
//		synchronized (crc32) {
//			crc32.reset();
//			String hashString = method + ":" + url + ":" + body.toString();
//			crc32.update(hashString.getBytes());
//			return (int) crc32.getValue();
//		}
//	}

	private SupportRequest() {
		builder = new Request.Builder();
		builder.cacheControl(cacheControl);
		listener = null;
		response = null;
	}

	public static void initCacheControl(CacheControl cacheControl) {
		SupportRequest.cacheControl = cacheControl;
	}

	private static SupportRequest make(String method, String url, RequestBody body) {
		SupportRequest request = new SupportRequest();
		request
				.builder
				.method(method, body)
				.url(url);

//		request.hashCode = hashCode(method, url, body);

		return request;
	}

	public static SupportRequest get(String url) {
		return make("GET", url, null);
	}

	public static SupportRequest post(String url, RequestBody postBody) {
		return make("POST", url, postBody);
	}

	public static SupportRequest delete(String url, RequestBody postBody) {
		return make("DELETE", url, postBody);
	}

	public static SupportRequest put(String url, RequestBody postBody) {
		return make("PUT", url, postBody);
	}

	public static void init(int readTimeout, int connectTimeout, Cache cache, Interceptor... interceptors) {
		Assert.assertNull(okHttpClient);

		OkHttpClient.Builder builder = new OkHttpClient.Builder()
											   .readTimeout(readTimeout, TimeUnit.MILLISECONDS)
											   .connectTimeout(connectTimeout, TimeUnit.MILLISECONDS);

		for (Interceptor interceptor : interceptors) {
			builder.addInterceptor(interceptor);
		}

		if (cache != null) {
			builder.cache(cache);
		}

		okHttpClient = builder.build();
	}

	public static OkHttpClient getOkHttpClient() {
		return okHttpClient;
	}

	public static void setOkHttpClient(OkHttpClient okHttpClient) {
		SupportRequest.okHttpClient = okHttpClient;
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

	public SupportRequest setListener(WorkerThreadListener listener) {
		this.listener = listener;
		return this;
	}

	public SupportRequest addHeader(String name, String value) {
		builder.addHeader(name, value);
		return this;
	}

	public void queue(Object owner) {

		Tasks.execute(new Runner(owner) {

			Object result = null;

			@Override
			protected void onPreExecute() throws Exception {
				super.onPreExecute();

				//билдим запрос
				request = builder.build();
				builder = null;

				if (listener != null) {
					listener.publishPreExecute(SupportRequest.this, request);
				}
			}

			@Override
			protected void doInBackground() throws Exception {
				response = okHttpClient.newCall(request).execute();
				fetch = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
				try {
					if (listener != null) {
						result = listener.publishResponce(SupportRequest.this, response);
					}
				} finally {
					response.close();
				}
			}

			@Override
			protected void onPostExecute() {
				super.onPostExecute();
				if (listener != null) {
					listener.publishPostProcess(SupportRequest.this, response, result);
				}
			}

			@Override
			protected void onError(Throwable throwable) {
				super.onError(throwable);

				if (throwable instanceof SocketTimeoutException) {
					throwable = new SocketTimeoutException("Unable to connect to server in " + okHttpClient.connectTimeoutMillis() + " msec");
				}

				if (listener != null) {
					listener.publishError(SupportRequest.this, throwable);
				}
			}

			@Override
			protected void onFinished() {
				super.onFinished();
				if (listener != null) {
					listener.publishComplete(SupportRequest.this);
				}

				builder = null;
				listener = null;
				request = null;
				result = null;
				response = null;
			}


		});
	}


	public void execute() {
		try {

			//билдим запрос
			request = builder.build();
			if (listener != null) listener.publishPreExecute(this, request);

			builder = null;
			try {
				//запускаем на выполение
				response = okHttpClient.newCall(request).execute();

				if (listener != null) {
					Object result = listener.publishResponce(this, response);
					listener.publishPostProcess(this, response, result);
				}
			} finally {
				response.close();
			}
		} catch (Exception e) {
			if (listener != null) listener.publishError(this, e);
		}

		if (listener != null) listener.publishComplete(this);
	}

//	@Override
//	public int hashCode() {
//		return hashCode;
//	}

	@Override
	public String toString() {
		return request == null ? super.toString() : request.toString();
	}
}
