package controllers;

import akka.NotUsed;
import akka.actor.ActorSystem;
import akka.event.LoggingAdapter;
import akka.japi.Pair;
import akka.japi.pf.PFBuilder;
import akka.stream.Materializer;
import akka.stream.javadsl.*;
import models.User;
import play.Logger;
import play.api.libs.streams.ActorFlow;
import play.libs.F;
import play.libs.Json;
import play.mvc.*;
import services.*;
import akka.event.Logging;
import akka.actor.*;
import akka.stream.*;
import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.*;

public class ChatController extends Controller {

	//@Security.Authenticated(TokenAuthenticator.class)
	public Result index(long uid, long otherSide) {
		Http.Request request = request();
		String url = routes.ChatController.socket(uid, otherSide).webSocketURL(request);
		return Results.ok(views.html.chat.render(url));
	}
	//@Security.Authenticated(TokenAuthenticator.class)
	@SuppressWarnings("deprecation")
	public LegacyWebSocket<String> socket(long uid, long otherSid) {
		//if (uid != otherSid && checkStatusBefore(otherSid)) {
			return WebSocket.whenReady((in, out) ->
				{
					try {
						Pair<Channel, Channel> channels = preConfigRabbitMQChannel(uid, otherSid);
						in.onMessage(load ->
							{
								//String load = inputedJson.get("msg").asText();
								Logger.info(load);
								try {
									byte[] loadBytes = load.getBytes("UTF-8");
									if (load != null && !load.trim().isEmpty()) {
										channels.second().basicPublish("MSG_EXCHANGE" + otherSid, "MESSAGE",
												MessageProperties.PERSISTENT_BASIC,
												loadBytes);
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							});
						channels.first().basicConsume("MSG_QUEUE" + uid,
								new com.rabbitmq.client.DefaultConsumer(channels.first()) {
									@Override
									public void handleDelivery(String consumerTag, Envelope envelope,
											AMQP.BasicProperties properties, byte[] body) throws IOException {
										long deliveryTag = envelope.getDeliveryTag();
									//	JsonNode node = Json.parse(new String(body, "UTF-8"));
										//String from = node.get("from").asText();
										//String to = node.get("to").asText();
									//	long fromIndex = -1;
									//	long toIndex = -1;
									//	try {
									//		fromIndex = Long.parseLong(from);
									//		toIndex = Long.parseLong(to);
									//	} catch (NumberFormatException nfe) {
									//		Logger.info("Error when parse json");
									//	}

									//	if (toIndex == uid && fromIndex == otherSid) {
											try {
												out.write(new String(body,"UTF-8"));
												channels.first().basicAck(deliveryTag, false);
											} catch (Exception e) {
												channels.first().basicNack(deliveryTag, false, true);
												throw e;
											}
									//	} else {
									//		channels.first().basicNack(deliveryTag, false, true);
									//	}

									}

								});
					} catch (IOException|TimeoutException e) {
						e.printStackTrace();
					}
				});

		//} else {
		//	return WebSocket.reject(notFound());
		//}

	}
/*	//@Security.Authenticated(TokenAuthenticator.class)
	@SuppressWarnings("deprecation")
	public LegacyWebSocket<JsonNode> socket(long uid, long otherSid) {
		//if (uid != otherSid && checkStatusBefore(otherSid)) {
			return WebSocket.whenReady((in, out) ->
				{
					try {
						Pair<Channel, Channel> channels = preConfigRabbitMQChannel(uid, otherSid);
						in.onMessage(inputedJson ->
							{
								String load = inputedJson.get("msg").asText();
								Logger.info(load);
								try {
									if (load != null && !load.trim().isEmpty()) {
										channels.second().basicPublish("MSG_EXCHANGE" + otherSid, "MESSAGE",
												MessageProperties.PERSISTENT_BASIC,
												inputedJson.toString().getBytes("UTF-8"));
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							});
						channels.first().basicConsume("MSG_QUEUE" + uid,
								new com.rabbitmq.client.DefaultConsumer(channels.first()) {
									@Override
									public void handleDelivery(String consumerTag, Envelope envelope,
											AMQP.BasicProperties properties, byte[] body) throws IOException {
										long deliveryTag = envelope.getDeliveryTag();
										JsonNode node = Json.parse(new String(body, "UTF-8"));
										String from = node.get("from").asText();
										String to = node.get("to").asText();
										long fromIndex = -1;
										long toIndex = -1;
										try {
											fromIndex = Long.parseLong(from);
											toIndex = Long.parseLong(to);
										} catch (NumberFormatException nfe) {
											Logger.info("Error when parse json");
										}

										if (toIndex == uid && fromIndex == otherSid) {
											try {
												out.write(node);
												channels.first().basicAck(deliveryTag, false);
											} catch (Exception e) {
												channels.first().basicNack(deliveryTag, false, true);
												throw e;
											}
										} else {
											channels.first().basicNack(deliveryTag, false, true);
										}

									}

								});
					} catch (IOException|TimeoutException e) {
						e.printStackTrace();
					}
				});

		//} else {
		//	return WebSocket.reject(notFound());
		//}

	}*/

	public boolean checkStatusBefore(long otherSide) {
		User user = User.findByID(otherSide);
		return (user == null) ? false : true;
	}

	private Pair<Channel, Channel> preConfigRabbitMQChannel(long uid, long otherSid) throws IOException,TimeoutException {

		Connection connection = RabbitMQConnection.getConnection();
		Channel targetChannel = connection.createChannel();
		Channel myChannel = connection.createChannel();
		if (targetChannel != null) {
			targetChannel.exchangeDeclare("MSG_EXCHANGE" + otherSid, "direct", true, false, false, null);
			targetChannel.queueDeclare("MSG_QUEUE" + otherSid, true, false, false, null);
			targetChannel.queueBind("MSG_QUEUE" + otherSid, "MSG_EXCHANGE" + otherSid, "MESSAGE");
		}
		if (myChannel != null) {
			myChannel.exchangeDeclare("MSG_EXCHANGE" + uid, "direct", true, false, false, null);
			myChannel.queueDeclare("MSG_QUEUE" + uid, true, false, false, null);
			myChannel.queueBind("MSG_QUEUE" + uid, "MSG_EXCHANGE" + uid, "MESSAGE");
		}
		return Pair.create(myChannel, targetChannel);
	}

	/**
	 * Checks that the WebSocket comes from the same origin. This is necessary
	 * to protect against Cross-Site WebSocket Hijacking as WebSocket does not
	 * implement Same Origin Policy.
	 *
	 * See https://tools.ietf.org/html/rfc6455#section-1.3 and
	 * http://blog.dewhurstsecurity.com/2013/08/30/security-testing-html5-websockets.html
	 */
	private boolean sameOriginCheck(Http.RequestHeader request) {
		String[] origins = request.headers().get("Origin");
		if (origins.length > 1) {
			// more than one origin found
			return false;
		}
		String origin = origins[0];
		return originMatches(origin);
	}

	private boolean originMatches(String origin) {
		if (origin == null)
			return false;
		try {
			URL url = new URL(origin);
			return url.getHost().equals("localhost") && (url.getPort() == 9000 || url.getPort() == 19001);
		} catch (Exception e) {
			return false;
		}
	}

}
