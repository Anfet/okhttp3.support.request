package net.anfet.okhttpwrapper;

import okhttp3.Response;

/**
 * Процессор отвечающий за перевод данных в строку
 */
public class StringProcessor implements ResponceProcessor<String> {
	@Override
	public String getResult(Response response) throws Exception {
		try {
			return response.body().string();
		} finally {
			response.close();
		}
	}

}
