package net.anfet.okhttpwrapper;

import android.os.Handler;

import net.anfet.okhttpwrapper.abstraction.ResponceProcessor;

import java.util.concurrent.CountDownLatch;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Дженерик для обработки любых запросов и выдачей результата в UI поток
 */
public class MainThreadListener<T> extends WorkerThreadListener<T> {

	private final Handler handler;
	private Exception exception;

	public MainThreadListener(ResponceProcessor<T> processor) {
		super(processor);
		handler = new Handler();
	}

	@Override
	void publishPostProcess(final SupportRequest supportRequest, final Response response, final T t) {
		final CountDownLatch latch = new CountDownLatch(1);
		handler.post(new Runnable() {
			@Override
			public void run() {
				onPostProcess(supportRequest, response, t);
				latch.countDown();
			}
		});

		try {
			latch.await();
		} catch (InterruptedException ignored) {
			//thread can be interrupted. it's ok
		}
	}

	@Override
	void publishError(final SupportRequest supportRequest, final Throwable throwable) {
		final CountDownLatch latch = new CountDownLatch(1);
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
			//thread can be interrupted. it's ok
		}
	}

	@Override
	void publishComplete(final SupportRequest supportRequest) {
		final CountDownLatch latch = new CountDownLatch(1);
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
			//thread can be interrupted. it's ok
		}
	}

	@Override
	void publishPreExecute(final SupportRequest supportRequest, final Request request) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		handler.post(new Runnable() {
			@Override
			public void run() {
				try {
					onPreExecute(supportRequest, request);
				} catch (Exception e) {
					exception = e;
				}
				latch.countDown();
			}
		});

		try {
			latch.await();

			if (exception != null) {
				throw exception;
			}
		} catch (InterruptedException ignored) {
			//thread can be interrupted. it's ok
		}
	}

	/**
	 * Функция вызывается в основном потоке после вызоыв {@link #publishProgress(SupportRequest, Object...)}
	 * @param supportRequest выполняемый запрос
	 * @param params         параметры
	 */
	protected void onPublishProgress(SupportRequest supportRequest, Object... params) {

	}

	/**
	 * функция для обновления чего-то (прогресса работы). Может вызываться где угодно и вызывает в основном потоке {@link #onPublishProgress(SupportRequest, Object...)}
	 * @param params параметры для передачи
	 */
	public final void publishProgress(final SupportRequest supportRequest, final Object... params) {

		if (!supportRequest.getRunner().alive()) return;

		final CountDownLatch latch = new CountDownLatch(1);
		handler.post(new Runnable() {
			@Override
			public void run() {
				onPublishProgress(supportRequest, params);
				latch.countDown();
			}
		});

		try {
			latch.await();
		} catch (InterruptedException ignored) {
			//thread can be interrupted. it's ok
		}

	}
}