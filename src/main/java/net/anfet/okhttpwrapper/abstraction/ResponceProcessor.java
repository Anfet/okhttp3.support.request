package net.anfet.okhttpwrapper.abstraction;

import net.anfet.okhttpwrapper.SupportRequest;

import okhttp3.Response;

/**
 * Processor interface for {@link SupportRequest}
 */
public interface ResponceProcessor<T> {

	/**
	 * @param response ответ сервера
	 * @return тип данных сконвертированый из ответа
	 */
	T getResult(Response response) throws Exception;

}
