package controllers;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import akka.event.Logging.Info;
import models.Book;
import play.libs.Json;
import play.libs.concurrent.HttpExecutionContext;
import play.libs.ws.WSClient;
import play.libs.ws.WSRequest;
import play.libs.ws.WSResponse;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.TokenAuthenticator;

public class BookController extends Controller {

	private final String GOOGLE_BOOK_URL_BASE = "https://www.googleapis.com/books/v1/volumes";
	private final String JSON = "application/json; charset=UTF-8";
	@Inject
	private WSClient ws;
	@Inject HttpExecutionContext ec;
	private JsonNode handleGoogleJson(WSResponse res) {
		JsonNode node = res.asJson();
		ObjectNode result = Json.newObject();
		JsonNode temp = node.findValue("totalItems");
		if (temp != null)
			result.set("numberOfResult", temp);
		temp = node.findValue("selfLink");
		if (temp != null)
			result.set("origin", temp);
		ArrayNode newList = Json.newObject().arrayNode();
		for (JsonNode item : node.withArray("items")) {
			ObjectNode newItem = Json.newObject();
			JsonNode volumeInfo = item.findValue("volumeInfo");
			for (JsonNode isbnInfo : volumeInfo.withArray("industryIdentifiers")) {
				if (isbnInfo != null)
					newItem.set(isbnInfo.findValue("type").asText(), isbnInfo.findValue("identifier"));
			}
			if ((temp = volumeInfo.findValue("title")) != null)
				newItem.set("title", temp);
			if ((temp = volumeInfo.findValue("authors")) != null)
				newItem.set("authors", temp);
			if ((temp = volumeInfo.findValue("publisher")) != null)
				newItem.set("publisher", temp);
			if ((temp = volumeInfo.findValue("publishedDate")) != null)
				newItem.set("publishedDate", temp);
			if ((temp = volumeInfo.findValue("description")) != null)
				newItem.set("description", temp);
			if ((temp = volumeInfo.findValue("category")) != null)
				newItem.set("category", temp);
			if ((temp = volumeInfo.findValue("thumbnail")) != null)
				newItem.set("thumbnail", temp);
			if ((temp = volumeInfo.findValue("language")) != null)
				newItem.set("language", temp);
			newList.add(newItem);
		}
		result.set("items", newList);
		return result;
	}

	@Security.Authenticated(TokenAuthenticator.class)
	public CompletionStage<Result> queryById(long bid, long uid) {
		return CompletableFuture.supplyAsync(() -> {
			Book book = Book.find.byId(bid);
			return (book != null && book.isbn != null) ? book.isbn : null;
		}).thenComposeAsync(isbn -> {
			return (isbn != null)
					? ws.url(GOOGLE_BOOK_URL_BASE).setContentType(JSON)
							.setQueryParameter("userip", request().remoteAddress())
							.setQueryParameter("printType", "books").setQueryParameter("q", "=isbn:" + isbn).get()
							.thenApplyAsync(res -> (res.getStatus() == 200)
									? ok(Json.prettyPrint(handleGoogleJson(res))).as(JSON)
									: notFound(Json.toJson("Error from google")),ec.current())
					: CompletableFuture.completedFuture(notFound(Json.toJson("Not found the book")));
		},ec.current());

	}

	@Security.Authenticated(TokenAuthenticator.class)
	public CompletionStage<Result> queryByName(String query, long uid, int limit, int startIndex) {
		if (limit > 40)
			limit = 40;
		WSRequest request = ws.url(GOOGLE_BOOK_URL_BASE).setContentType(JSON)
				.setQueryParameter("userip", request().remoteAddress()).setQueryParameter("printType", "books")
				.setQueryParameter("q", query).setQueryParameter("key","<To fill>").setQueryParameter("country","UK").setQueryParameter("startIndex", String.valueOf(startIndex));
					
		if (limit > 0)
			request.setQueryParameter("maxResults", String.valueOf(limit));

		return request.get().thenApplyAsync(res -> (res.getStatus() == 200)
				? ok(Json.prettyPrint(handleGoogleJson(res))).as(JSON) : notFound(Json.toJson("Error from google.Code:"+res.getStatus()+res.getBody())),ec.current());

	}
}
