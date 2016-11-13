package net.anfet.okhttpwrapper;

import okhttp3.Response;

/**
 * Базовый класс приемки любого ответа
 */
public interface ISupportRequestListener<T> {

	/**
	 * Обрабатывает получение результата запроса. Это ответ от сервера, но он не обязательно должен быть положительным с точки зрения клиента
	 * @param supportRequest запрос
	 * @param response       ответ
	 */
	void publishResponce(SupportRequest supportRequest, Response response);

	/**
	 * Вызывается при ошибке запроса. Это может быть как {@link java.io.IOException} который укажет на отсутствие связи, так и любая другая ошибка от {@link #publishResponce(SupportRequest, Response)} (SupportRequest, Response)}
	 * @param supportRequest запрос
	 * @param throwable      ошибка
	 */
	void publishError(SupportRequest supportRequest, Throwable throwable);
}
