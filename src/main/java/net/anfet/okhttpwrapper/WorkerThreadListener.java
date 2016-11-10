package net.anfet.okhttpwrapper;

import android.util.Log;

import okhttp3.Response;

/**
 * Дженерик для любого типа ответа обрабатываюшийся в потоке
 */
public class WorkerThreadListener<T> implements ISupportRequestListener<T> {

	protected ResponceProcessor<T> processor;

	public WorkerThreadListener(ResponceProcessor<T> processor) {
		this.processor = processor;
	}


	protected T processResult(SupportRequest supportRequest, Response response) throws Exception {
		T result = null;
		if (processor != null) {
			result = processor.getResult(response);
		}
		onProcessResult(supportRequest, response, result);
		return result;
	}

	/**
	 * Вызывается для процессинга результата
	 * @param supportRequest запрос
	 * @param response       ответ
	 * @param t              результат
	 */
	protected void onProcessResult(SupportRequest supportRequest, Response response, T t) {

	}

	@Override
	public void publishResponce(SupportRequest supportRequest, Response response) {
		try {
			onPostProcess(supportRequest, response, processResult(supportRequest, response));
			onComplete(supportRequest);
		} catch (InterruptedException ex) {
			return;
		} catch (Exception ex) {
			publishError(supportRequest, ex);
		}
	}

	/**
	 * вызывается после {@link #publishError(SupportRequest, Throwable)}
	 * @param supportRequest запрос
	 * @param error          ошибка
	 */
	protected void onError(SupportRequest supportRequest, Throwable error) {
		Log.e(getClass().getSimpleName(), error.getMessage(), error);
		onComplete(supportRequest);
	}

	@Override
	public void publishError(SupportRequest supportRequest, Throwable throwable) {
		onError(supportRequest, throwable);
	}

	/**
	 * Вызывается после получения нужного ответа
	 * @param request  запрос
	 * @param response ответ
	 * @param t        результат обработки
	 */
	protected void onPostProcess(SupportRequest request, Response response, T t) {

	}

	/**
	 * Вызывается всегде после окончания обработки запроса. Вызывается один раз
	 * @param request запрос
	 */
	protected void onComplete(SupportRequest request) {

	}
}
