package controllers;

import java.util.Date;
import java.util.regex.Pattern;
import java.util.List;
import javax.persistence.NonUniqueResultException;

import com.avaje.ebean.Ebean;
import com.avaje.ebean.Expr;
import com.avaje.ebean.ExpressionList;
import com.avaje.ebean.PagedList;
import com.avaje.ebean.text.json.JsonContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import akka.japi.Pair;
import models.Book;
import models.BookEntity;
import models.User;
import play.Logger;
import play.db.ebean.Transactional;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import services.GeolocationHelper;
import services.ReviewUpdateHelper;
import services.TokenAuthenticator;

public class BookEntityController extends Controller {

	private final String JSON = "application/json; charset=UTF-8";

	@Transactional // uid, token, isbn
	@Security.Authenticated(TokenAuthenticator.class)
	public Result postbook() {
		JsonNode json = request().body().asJson();

		JsonNode isbnJson = json.findValue("isbn");
		JsonNode uidJson = json.findValue("uid");
		if (isbnJson == null || uidJson == null) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		String isbnStr = isbnJson.asText();
		String uidStr = uidJson.asText();
		//Logger.info("isbn: " + isbnStr);
		//Logger.info("uidStr: " + uidStr);
		if (isbnStr == null || uidStr == null || uidStr.trim().isEmpty() || isbnStr.trim().isEmpty()
				|| isbnStr.length() != 13 || !Pattern.matches("97[89][0-9]{10}", isbnStr))
			return badRequest("Invalid Parameter").as(JSON);

		long uid = -1;

		try {
			uid = Long.parseLong(uidStr);
		} catch (NumberFormatException e) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		User owner = null;
		try {
			owner = User.findByID(uid);
		} catch (NullPointerException e) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		if (owner.inBlock) {
			return badRequest("The user is inblock.").as(JSON);
		}
		Book templete;
		try {
			templete = Book.find.setForUpdate(true).where().eq("isbn", isbnStr).findUnique();
		} catch (NonUniqueResultException e) {
			return internalServerError("Database inconsistent").as(JSON);
		} catch (NullPointerException ne) {
			return notFound("No book").as(JSON);
		}
		if (templete == null) {
			templete = new Book();
			templete.isbn = isbnStr;
			templete.save();
		}

		BookEntity newbookentity = new BookEntity();
		newbookentity.book = templete;
		newbookentity.owner = owner;
		// newbookentity.avaterLoc
		newbookentity.save();
		ObjectNode result = Json.newObject();
		result.put("status", "Successful").put("eid", newbookentity.id);
		return created(result);
	}

