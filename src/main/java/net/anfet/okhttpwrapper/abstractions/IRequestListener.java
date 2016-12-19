package net.anfet.okhttpwrapper.abstractions;

import net.anfet.okhttpwrapper.SupportRequest;

import okhttp3.Request;
import okhttp3.Response;

/**
 * Базовый класс приемки любого ответа
 */
public interface IRequestListener<T> {

	void publishPreExecute(SupportRequest supportRequest, Request request) throws Exception;

	T publishProcessResult(SupportRequest supportRequest, Request request, Response response) throws Exception;

	void publishPostProcess(SupportRequest supportRequest, T t);

	void publishError(SupportRequest supportRequest, Throwable throwable);

	void publishComplete(SupportRequest supportRequest);
}
