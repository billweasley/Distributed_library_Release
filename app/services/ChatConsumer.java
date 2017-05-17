package services;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;

import akka.actor.*;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import play.libs.Json;
import play.mvc.WebSocket;

public class ChatConsumer extends UntypedActor {

	// private final ActorRef out;
	// private Channel channel;
	 //public static Props props = Props.create(ChatConsumer.class);
	private WebSocket.Out<Json> outStream;

	public ChatConsumer(WebSocket.Out<Json> outStream) {
		this.outStream = outStream;
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onReceive(Object message) throws Throwable {
		if (message instanceof Json)
			outStream.write((Json) message);
	}

}
