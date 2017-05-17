package models;

//import java.math.BigDecimal;
import java.util.*;
import javax.persistence.*;

import com.avaje.ebean.Model;
import com.avaje.ebean.annotation.DbJson;
import com.fasterxml.jackson.annotation.JsonBackReference;

import play.data.format.*;
import play.data.validation.*;

@Entity
public class User extends Model {

	public static Finder<Long, User> find = new Finder<Long, User>(User.class);

	public static List<User> findAll() {
		return find.all();
	}
	
	public static List<User> findByEmail(String email) {
		return find.where().eq("email", email.toLowerCase()).findList();
	}
	public static User findByID(long id) {
		return find.byId(id);
	}
	public static boolean isExistingAccount(String email) {
		return findByEmail(email.toLowerCase()).size() != 0;
	}

	@Column(name = "uid", unique = true, nullable = false)
	@Id
	@GeneratedValue
	public long id;

	@Column(nullable = false)
	@Constraints.Pattern(value = "\\b[a-zA-Z0-9_\\$#\\-]+\\b", message = "Only characters, numbers and _ - $ # are allowed.")
	@Constraints.Required(message = "Name field is required.")
	@Formats.NonEmpty
	public String name;

	@Column(unique = true, nullable = false)
	@Constraints.Pattern(value = "\\b[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\.[aA][cC]\\.[uU][kK]\\b", message = "It should in email format end up with \".ac.uk\".")
	@Constraints.Required(message = "A university email ending up with .ac.uk is required.")
	@Formats.NonEmpty
	public String email;

	@Column(nullable = false, columnDefinition = "tinyint(1) default 0")
	public boolean isVaildEmail;
	
	@Column(length = 1024)
	public String avaterLoc;

	@Column(name = "psd", nullable = false)
	@Constraints.MinLength(8)
	@Constraints.MaxLength(255)
	@Constraints.Required(message = "The field is required.")
	@Formats.NonEmpty
	public String password;

	@Column(name = "dob", nullable = false)
	@Formats.NonEmpty
	@Formats.DateTime(pattern = "dd/MM/yyyy")
	public Date dateOfBirth;

	@Column(name = "dor", nullable = false)
	@Formats.DateTime(pattern = "dd/MM/yyyy")
	@Formats.NonEmpty
	public Date dateOfRegistration;
	
	@Column(nullable = false, columnDefinition = "int default 0")
	public int credit;
	
	@Column(nullable = false, columnDefinition = "DECIMAL(3,2) default 3.00")
	@Constraints.Min(0)
	@Constraints.Max(5)
	@Formats.NonEmpty
	public double rating;
	
	@Column(precision = 2, nullable = false, columnDefinition = "DECIMAL(3,2) default 0.00")
	public double ratingConfidence;

	@Column(name = "total_lent", nullable = false, columnDefinition = "bigint default 0")
	public long totalNumOfLent;
	
	@Column(nullable = false, columnDefinition = "bigint default 0")
	public long totalNumOfPraise;
	
	@Column(nullable = false, columnDefinition = "tinyint(1) default 0")
	public boolean inBlock;
	
	@Column(nullable = false)
	public double recentLatitude;
	
	@Column(nullable = false)
	public double recentLongitude;
	
	public String recentLocation;
	
	@Column(name = "recent_ipv4", nullable = false)
	public String recentIP;

	@Column(name = "allow_gps", nullable = false, columnDefinition = "tinyint(1) default 1")
	public boolean allowGPS;
	
	@DbJson
	public Map<String, Object> lastLoginRec;
	
	public String uuid;
	
	public long uuidValidUntil;
	public String pswResetuuid;
	public long pswResetUuidValidUntil;
	public long randomCode;
	
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "owner") @JsonBackReference
	public List<BookEntity> ownedBooks;
}
