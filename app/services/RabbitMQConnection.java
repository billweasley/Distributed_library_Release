package services;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.google.inject.Singleton;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.typesafe.config.ConfigFactory;
@Singleton
public class RabbitMQConnection {

	private volatile static Connection CONNECTION = null;
	private volatile static ConnectionFactory factory = new ConnectionFactory();
	private static String RABBITMQ_HOST = ConfigFactory.load().getString("rabbitmq.host");
	//private static int RABBITMQ_PORT = ConfigFactory.load().getInt("rabbitmq.port");
	public static Connection getConnection() throws IOException, TimeoutException{
		factory.setHost(RABBITMQ_HOST);
		//actory.setPort(RABBITMQ_PORT);
		factory.setAutomaticRecoveryEnabled(true);
		factory.setTopologyRecoveryEnabled(true);
		if (CONNECTION == null){
			if (CONNECTION == null){
				return factory.newConnection();
			}else{
				return CONNECTION;
			}

		}
		return CONNECTION;
	}

}
