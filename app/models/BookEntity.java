package models;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;
import com.fasterxml.jackson.annotation.JsonBackReference;

import play.data.validation.Constraints;

@Entity
public class BookEntity extends Model {
	
	public static Finder<Long, BookEntity> find = new Finder<Long, BookEntity>(BookEntity.class);
	
	@Column(name = "eid", unique = true, nullable = false)
	@Constraints.Min(0)
	@Id
	@GeneratedValue
	public long id;
	
	@Column(length = 1024)
	public String avaterLoc;
	
	@ManyToOne(optional=false)
	@JoinColumn(name="bid", nullable = false)
	public Book book;
	
	@ManyToOne(optional=false)
	@JoinColumn(name="uid", nullable = false)
	public User owner;
	
	@Column(nullable = false,columnDefinition = "tinyint(1) default 0")
	@Constraints.Max(2)
	@Constraints.Min(0)
	public int status;
	
	@ManyToOne(optional=true)
	@JoinColumn(nullable = true,columnDefinition = "default null")
	public User possibleNextOwner;
	
	public Date requestTime;
}
