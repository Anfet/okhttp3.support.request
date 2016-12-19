package net.anfet.okhttpwrapper;

import net.anfet.okhttpwrapper.abstractions.IRequestListener;
import net.anfet.okhttpwrapper.abstractions.ResponceProcessor;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Дженерик для любого типа ответа обрабатываюшийся в потоке
 */
public class ThreadListener<T> implements IRequestListener<T> {

	protected ResponceProcessor<T> processor;

	public ThreadListener(ResponceProcessor<T> processor) {
		this.processor = processor;
	}

	public ThreadListener() {
		this(null);
	}


	@Override
	public void publishPreExecute(SupportRequest supportRequest, Request request) throws Exception {
		onPreExecute(supportRequest, request);
	}

	@Override
	public T publishProcessResult(SupportRequest supportRequest, Request request, Response response) throws Exception {
		return onProcessResult(supportRequest, request, response);
	}

	@Override
	public void publishPostProcess(SupportRequest supportRequest, T t) {
		onPostProcess(supportRequest, t);
	}

	@Override
	public void publishError(SupportRequest supportRequest, Throwable throwable) {
		onError(supportRequest, throwable);
	}

	protected void onError(SupportRequest supportRequest, Throwable throwable) {

	}

	@Override
	public void publishComplete(SupportRequest supportRequest) {
		onComplete(supportRequest);
	}

	protected void onComplete(SupportRequest supportRequest) {

	}

	protected void onPreExecute(SupportRequest supportRequest, Request request) throws Exception {

	}

	protected T onProcessResult(SupportRequest supportRequest, Request request, Response response) throws Exception {
		if (processor != null) return processor.getResult(response);
		return null;
	}

	protected void onPostProcess(SupportRequest supportRequest, T t) {

	}
}
