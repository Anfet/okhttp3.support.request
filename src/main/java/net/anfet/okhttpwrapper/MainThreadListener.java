package net.anfet.okhttpwrapper;

import android.os.Handler;

import net.anfet.okhttpwrapper.abstractions.ResponceProcessor;

import java.util.concurrent.CountDownLatch;

import okhttp3.Request;

/**
 * Дженерик для обработки любых запросов и выдачей результата в UI поток
 */
public class MainThreadListener<T> extends ThreadListener<T> {

	private Handler handler;
	private Exception error;
	private CountDownLatch latch;

	public MainThreadListener(ResponceProcessor<T> processor) {
		super(processor);
		error = null;
		handler = new Handler();
	}

	public MainThreadListener() {
		this(null);
	}

	@Override
	public void publishPreExecute(final SupportRequest supportRequest, final Request request) throws Exception {
		latch = new CountDownLatch(1);
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					onPreExecute(supportRequest, request);
				} catch (Exception e) {
					error = e;
				}
				latch.countDown();
			}
		});

		latch.await();
		if (error != null) {
			throw error;
		}
	}

	@Override
	public void publishPostProcess(final SupportRequest supportRequest, final T t) {
		latch = new CountDownLatch(1);
		handler.post(new Runnable() {
			@Override
			public void run() {
				onPostProcess(supportRequest, t);
				latch.countDown();
			}
		});

		try {
			latch.await();
		} catch (InterruptedException ignored) {
			//safely ignoring
		}
	}

	@Override
	public void publishError(final SupportRequest supportRequest, final Throwable throwable) {
		latch = new CountDownLatch(1);
		handler.post(new Runnable() {
			@Override
			public void run() {
				onError(supportRequest, throwable);
				latch.countDown();
			}
		});
		try {
			latch.await();
		} catch (InterruptedException ignored) {
			//safely ignoring
		}
	}


	@Override
	public void publishComplete(final SupportRequest supportRequest) {
		latch = new CountDownLatch(1);
		handler.post(new Runnable() {
			@Override
			public void run() {
				onComplete(supportRequest);
				latch.countDown();
			}
		});
		try {
			latch.await();
		} catch (InterruptedException ignored) {
			//safely ignoring
		}
	}
}