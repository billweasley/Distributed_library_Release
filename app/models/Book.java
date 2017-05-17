package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.avaje.ebean.Model;
import com.avaje.ebean.Model.Finder;

import play.data.validation.Constraints;

@Entity
public class Book extends Model {
	
	public static Finder<Long, Book> find = new Finder<Long, Book>(Book.class);
		
	@Column(name = "bid", unique = true, nullable = false)
	@Id
	@GeneratedValue
	public long id;
	
	@Column(unique = true, nullable = false)
	@Constraints.MaxLength(13)
	@Constraints.MinLength(13)
	public String isbn;
}
