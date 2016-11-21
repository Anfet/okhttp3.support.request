package net.anfet.okhttpwrapper;


import android.util.Log;

import junit.framework.Assert;

import net.anfet.tasks.Runner;
import net.anfet.tasks.Tasks;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by Oleg on 13.07.2016.
 */
public class SupportRequest {


	private static final HashMap<String, String> persistentHeaders = new HashMap<>();
	private static long TxRx = 0L;
	private static OkHttpClient okHttpClient = null;
	private static AtomicInteger ai = new AtomicInteger(0);

	private final HashMap<String, String> headers;
	private final int id;
	private ISupportRequestListener listener;
	private Request.Builder builder;
	private Request request;
	private Response response;
	private Runner runningTask;

	public SupportRequest() {
		id = ai.incrementAndGet();
		headers = new HashMap<>();
		listener = null;
		request = null;
		response = null;
		runningTask = null;
	}

	public static SupportRequest get(String url) {
		SupportRequest supportRequest = new SupportRequest();
		supportRequest.builder = new Request.Builder();
		supportRequest.builder.url(url);
		return supportRequest;
	}

	public static SupportRequest post(String url, RequestBody postBody) {
		SupportRequest supportRequest = new SupportRequest();
		supportRequest.builder = new Request.Builder();
		supportRequest.builder.post(postBody);
		supportRequest.builder.url(url);
		return supportRequest;
	}

	public static SupportRequest delete(String url, RequestBody postBody) {
		SupportRequest supportRequest = new SupportRequest();
		supportRequest.builder = new Request.Builder();
		supportRequest.builder.delete(postBody);
		supportRequest.builder.url(url);
		return supportRequest;
	}

	public static SupportRequest put(String url, RequestBody postBody) {
		SupportRequest supportRequest = new SupportRequest();
		supportRequest.builder = new Request.Builder();
		supportRequest.builder.put(postBody);
		supportRequest.builder.url(url);
		return supportRequest;
	}

	public static void addPersistentHeader(String name, String value) {
		synchronized (persistentHeaders) {
			persistentHeaders.put(name, value);
		}
	}

	public static void removePersistentHeader(String name) {
		synchronized (persistentHeaders) {
			persistentHeaders.remove(name);
		}
	}

	public static void init(int readTimeout, int connectTimeout, HttpLoggingInterceptor.Level logLevel) {
		Assert.assertNull(okHttpClient);

		HttpLoggingInterceptor logging = new HttpLoggingInterceptor().setLevel(logLevel);
		okHttpClient = new OkHttpClient.Builder().readTimeout(readTimeout, TimeUnit.MILLISECONDS).connectTimeout(connectTimeout, TimeUnit.MILLISECONDS).addInterceptor(logging).build();
		SupportRequest.setOkHttpClient(okHttpClient);
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
		builder.hostnameVerifier(AllTrustHostnameVerifier.getInstance());

		return builder;

	}

	public int getId() {
		return id;
	}

	public SupportRequest setListener(ISupportRequestListener listener) {
		this.listener = listener;
		return this;
	}

	public SupportRequest addHeader(String name, String value) {
		builder.addHeader(name, value);
		return this;
	}

	public void queue(Object owner) {
		Tasks.execute(runningTask = new Runner(owner) {
			@Override
			protected void doInBackground() throws Exception {
				execute();
			}
		});
	}


	public void cancel() {
		if (runningTask != null && runningTask.isRuninng()) {
			runningTask.cancel();
		}
	}

	public void execute() {
		if (runningTask != null && !runningTask.isRuninng()) {
			return;
		}

		//добавляем в реквест хедеры
		for (String key : headers.keySet()) {
			builder.addHeader(key, headers.get(key));
		}

		//добавляем статичные хедеры
		for (String key : persistentHeaders.keySet()) {
			builder.addHeader(key, persistentHeaders.get(key));
		}

		try {
			//билдим запрос
			Request request = builder.build();


			//запускаем на выполение
			response = okHttpClient.newCall(request).execute();
			try {

				if (listener != null && (runningTask == null || runningTask.isRuninng())) {
					//если мы сюда привалили - значит запрос дошел до сервера и получился ответ. Может быть и не всегда хороший
					//оповещаем листенеры
					listener.publishResponce(this, response);
				}
			} finally {
				response.close();
			}
		} catch (IOException ex) {
			throw new ConnectionTimeout("Connection timeout to " + request.url());
		} catch (Exception e) {
			onProcessError(e);
		}

	}

	private void onProcessError(Exception e) {
		//если мы упали сюда, то тут может быть connectionTimeout или еще какая ересь на транспортном уровне. чаще всего может встречаться отмена запроса, которая тоже падает сюда

		if (listener != null && (runningTask == null || runningTask.isRuninng())) {
			try {
				listener.publishError(this, e);
			} catch (Exception ex) {
				Log.e(getClass().getSimpleName(), ex.getMessage(), ex);
			}
		} else {
			Log.e(getClass().getSimpleName(), e.getMessage(), e);
		}
	}

	public boolean isCancelled() {
		return runningTask != null && !runningTask.isRuninng();
	}
}
