package models;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.avaje.ebean.Model;

public class Message extends Model {
	@Column(name = "mid", unique = true, nullable = false)
	@Id
	@GeneratedValue
	public long id;
	public long from;
	public long to;
	public String load;
	public boolean isDelivered;	
	public long serverTimestamp;
}
