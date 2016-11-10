package net.anfet.okhttpwrapper;

import android.os.Handler;

import okhttp3.Response;

/**
 * Дженерик для обработки любых запросов и выдачей результата в UI поток
 */
public class MainThreadListener<T> extends WorkerThreadListener<T> {

	protected Handler handler;

	public MainThreadListener(ResponceProcessor<T> processor) {
		super(processor);
		handler = new Handler();
	}

	@Override
	public void publishResponce(final SupportRequest supportRequest, final Response response) {
		try {
			final T result = processResult(supportRequest, response);

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
		} catch (InterruptedException ex) {
			return;
		} catch (final Exception ex) {
			if (!supportRequest.isCancelled()) {
				publishError(supportRequest, ex);
			}
		}
	}

	@Override
	public void publishError(final SupportRequest supportRequest, final Throwable throwable) {
		handler.post(new Runnable() {
			@Override
			public void run() {
				onError(supportRequest, throwable);
			}
		});
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