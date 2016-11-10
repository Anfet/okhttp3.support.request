package net.anfet.okhttpwrapper;

import okhttp3.Response;

/**
 * Интерфес процессора данных для {@link SupportRequest}.
 */
public interface ResponceProcessor<T> {

	/**
	 * @param response ответ сервера
	 * @return тип данных сконвертированый из ответа
	 */
	T getResult(Response response) throws Exception;

}