	// return single entity...
	@Security.Authenticated(TokenAuthenticator.class)
	public Result queryById(long eid, long uid) {

		BookEntity entity = BookEntity.find.select("id").fetch("book", "id,isbn").fetch("owner", "id,rating").where()
				.eq("id", eid).eq("status", 0).findUnique();
		return (entity == null) ? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(entity));
	}

	// return entity list...
	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result queryByBookID(long bid, long uid, int limit, int startIndex) {
		if (limit == -1)
			limit = 10;
		Book templete = Book.find.byId(bid);
		if (templete == null) {
			return notFound("No entity").as(JSON);
		}
		User querier = User.find.byId(uid);
		if (querier == null) {
			return notFound("No user").as(JSON);
		}
		PagedList<BookEntity> entities = BookEntity.find.select("id").fetch("book", "id,isbn")
				.fetch("owner", "id,rating,email").where().eq("book.id", bid).eq("status", 0).not(Expr.eq("owner.id", uid))
				.order("owner.rating desc,owner.ratingConfidence desc").findPagedList(startIndex, limit);
		List list = entities.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result queryByUserID(long uid, long target_uid, int limit, int startIndex) {
		if (limit == -1)
			limit = 10;
		User target = User.find.byId(target_uid);
		if (target == null) {
			return notFound("No entity").as(JSON);
		}
		PagedList<BookEntity> entities = BookEntity.find.select("id").fetch("book", "id,isbn")
				.fetch("owner", "id,rating,email").where().eq("status", 0).eq("owner.id", target_uid)
				.findPagedList(startIndex, limit);
		List<BookEntity> list = entities.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result queryByISBN(String isbn, long uid, int limit, int startIndex) {
		if (limit == -1)
			limit = 10;
		Book templete;
		try {
			templete = Book.find.where().eq("isbn", isbn).findUnique();
		} catch (NonUniqueResultException nue) {
			return internalServerError("Database inconsistent").as(JSON);
		}

		if (templete == null) {
			return notFound("No entity").as(JSON);
		}
		User querier = User.find.byId(uid);
		if (querier == null) {
			return notFound("No user").as(JSON);
		}

		PagedList<BookEntity> entities = BookEntity.find.select("id").fetch("book", "id,isbn")
				.fetch("owner", "name,email,rating").where().eq("book.id", templete.id).eq("status", 0)
				.not(Expr.eq("owner.id", uid)).order("owner.rating desc,owner.ratingConfidence desc")
				.findPagedList(startIndex, limit);
		List list = entities.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result queryByLocationAndBookID(long bid, long uid, int radius, int limit, int startIndex) {
		if (limit == -1)
			limit = 10;
		Book templete = Book.find.byId(bid);
		if (templete == null) {
			return notFound("No entity").as(JSON);
		}
		User querier = User.find.byId(uid);
		if (querier == null) {
			return notFound("No user").as(JSON);
		}

		if (querier.recentLatitude == -1 || querier.recentLongitude == -1) {
			return notFound("No GPS info").as(JSON);
		}

		Pair<Double, Double> latitudeRange = GeolocationHelper.getLatitudeRange(querier.recentLatitude, radius);
		Pair<Double, Double> longitudeRange = GeolocationHelper.getLongitudeRange(querier.recentLatitude,
				querier.recentLongitude, radius);
		PagedList<BookEntity> entities = BookEntity.find.select("id").fetch("book", "id,isbn")
				.fetch("owner", "name,email,id,rating,recentLatitude,recentLongitude").where().eq("book.id", bid).eq("status", 0)
				.not(Expr.eq("owner.id", uid))
				.between("owner.recentLatitude", latitudeRange.first(), latitudeRange.second())
				.between("owner.recentLongitude", longitudeRange.first(), longitudeRange.second())
				.order("owner.rating desc,owner.ratingConfidence desc").findPagedList(startIndex, limit);
		List list = entities.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result queryByLocationAndBookISBN(String isbn, long uid, int radius, int limit, int startIndex) {
		if (limit == -1)
			limit = 10;
		if (isbn == null || isbn.trim().isEmpty() || isbn.length() != 13 || !Pattern.matches("97[89][0-9]{10}", isbn)) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		Book templete;
		try {
			templete = Book.find.where().eq("isbn", isbn).findUnique();
		} catch (NonUniqueResultException e) {
			return internalServerError("Database inconsistent").as(JSON);
		} catch (NullPointerException ne) {
			return notFound("No book").as(JSON);
		}
		if (templete == null) {
			return notFound("No entity").as(JSON);
		}
		User querier = User.find.byId(uid);
		if (querier == null) {
			return notFound("No user").as(JSON);
		}

		if (querier.recentLatitude == -1 || querier.recentLongitude == -1) {
			return notFound("No GPS info").as(JSON);
		}

		Pair<Double, Double> latitudeRange = GeolocationHelper.getLatitudeRange(querier.recentLatitude, radius);
		Pair<Double, Double> longitudeRange = GeolocationHelper.getLongitudeRange(querier.recentLatitude,
				querier.recentLongitude, radius);
		PagedList<BookEntity> entities = BookEntity.find.select("id").fetch("book", "id,isbn")
				.fetch("owner", "name,email,id,rating,recentLatitude,recentLongitude").where().eq("book.id", templete.id)
				.eq("status", 0).not(Expr.eq("owner.id", uid))
				.between("owner.recentLatitude", latitudeRange.first(), latitudeRange.second())
				.between("owner.recentLongitude", longitudeRange.first(), longitudeRange.second())
				.order("owner.rating desc,owner.ratingConfidence desc").findPagedList(startIndex, limit);
		List list = entities.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result getRequstingBooks(Long uid, int limit, int startIndex){
		if (limit == -1)
			limit = 10;
		PagedList<BookEntity> requestingBookList = BookEntity.find.select("id")
				.fetch("book","id,isbn")
				.fetch("owner", "name,email,id,rating,recentLatitude,recentLongitude")
				.where().eq("possibleNextOwner.id", uid).eq("status", 1).order("requestTime desc").findPagedList(startIndex, limit);
		List list = requestingBookList.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result getPostingBooks(Long uid, int limit, int startIndex){
		if (limit == -1)
			limit = 10;
		PagedList<BookEntity> requestingBookList = BookEntity.find.select("id")
				.fetch("book","id,isbn,status")
				.where().disjunction()
							.conjunction()
							.eq("owner.id", uid).eq("status", 1)
							.endJunction()
							.conjunction()
							.eq("owner.id", uid).eq("status", 0)
							.endJunction()
						.endJunction().order("id desc").findPagedList(startIndex, limit);
		List list = requestingBookList.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result getNewlyComingBooks(Long uid){
		PagedList<BookEntity> requestingBookList = BookEntity.find.select("id")
				.fetch("book","id,isbn")
				.where().eq("status", 0)
				.order("id desc").findPagedList(0, 10);
		List list = requestingBookList.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result getBeingRequestedingBooks(Long uid, int limit, int startIndex){
		if (limit == -1)
			limit = 10;
		PagedList<BookEntity> requestingBookList = BookEntity.find.select("id")
				.fetch("book","id,isbn,status")
				.where().eq("owner.id", uid).eq("status", 1)
				.order("id desc").findPagedList(startIndex, limit);
		List list = requestingBookList.getList();
		return (list.isEmpty())? notFound("No entity").as(JSON) : ok(Ebean.json().toJson(list));
	}


	// Required: uid,token,eid
	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result requestBook() {
		JsonNode json = request().body().asJson();
		String eidStr = json.findPath("eid").asText();
		String uidStr = json.findPath("uid").asText();
		if (uidStr == null || eidStr == null || uidStr.trim().isEmpty() || eidStr.trim().isEmpty())
			return badRequest("Invalid Parameter").as(JSON);
		long eid = -1;
		long uid = -1;
		BookEntity toborrow;
		User borrower;
		try {
			uid = Long.parseLong(uidStr);
			eid = Long.parseLong(eidStr);
		} catch (NumberFormatException e) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		try {
			borrower = User.find.setForUpdate(true).where().eq("id", uid).findUnique();
		} catch (NonUniqueResultException e) {
			return internalServerError("Database inconsistent").as(JSON);
		} catch (NullPointerException ne) {
			return notFound("No user").as(JSON);
		}
		if (borrower == null) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		if (borrower.credit <= 0) {
			return badRequest("No credit").as(JSON);
		}
		if (borrower.inBlock) {
			return badRequest("The user is inblock.").as(JSON);
		}

		try {
			toborrow = BookEntity.find.setForUpdate(true).where().eq("id", eid).findUnique();
		} catch (NonUniqueResultException e) {
			return internalServerError("Database inconsistent").as(JSON);
		} catch (NullPointerException ne) {
			return notFound("No user").as(JSON);
		}
		if (toborrow == null || toborrow.status > 0 || toborrow.owner.id==borrower.id) {
			return notFound("Not available").as(JSON);
		} else {
			if (toborrow.status == 0) {
				borrower.credit--;
				borrower.save();
				toborrow.status = 1;
				toborrow.possibleNextOwner = borrower;
				toborrow.requestTime = new Date();
				toborrow.save();
				ObjectNode result = Json.newObject();
				result.put("status", "Successful").put("entity_id", toborrow.id).put("borrower_id", borrower.id);
				return created(result);
			} else {
				return notFound("Not available").as(JSON);
			}
		}

	}

	// Required: uid,token,eid
	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result cancelRequest() {
		JsonNode json = request().body().asJson();
		String eidStr = json.findPath("eid").asText();
		String uidStr = json.findPath("uid").asText();
		if (uidStr == null || eidStr == null || uidStr.trim().isEmpty() || eidStr.trim().isEmpty())
			return badRequest("Invalid Parameter").as(JSON);
		long eid = -1;
		long uid = -1;
		BookEntity tocancel;
		User borrower;
		try {
			uid = Long.parseLong(uidStr);
			eid = Long.parseLong(eidStr);
		} catch (NumberFormatException e) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		try {
			borrower = User.find.setForUpdate(true).where().eq("id", uid).findUnique();
		} catch (NonUniqueResultException e) {
			return internalServerError("Database inconsistent").as(JSON);
		} catch (NullPointerException ne) {
			return notFound("No user").as(JSON);
		}
		if (borrower == null) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		if (borrower.inBlock) {
			return badRequest("The user is inblock.").as(JSON);
		}

		try {
			tocancel = BookEntity.find.setForUpdate(true).where().eq("id", eid).findUnique();
		} catch (NonUniqueResultException e) {
			return internalServerError("Database inconsistent").as(JSON);
		} catch (NullPointerException ne) {
			return notFound("No user").as(JSON);
		}
		if (tocancel == null || tocancel.status != 1 || !(tocancel.possibleNextOwner.id==borrower.id||tocancel.owner.id==borrower.id)) {
			return notFound("Not available").as(JSON);
		} else {
			if (tocancel.status == 1) {
				if(tocancel.possibleNextOwner.id==borrower.id){
					borrower.credit++;
				}
				tocancel.possibleNextOwner = null;
				tocancel.requestTime = null;
				tocancel.status = 0;
				borrower.save();
				tocancel.save();
				ObjectNode result = Json.newObject();
				result.put("status", "Successful").put("entity_id", tocancel.id).put("borrower_id", borrower.id);
				return created(result);
			} else {
				return notFound("Internal error").as(JSON);
			}
		}
	}

	// Required: uid(borrower), token, eid, review
	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result endRequest() {
		JsonNode json = request().body().asJson();
		String eidStr = json.findPath("eid").asText();
		String uidStr = json.findPath("uid").asText();
		String reviewStr = json.findPath("review").asText();
		if (uidStr == null || eidStr == null || uidStr.trim().isEmpty() || eidStr.trim().isEmpty())
			return badRequest("Invalid Parameter").as(JSON);
		long review = 5;
		long eid = -1;
		long uid = -1;
		if (reviewStr != null && !reviewStr.trim().isEmpty()) {
			try {
				int temp = Integer.parseInt(reviewStr);
				if (temp >= 0 && temp < 5) {
					review = temp;
				}
			} catch (NumberFormatException nfe) {
			}
		}
		BookEntity toconfirm;
		User borrower;
		try {
			uid = Long.parseLong(uidStr);
			eid = Long.parseLong(eidStr);
		} catch (NumberFormatException e) {
			return badRequest("Invalid Parameter").as(JSON);
		}
		try {
			borrower = User.find.setForUpdate(true).where().eq("id", uid).findUnique();
		} catch (NonUniqueResultException e) {
			return internalServerError("Database inconsistent").as(JSON);
		} catch (NullPointerException ne) {
			return notFound("No user").as(JSON);
		}
		if (borrower == null) {
			return badRequest("Invalid Parameter").as(JSON);
		}

		if (borrower.inBlock) {
			return badRequest("The user is inblock.").as(JSON);
		}
		try {
			toconfirm = BookEntity.find.setForUpdate(true).where().eq("id", eid).findUnique();
		} catch (NonUniqueResultException e) {
			return internalServerError("Database inconsistent").as(JSON);
		} catch (NullPointerException ne) {
			return notFound("No user").as(JSON);
		}
		User originalOwner = toconfirm.owner;

		if (toconfirm == null || toconfirm.status != 1 || toconfirm.possibleNextOwner.id!=borrower.id) {
			return notFound("Not available").as(JSON);
		} else {
			if (toconfirm.status == 1) {
				toconfirm.possibleNextOwner = null;
				toconfirm.requestTime = null;
				toconfirm.owner = borrower;
				toconfirm.status = 2;
				toconfirm.save();
				originalOwner.credit++;
				originalOwner.totalNumOfLent++;
				originalOwner.rating = ReviewUpdateHelper.updateReview(originalOwner.rating, review,
						originalOwner.totalNumOfLent);
				if (review > 3)
					originalOwner.totalNumOfPraise++;
				originalOwner.ratingConfidence = ReviewUpdateHelper
						.updateWilsonConfidence(originalOwner.totalNumOfPraise, originalOwner.totalNumOfLent);
				originalOwner.save();
				ObjectNode result = Json.newObject();
				result.put("status", "Successful").put("entity_id", toconfirm.id).put("borrower_id", borrower.id);
				return created(result);
			} else {
				return notFound("Internal error").as(JSON);
			}
		}
	}

}
