package net.anfet.okhttpwrapper;

import android.util.Log;

import net.anfet.okhttpwrapper.abstraction.ResponceProcessor;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Дженерик для любого типа ответа обрабатываюшийся в потоке
 */
public class WorkerThreadListener<T> {

	protected ResponceProcessor<T> processor;

	public WorkerThreadListener(ResponceProcessor<T> processor) {
		this.processor = processor;
	}


	/**
	 * Вызывается для процессинга результата
	 * @param supportRequest запрос
	 * @param response       ответ
	 * @param t              результат
	 */
	protected void onProcessResult(final SupportRequest supportRequest, final Response response, final T t) throws Exception {

	}


	T publishResponce(final SupportRequest supportRequest, final Response response) throws Exception {
		T result = null;
		try {
			if (processor != null) {
				result = processor.getResult(response);
			}

			onProcessResult(supportRequest, response, result);
		} catch (InterruptedException ignored) {
			//this is ignored
		}
		return result;
	}

	/**
	 * вызывается после {@link #publishError(SupportRequest, Throwable)}
	 * @param supportRequest запрос
	 * @param error          ошибка
	 */
	protected void onError(final SupportRequest supportRequest, final Throwable error) {
		Log.e(getClass().getSimpleName(), error.getMessage(), error);
	}


	void publishError(final SupportRequest supportRequest, final Throwable throwable) {
		onError(supportRequest, throwable);
	}

	/**
	 * Вызывается после получения нужного ответа
	 * @param request  запрос
	 * @param response ответ
	 * @param t        результат обработки
	 */
	protected void onPostProcess(final SupportRequest request, final Response response, final T t) {

	}

	/**
	 * Вызывается всегде после окончания обработки запроса. Вызывается один раз
	 * @param request запрос
	 */
	protected void onComplete(final SupportRequest request) {

	}

	void publishPreExecute(final SupportRequest supportRequest, final Request request) throws Exception {
		onPreExecute(supportRequest, request);
	}

	protected void onPreExecute(final SupportRequest supportRequest, final Request request) throws Exception {

	}

	void publishComplete(final SupportRequest supportRequest) {
		onComplete(supportRequest);
	}

	void publishPostProcess(final SupportRequest supportRequest, final Response response, final T t) {
		onPostProcess(supportRequest, response, t);
	}
}
