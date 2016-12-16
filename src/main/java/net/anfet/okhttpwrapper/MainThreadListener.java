package net.anfet.okhttpwrapper;

import android.os.Handler;

import okhttp3.Response;

/**
 * Дженерик для обработки любых запросов и выдачей результата в UI поток
 */
public class MainThreadListener<T> extends WorkerThreadListener<T> {

	private Handler handler;

	public MainThreadListener(ResponceProcessor<T> processor) {
		super(processor);
		handler = new Handler();
	}

	@Override
	public void publishResponce(final SupportRequest supportRequest, final Response response) {
		try {
			final T result = processResult(supportRequest, response);
			publishPostProcess(supportRequest, response, result);
		} catch (InterruptedException ignored) {
		} catch (final Exception ex) {
			publishError(supportRequest, ex);
		}
	}

	private void publishPostProcess(final SupportRequest supportRequest, final Response response, final T result) {
		if (!supportRequest.isCancelled()) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					try {
						onPostProcess(supportRequest, response, result);
					} finally {
						onComplete(supportRequest);
					}
				}
			});
		}
	}

	@Override
	public void publishError(final SupportRequest supportRequest, final Throwable throwable) {
		if (!supportRequest.isCancelled()) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					try {
						onError(supportRequest, throwable);
					} finally {
						onComplete(supportRequest);
					}
				}
			});
		}
	}

	/**
	 * Функция вызывается в основном потоке после вызоыв {@link #publishProgress(SupportRequest, Object...)}
	 * @param supportRequest выполняемый запрос
	 * @param params         параметры
	 */
	public void onPublishProgress(SupportRequest supportRequest, Object... params) {

	}

	/**
	 * функция для обновления чего-то (прогресса работы). Может вызываться где угодно и вызывает в основном потоке {@link #onPublishProgress(SupportRequest, Object...)}
	 * @param params параметры для передачи
	 */
	public final void publishProgress(final SupportRequest supportRequest, final Object... params) {
		if (!supportRequest.isCancelled()) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					onPublishProgress(supportRequest, params);
				}
			});
		}
	}
}