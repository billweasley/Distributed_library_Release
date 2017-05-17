package services;

import java.util.*;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.typesafe.config.ConfigFactory;


public class SelfMailSender  {
	
	public void send(String groupName,String from, String to, String title, String content){
		String user = ConfigFactory.load().getString("selfmail."+groupName+".user");
		String psw = ConfigFactory.load().getString("selfmail."+groupName+".password");
		send(user,psw,from,to,title,content);
	}
	
	
	public void send(String user,String pwd,String from, String to, String title, String content) {
	    Properties properties = new Properties();
	    properties.put("mail.smtp.ssl.enable", "true");
	    properties.put("mail.smtp.host", "email-smtp.eu-west-1.amazonaws.com");
	    properties.put("mail.smtp.port", "587");
	    properties.put("mail.smtp.auth", "true");
	    Session session = Session.getDefaultInstance(properties, new javax.mail.Authenticator() {
	        @Override
			protected PasswordAuthentication getPasswordAuthentication() {
	            return new PasswordAuthentication(user, pwd);
	        }
	    });

	    MimeMessage message = new MimeMessage(session);
	    try {
			message.setFrom(new InternetAddress(from));
		    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
		    message.setSubject(title);
		    message.setText(content);
		    Transport.send(message);
		} catch (MessagingException e) {
			e.printStackTrace();
		}

	}


}
