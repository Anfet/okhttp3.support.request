package net.anfet.okhttpwrapper.processors;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import net.anfet.okhttpwrapper.ResponceProcessor;

import okhttp3.Response;

/**
 * Процессор для получения JSON ответов с сервера
 */

public class GsonElementProcessor implements ResponceProcessor<JsonElement> {

	private final GsonBuilder builder;

	public GsonElementProcessor(GsonBuilder builder) {
		this.builder = builder;
		if (this.builder == null) {
			throw new AssertionError("Null GsonBuilder");
		}
	}

	public GsonElementProcessor() {
		this(new GsonBuilder());
	}

	@Override
	public JsonElement getResult(Response response) throws Exception {
		return builder.create().fromJson(response.body().charStream(), JsonElement.class);
	}
}
