package controllers;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;
import java.text.SimpleDateFormat;
import javax.persistence.PersistenceException;

import com.google.inject.Inject;

import models.User;
import play.libs.Json;
import play.libs.concurrent.HttpExecution;
import play.libs.mailer.Email;
import play.libs.mailer.MailerClient;
import play.libs.ws.WSClient;
import play.data.DynamicForm;
import play.data.Form;
import play.data.FormFactory;
import play.db.ebean.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import services.CipherHelper;
import services.TokenAuthenticator;
import views.html.*;

import com.avaje.ebean.Transaction;
import com.edulify.modules.geolocation.GeolocationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.Logger;
import play.api.mvc.Request;
import play.mvc.Security;
import scala.collection.mutable.Map;
import play.mvc.BodyParser;

public class UserController extends Controller {

	@Inject
	private FormFactory formFactory;
	@Inject
	private GeolocationService geolocationService;
	@Inject
	private MailerClient mailerClient;
	// a day represented in Unix timestamp (millionsecond)
	private final long validInternal = 24 * 60 * 60 * 1000;
	private final long pswResetValidInternal = 60 * 60 * 1000;
	// 2 hours represented in Unix timestamp (millionsecond)
	private final long increasedInternal = 2 * 60 * 60 * 1000;
	private final String JSON = "application/json; charset=UTF-8";
	private final String[] userVisiableNames = { "email", "name", "dateOfBirth", "dateOfRegistration", "rating",
			"credit", "inBlock", "totalNumOfLent", "recentLocation", "allowGPS" };
	private final String[] userUpdatableNames = { "name", "password", "allowGPS" };

	@Transactional
	private String produceToken(User user, Date timestamp, boolean isIncrease)
			throws IllegalArgumentException, UnsupportedEncodingException {
		if (!User.isExistingAccount(user.email) || !user.isVaildEmail)
			throw new IllegalArgumentException();
		int salt = new Random().nextInt(90000) + 10000;
		long newVaildtime = timestamp.getTime() + (isIncrease ? increasedInternal : validInternal);
		String rawToken = String.valueOf(newVaildtime) + String.valueOf(salt) + String.valueOf(user.id);
		user.uuid = CipherHelper.encryptBCrypt(rawToken);
		user.randomCode = salt;
		user.uuidValidUntil = newVaildtime;
		user.save();
		return CipherHelper.encryptAES(rawToken);
	}

/*	public Result showCreateUser() {
		//Form<User> initialForm = formFactory.form(User.class);
		//return ok(registration_form.render(initialForm));
		return ok(signUp.render());
	}*/


	@Security.Authenticated(TokenAuthenticator.class)
	public Result checkToken(long uid){
		return ok();
	}

	@Transactional
	@BodyParser.Of(BodyParser.Json.class)
	public Result createUser() {
		// Form<User> filledForm =
		// formFactory.form(User.class).bindFromRequest();
		JsonNode filledForm = request().body().asJson();
		if (filledForm == null) {
			return badRequest(Json.toJson("Expecting Json data"));
		}

		String name = filledForm.findPath("name").textValue();
		String email = filledForm.findPath("email").textValue();
		String confirmEmail = filledForm.findPath("confirm_email").textValue();
		String password = filledForm.findPath("password").textValue();
		String confirmPassword = filledForm.findPath("confirm_password").textValue();

		if (name == null || email == null || confirmEmail == null || password == null || confirmPassword == null) {
			return badRequest(Json.toJson("Parameter missing"));
		}

		if (name.trim().isEmpty() || email.trim().isEmpty() || confirmEmail.trim().isEmpty()
				|| password.trim().isEmpty() || confirmPassword.trim().isEmpty()) {
			return badRequest(Json.toJson("Parameter missing"));
		}

		if (!Pattern.matches("\\b[a-zA-Z0-9_\\$#\\-]+\\b", name)) {
			return badRequest(Json.toJson("Only characters, numbers and _ - $ # are allowed for name"));
		}

		if (!email.equals(confirmEmail))
			return badRequest(Json.toJson("Email doesn't match"));
		if (!Pattern.matches(
				"\\b[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\.[aA][cC]\\.[uU][kK]\\b",
				email))
			return badRequest(Json.toJson("Email format error"));

		if (User.isExistingAccount(email)) {
			return badRequest(Json.toJson("Existing mail address"));
		}

		if (!password.equals(confirmPassword))
			return badRequest(Json.toJson("Parameter doesn't match"));
		if (password.length() < 8) {
			return badRequest(Json.toJson("Password too short [least 8]"));
		}
		if (password.length() > 72) {
			return badRequest(Json.toJson("Password too long [most 72]"));
		}

		String yearStr = filledForm.findPath("year").textValue();
		String monthStr = filledForm.findPath("month").textValue();
		String dayStr = filledForm.findPath("day").textValue();
		int year = -1, month = -1, day = -1;

		try {
			year = Integer.parseInt(yearStr);
			if (year < Calendar.getInstance().get(Calendar.YEAR) - 130
					|| year > Calendar.getInstance().get(Calendar.YEAR)) {
				return badRequest(Json.toJson("Wrong format of day"));
			}
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Wrong format of day"));
		}
		try {
			month = Integer.parseInt(monthStr);
			if (month <= 0 || month > 12) {
				return badRequest(Json.toJson("Wrong format of month"));
			}
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Wrong format of month"));
		}
		try {
			day = Integer.parseInt(dayStr);
			if (day <= 0) {
				badRequest(Json.toJson("Wrong format of day"));
			}

			if (month == 2) {
				if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
					if (day > 29)
						return badRequest(Json.toJson("Wrong format of day"));
				}
			} else if (month == 4 || month == 6 || month == 9 || month == 11) {
				if (day > 30)
					return badRequest(Json.toJson("Wrong format of day"));
			} else {
				if (day > 31)
					return badRequest(Json.toJson("Wrong format of day"));
			}
		} catch (NumberFormatException nfe) {
			badRequest(Json.toJson("Wrong format of day"));
		}

		User newUser = Json.fromJson(filledForm, User.class);
		newUser.email = newUser.email.toLowerCase();
		Calendar birthCalender = Calendar.getInstance();
		birthCalender.set(year, month - 1, day, 0, 0);
		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		if (birthCalender.after(today)) {
			badRequest(Json.toJson("Illegal date parameter"));
		}
		try {
			newUser.password = CipherHelper.encryptBCrypt(newUser.password);
		} catch (Exception e) {
			return internalServerError(Json.toJson("Encrypt error"));
		}
		newUser.credit = 1;
		newUser.rating = 3.00;
		newUser.recentIP = request().remoteAddress();
		newUser.dateOfBirth = birthCalender.getTime();
		newUser.dateOfRegistration = new Date();
		try {
			newUser.save();
		} catch (PersistenceException pe) {
			return internalServerError(Json.toJson("Internal Server Error"));
		}
		ObjectNode result = Json.newObject();
		result.put("Status", "Successful");
		result.put("id", newUser.id);
		result.put("day",day);
		result.put("month",month);
		result.put("year",year);
		try{
			sendConfirmEmail(newUser.id);
		}catch(UnsupportedEncodingException|IllegalArgumentException e){
			flash("error", "Failed to send confirm email");
		}
		geolocationService.getGeolocation(request().remoteAddress()).thenApplyAsync(getGeolocation ->
			{
				newUser.recentLatitude = getGeolocation.getLatitude();
				newUser.recentLongitude = getGeolocation.getLongitude();
				newUser.recentLocation = getGeolocation.getCity();
				result.put("recentLocation", newUser.recentLocation);
				result.put("credit", newUser.credit);
				return newUser;
			}, HttpExecution.defaultContext()).thenAcceptAsync(user ->
				{
					user.save();
				});

		return ok(result);

	}
	@Transactional
	private void sendConfirmEmail(long uid) throws UnsupportedEncodingException,IllegalArgumentException{
		User target = User.findByID(uid);
		long randomCode = (new Random().nextInt(90000000) + 10000000);
		target.randomCode = randomCode;
		Date regTime = target.dateOfRegistration;
		try {
			target.uuid = CipherHelper.encryptBCrypt(regTime.getTime() + String.valueOf(randomCode) + uid);
			target.uuidValidUntil = regTime.getTime() + validInternal * 2;
		} catch (IllegalArgumentException | UnsupportedEncodingException e) {
			throw e;
		}
		target.save();

		String uuid = CipherHelper.encryptAES(regTime.getTime() + String.valueOf(randomCode) + uid);
		String verfyURL = routes.UserController.confirmUser(uid, uuid).absoluteURL(request());
		Email email = new Email().setCharset("UTF-8")
				.setSubject("Welcome to BookSwop! Open the mail to finish your registration!")
				.setFrom("Registration_NO_REPLY@bookswop.me").addTo(target.email)
				.setBodyText("Hi, Please click the link below to finish the registration.\n" + verfyURL);
		mailerClient.send(email);
	}

	@Transactional
	public Result confirmUserRequest(long uid) {
		User target = User.findByID(uid);
		if (target == null || target.email == null) {
			return badRequest("No User");
		}
		if (target.isVaildEmail) {
			return badRequest("The email has been activatded");
		}
		try{
			sendConfirmEmail(uid);
		}catch(UnsupportedEncodingException|IllegalArgumentException e){
			return badRequest("Error when sending email");
		}
		return ok("Mail send");
	}

	@Transactional
	public Result confirmUser(long uid, String uuid) {
		if (uuid == null) {
			return badRequest(Json.toJson("Parameter Parsing Error"));
		}
		String verfyUUID = CipherHelper.decryptAES(uuid);
		if (verfyUUID == null) {
			return badRequest(Json.toJson("Parameter Parsing Error"));
		}
		long accuiredTime = -1;
		long accuiredUid = -1;
		long accuiredRandom = -1;
		// Logger.info(verfyUUID);
		try {
			accuiredTime = Long.parseLong(verfyUUID.substring(0, 13));
			// 13 digits,index:the 0th to12th
			accuiredRandom = Long.parseLong(verfyUUID.substring(13, 21));
			accuiredUid = Long.parseLong(verfyUUID.substring(21, verfyUUID.length()));
			// from the 21th index (inclusive) to the end
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Parameter Parsing Error"));
		}

		if (uid == -1 || accuiredTime == -1 || accuiredRandom == -1) {
			return badRequest(Json.toJson("Illegal request"));
		}

		if (uid != accuiredUid) {
			return badRequest(Json.toJson("Uid Mismatched"));
		}

		User target = User.findByID(uid);
		if (target == null) {
			return notFound(Json.toJson("No user"));
		}
		if (target == null||target.uuid==null||target.uuid.isEmpty() || target.uuidValidUntil == -1) {
			return notFound(Json.toJson("Illegal request!"));
		}
		try {
			if (!CipherHelper.validateBCrypt(verfyUUID, target.uuid)) {
				return badRequest(Json.toJson("Illegal request"));
			}
		} catch (UnsupportedEncodingException e1) {
			return internalServerError(Json.toJson("Encoding error"));
		}

		if (accuiredRandom != target.randomCode) {
			return badRequest(Json.toJson("Illegal request"));
		}
		if (accuiredTime + validInternal * 2 != target.uuidValidUntil) {
			target.dateOfRegistration = new Date();
			target.save();
			return badRequest(Json.toJson("Illegal request"));
		}
		if (target.uuidValidUntil <= System.currentTimeMillis()) {
			target.dateOfRegistration = new Date();
			target.save();
			return badRequest(Json.toJson("Link Out Dated"));
		}
		if (accuiredTime + validInternal * 2 <= System.currentTimeMillis()) {
			target.dateOfRegistration = new Date();
			target.save();
			return badRequest(Json.toJson("Link Out Dated"));
		}

		if (target.isVaildEmail) {
			return badRequest(Json.toJson("Illegal request"));
		}

		target.isVaildEmail = true;
		target.dateOfRegistration = new Date();
		try {
			target.uuid = produceToken(target, target.dateOfRegistration, false);
		} catch (IllegalArgumentException | UnsupportedEncodingException e) {
			return internalServerError("Internal Server Error");
		}
		target.save();

		ObjectNode result = Json.newObject().put("Status", "Successful").put("id", target.id);
		return redirect("/activation");
	}

	public Result showLogin() {
		//Form<User> initialForm = formFactory.form(User.class);
		//return ok(login_form.render(initialForm));
		return ok(logIn.render());
	}

	@BodyParser.Of(BodyParser.Json.class)
	@Transactional
	public Result login() {
		// Form<User> loginForm =
		// formFactory.form(User.class).bindFromRequest();
		JsonNode loginForm = request().body().asJson();
		String email;
		String password;
		if ((email = loginForm.get("email").textValue()) == null
				|| (password = loginForm.get("password").textValue()) == null) {
			return badRequest(Json.toJson("Wrong Parameter numbers"));
		}

		if (email.trim().isEmpty() || password.trim().isEmpty()) {
			return badRequest(Json.toJson("Wrong Parameter numbers"));
		}
		if (!Pattern.matches(
				"\\b[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\.[aA][cC]\\.[uU][kK]\\b",
				email))
			return badRequest(Json.toJson("Email format error"));
		List<User> user = User.findByEmail(email);
		if (user.isEmpty())
			return badRequest(Json.toJson("No user found!"));
		if (user.size() > 1)
			return internalServerError(Json.toJson("Database inconsistent!"));
		if (user.get(0).isVaildEmail == false){
			ObjectNode temp = Json.newObject();
			temp.put("status", "Unactivated").put("id", user.get(0).id).put("name",user.get(0).name).put("email",user.get(0).email).put("active",user.get(0).isVaildEmail);
			return ok(temp);
		}

		try {
			if (!CipherHelper.validateBCrypt(password, user.get(0).password))
				return unauthorized(Json.toJson("Wrong Password!"));
		} catch (UnsupportedEncodingException e) {
			return unauthorized(Json.toJson("Internal error when validate password!"));
		}
		Date timestamp = new Date();
		String uuid = null;
		try {
			uuid = produceToken(user.get(0), timestamp, false);
		} catch (Exception e) {
			return internalServerError(Json.toJson("Error when issue token!"));
		}
		ObjectNode result = Json.newObject();
		geolocationService.getGeolocation(request().remoteAddress()).thenApplyAsync(getGeolocation ->
			{
				user.get(0).recentLatitude = getGeolocation.getLatitude();
				user.get(0).recentLongitude = getGeolocation.getLongitude();
				user.get(0).recentLocation = getGeolocation.getCity();
				user.get(0).save();
				return user.get(0);
			}, HttpExecution.defaultContext()).thenAcceptAsync(toStorageUser -> toStorageUser.save());
		result.put("status", "Successful").put("id", user.get(0).id).put("uuid", uuid).put("valid_until",
				user.get(0).uuidValidUntil).put("name",user.get(0).name).put("location",user.get(0).recentLocation).put("email",user.get(0).email)
				.put("active",user.get(0).isVaildEmail).put("credit",user.get(0).credit)
				.put("day",new SimpleDateFormat("dd").format(user.get(0).dateOfBirth))
				.put("month",new SimpleDateFormat("MM").format(user.get(0).dateOfBirth))
				.put("year",new SimpleDateFormat("yyyy").format(user.get(0).dateOfBirth));
		return ok(result);
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result loginOff() {
		String uuid = request().getHeader("X-AUTH-TOKEN");
		if (uuid == null || uuid.trim().isEmpty()) {
			return badRequest(Json.toJson("Invalid Parameters"));
		}

		uuid = CipherHelper.decryptAES(uuid);

		long accuiredTime = -1;
		long accuiredUid = -1;
		long accuiredRandom = -1;
		try {
			accuiredTime = Long.parseLong(uuid.substring(0, 13));
			accuiredRandom = Long.parseLong(uuid.substring(13, 18));
			accuiredUid = Long.parseLong(uuid.substring(18, uuid.length()));
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Parameter Parsing Error"));
		}

		User toLoginOff = User.findByID(accuiredUid);
		if (toLoginOff == null) {
			return notFound(Json.toJson("No such a user"));
		}
		try {
			if (!CipherHelper.validateBCrypt(uuid, toLoginOff.uuid)) {
				return unauthorized(Json.toJson("Not a valid token"));
			}
		} catch (UnsupportedEncodingException e) {
			return badRequest(Json.toJson("Parameter Validate Error"));
		}
		toLoginOff.uuid = null;
		toLoginOff.randomCode = -1;
		toLoginOff.uuidValidUntil = -1;
		toLoginOff.save();

		return ok(Json.newObject().put("status", "Successful").put("id", toLoginOff.id));

	}

	@Transactional
	public Result psdResetRequest(String email) {

		User toReset;
		try{
			toReset = User.find.where().eq("email", email).findUnique();
		}catch(Exception nre){
			return badRequest("Inconsistent Email in database").as(JSON);
		}
		if (toReset == null)
			return notFound("No user").as(JSON);
		if (toReset.inBlock)
			return badRequest("In blocking user").as(JSON);
		if (!toReset.isVaildEmail)
			return badRequest("Email not activated").as(JSON);

		long randomCode = (new Random().nextInt(90000000) + 10000000);
		toReset.randomCode = randomCode;
		Date resetTime = new Date();
		long uid = toReset.id;
		try {
			toReset.pswResetuuid = CipherHelper.encryptBCrypt(resetTime.getTime() + String.valueOf(randomCode) + uid);
			toReset.pswResetUuidValidUntil = resetTime.getTime() + pswResetValidInternal;
			toReset.save();
		} catch (IllegalArgumentException | UnsupportedEncodingException e) {
			return badRequest("Internal Server Error").as(JSON);
		}
		toReset.save();

		String uuid = CipherHelper.encryptAES(resetTime.getTime() + String.valueOf(randomCode) + uid);
		String resetURL = routes.UserController.psdReset(uid, uuid).absoluteURL(request());
		Email toSend = new Email().setCharset("UTF-8").setSubject("BookSwop! Resetting your password.")
				.setFrom("Registration_NO_REPLY@bookswop.me").addTo(toReset.email)
				.setBodyText("Hi, Please click the link below to reset your password.\n" + resetURL);
		mailerClient.send(toSend);
		return ok();
	}

	public Result psdReset(long uid, String uuid) {

		if(uid<0||uuid.isEmpty()){
			return badRequest(Json.toJson("Parameter Missing"));
		}

		String verfyUUID = CipherHelper.decryptAES(uuid);
		long accuiredTime = -1;
		long accuiredUid = -1;
		long accuiredRandom = -1;
		try {
			accuiredTime = Long.parseLong(verfyUUID.substring(0, 13));
			// 13 digits,index:the 0th to12th
			accuiredRandom = Long.parseLong(verfyUUID.substring(13, 21));
			accuiredUid = Long.parseLong(verfyUUID.substring(21, verfyUUID.length()));
			// from the 21th index (inclusive) to the end
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Parameter Parsing Error 2"));
		}

		if (accuiredUid == -1 || accuiredTime == -1 || accuiredRandom == -1) {
			return badRequest(Json.toJson("Parameter Parsing Error 3"));
		}

		if (uid != accuiredUid) {
			return badRequest(Json.toJson("Uid Mismatched"));
		}
		User target = User.findByID(uid);
		if (target == null) {
			return notFound(Json.toJson("No user found!"));
		}
		if (target.pswResetuuid==null||target.pswResetuuid.isEmpty() || target.pswResetUuidValidUntil == -1) {
			return notFound(Json.toJson("Illegal request!"));
		}

		if (target.pswResetUuidValidUntil <= System.currentTimeMillis()) {
			return badRequest(Json.toJson("Link Out Dated"));
		}

		if (accuiredTime + pswResetValidInternal != target.pswResetUuidValidUntil) {
			return badRequest(Json.toJson("Illegal request"));
		}
		if (accuiredTime + pswResetValidInternal <= System.currentTimeMillis()) {
			return badRequest(Json.toJson("Link Out Dated"));
		}

		if (accuiredRandom != target.randomCode) {
			return badRequest(Json.toJson("Illegal request"));
		}

		if (!target.isVaildEmail) {
			return badRequest(Json.toJson("Illegal request"));
		}
		if (target.inBlock) {
			return badRequest(Json.toJson("Illegal request"));
		}

		try {
			if (!CipherHelper.validateBCrypt(verfyUUID, target.pswResetuuid)) {
				return badRequest(Json.toJson("Illegal request"));
			}
		} catch (UnsupportedEncodingException e1) {
			return internalServerError(Json.toJson("Encoding error"));
		}

		return ok(changePassword.render());

	}

	// Required uid, uuid, password, confirm_password
	@Transactional
	public Result finishPsdReset() {
		DynamicForm resetForm = formFactory.form().bindFromRequest("uid", "uuid", "password", "confirm_password");
		if (resetForm.field("password").value() == null || resetForm.field("password").value().trim().isEmpty()) {
			resetForm.reject("password", "The password is required");
		}
		if (resetForm.field("uid").value() == null || resetForm.field("uid").value().trim().isEmpty()) {
			resetForm.reject("uid", "The uid is required");
		}
		if (resetForm.field("uuid").value() == null || resetForm.field("uuid").value().trim().isEmpty()) {
			resetForm.reject("uuid", "The uuid is required");
		}

		String uuid = resetForm.field("uuid").value();
		String uidStr = resetForm.field("uid").value();
		long uid = -1;
		try {
			uid = Long.parseLong(uidStr);
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Parameter Parsing Error 1"));
		}

		if (resetForm.field("password").value() != null&&resetForm.field("password").value().length() < 8) {
			resetForm.reject("password", "The password length should at least 8 digit");
		}

		if (resetForm.field("password").value() != null&&resetForm.field("password").value().length() > 72) {
			resetForm.reject("password", "The password length is too long");
		}

		if (resetForm.field("confirm_password").value() == null||resetForm.field("password").value() == null
				|| !resetForm.field("password").value().equals(resetForm.field("confirm_password").value())) {
			resetForm.reject("confirm_password", "Password don't match");
		}

		if (resetForm.hasErrors()) {
			JsonNode info = Json.newObject().put("uid", uid).put("uuid", uuid);
			JsonNode err = Json.newObject().set("error", resetForm.errorsAsJson());
			ArrayNode res = Json.newArray().add(info).add(err);
			return badRequest(res);
		}

		String newPsw = resetForm.field("password").value();

		String verfyUUID = CipherHelper.decryptAES(uuid);
		if (verfyUUID == null) {
			return badRequest(Json.toJson("Parameter missing"));
		}

		long accuiredTime = -1;
		long accuiredUid = -1;
		long accuiredRandom = -1;
		try {
			accuiredTime = Long.parseLong(verfyUUID.substring(0, 13));
			// 13 digits,index:the 0th to12th
			accuiredRandom = Long.parseLong(verfyUUID.substring(13, 21));
			accuiredUid = Long.parseLong(verfyUUID.substring(21, verfyUUID.length()));
			// from the 21th index (inclusive) to the end
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Parameter Parsing Error 2"));
		}

		if (accuiredUid == -1 || accuiredTime == -1 || accuiredRandom == -1) {
			return badRequest(Json.toJson("Parameter Parsing Error 3"));
		}

		if (uid != accuiredUid) {
			return badRequest(Json.toJson("Uid Mismatched"));
		}

		User target = User.findByID(uid);
		if (target == null) {
			return notFound(Json.toJson("No user found!"));
		}
		if (target.pswResetuuid==null||target.pswResetuuid.isEmpty() || target.pswResetUuidValidUntil == -1) {
			return notFound(Json.toJson("Illegal request!"));
		}
		if (accuiredTime + pswResetValidInternal != target.pswResetUuidValidUntil) {
			return badRequest(Json.toJson("Illegal request"));
		}
		if (accuiredTime + pswResetValidInternal <= System.currentTimeMillis()) {
			return badRequest(Json.toJson("Link Out Dated"));
		}
		if (target.pswResetUuidValidUntil <= System.currentTimeMillis()) {
			return badRequest(Json.toJson("Link Out Dated"));
		}

		if (accuiredRandom != target.randomCode) {
			return badRequest(Json.toJson("Illegal request"));
		}

		if (!target.isVaildEmail) {
			return badRequest(Json.toJson("Illegal request"));
		}
		if (target.inBlock) {
			return badRequest(Json.toJson("Illegal request"));
		}
		try {
			if (!CipherHelper.validateBCrypt(verfyUUID, target.pswResetuuid)) {
				return badRequest(Json.toJson("Illegal request"));
			}
		} catch (UnsupportedEncodingException e1) {
			return internalServerError(Json.toJson("Encoding error"));
		}

		try {
			target.password = CipherHelper.encryptBCrypt(newPsw);
			target.uuid = "";
			target.randomCode = -1;
			target.uuidValidUntil = -1;
			target.pswResetuuid = "";
			target.uuidValidUntil = -1;
		} catch (IllegalArgumentException | UnsupportedEncodingException e1) {
			return internalServerError(Json.toJson("Encrypt Error"));
		}
		target.save();

		ObjectNode result = Json.newObject().put("Status", "Successful").put("id", target.id);
		return ok(result);
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result update() {

		Form<User> resetForm = formFactory.form(User.class).bindFromRequest(userUpdatableNames);
		JsonNode dateForm = request().body().asJson();
		boolean[] isArgs = new boolean[userUpdatableNames.length];

		for (int i = 0; i < userUpdatableNames.length; i++) {
			if (dateForm.findValue(userUpdatableNames[i]) != null) {
				if (resetForm.error(userUpdatableNames[i]) != null)
					return badRequest(Json.toJson("Parameter Error 1"));
				isArgs[i] = true;
			}

		}

		// for (int i = 0; i < userUpdatableNames.length; i++) {
		// Logger.info(String.valueOf(isArgs[i]) + "\n");
		// }

		String uidStr = dateForm.findPath("uid").textValue();
		long uid = -1;
		try {
			uid = Long.parseLong(uidStr);
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Parameter Error 2"));
		}

		if (uid == -1) {
			return badRequest(Json.toJson("Parameter Error 3"));
		}

		boolean setBirth = false;
		String yearStr = dateForm.findPath("year").textValue();
		String monthStr = dateForm.findPath("month").textValue();
		String dayStr = dateForm.findPath("day").textValue();

		int year = -1, month = -1, day = -1;
		if (yearStr != null && monthStr != null && dayStr != null) {
			try {
				year = Integer.parseInt(yearStr);
				if (year < Calendar.getInstance().get(Calendar.YEAR) - 130
						|| year > Calendar.getInstance().get(Calendar.YEAR)) {
					return badRequest(Json.toJson("Wrong format of day"));
				}
			} catch (NumberFormatException nfe) {
				return badRequest(Json.toJson("Wrong format of day"));
			}
			try {
				month = Integer.parseInt(monthStr);
				if (month <= 0 || month > 12) {
					return badRequest(Json.toJson("Wrong format of month"));
				}
			} catch (NumberFormatException nfe) {
				return badRequest(Json.toJson("Wrong format of month"));
			}
			try {
				day = Integer.parseInt(dayStr);
				if (day <= 0) {
					badRequest(Json.toJson("Wrong format of day"));
				}

				if (month == 2) {
					if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0) {
						if (day > 29)
							return badRequest(Json.toJson("Wrong format of day"));
					}
				} else if (month == 4 || month == 6 || month == 9 || month == 11) {
					if (day > 30)
						return badRequest(Json.toJson("Wrong format of day"));
				} else {
					if (day > 31)
						return badRequest(Json.toJson("Wrong format of day"));
				}
				setBirth = true;
			} catch (NumberFormatException nfe) {
				badRequest(Json.toJson("Wrong format of day"));
			}
		}

		User target = User.findByID(uid);
		if (target == null) {
			return notFound(Json.toJson("No user found!"));
		}

		if (!target.isVaildEmail) {
			return badRequest(Json.toJson("Illegal request"));
		}

		if (target.inBlock) {
			return badRequest(Json.toJson("Illegal request"));
		}
		if (setBirth) {
			Calendar birthCalender = Calendar.getInstance();
			birthCalender.set(year, month - 1, day, 0, 0);
			Calendar today = Calendar.getInstance();
			today.set(Calendar.HOUR_OF_DAY, 0);
			if (birthCalender.after(today)) {
				badRequest(Json.toJson("Illegal date parameter"));
			}
			target.dateOfBirth = birthCalender.getTime();
		}
		ObjectNode result = Json.newObject();
		Field field;
		Class<?> type;
		Method method;
		for (int i = 0; i < userUpdatableNames.length; i++) {
			if (isArgs[i]) {
				// Logger.info(String.valueOf(i));
				try {
					field = target.getClass().getDeclaredField(userUpdatableNames[i]);
					type = field.getType();
					method = target.getClass().getMethod("set" + initialUpperize(userUpdatableNames[i]), type);
					method.invoke(target, convert(type, dateForm.findValue(userUpdatableNames[i]).textValue()));
				} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException
						| NoSuchMethodException | InvocationTargetException e) {
					return internalServerError(Json.toJson("Invoke exception"));
				}
			}
		}
		target.save();
		return ok(result);

	}

	private String initialUpperize(String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private Object convert(Class<?> targetType, String text) {
		PropertyEditor editor = PropertyEditorManager.findEditor(targetType);
		editor.setAsText(text);
		return editor.getValue();
	}

	@Transactional
	@Security.Authenticated(TokenAuthenticator.class)
	public Result updateLoc() {
		DynamicForm updateForm = formFactory.form().bindFromRequest("uid");
		String uidStr;
		if (updateForm.field("uid").value() == null || (uidStr = updateForm.field("uid").value().trim()).isEmpty()) {
			return badRequest(Json.toJson("Parameter Parsing Error"));
		}
		long uid = -1;
		try {
			uid = Long.parseLong(uidStr);
		} catch (NumberFormatException nfe) {
			return badRequest(Json.toJson("Parameter Parsing Error"));
		}

		if (uid == -1) {
			return badRequest(Json.toJson("Parameter Parsing Error"));
		}
		User target = User.findByID(uid);
		if (target == null) {
			return notFound(Json.toJson("No user found!"));
		}

		if (!target.isVaildEmail) {
			return badRequest(Json.toJson("Illegal request"));
		}

		if (target.inBlock) {
			return badRequest(Json.toJson("Illegal request"));
		}
		ObjectNode result = Json.newObject().put("Status", "Successful").put("id", target.id);
		target.recentIP = request().remoteAddress();
		geolocationService.getGeolocation(request().remoteAddress()).thenApplyAsync(getGeolocation ->
			{
				target.recentLatitude = getGeolocation.getLatitude();
				target.recentLatitude = getGeolocation.getLongitude();
				target.recentLocation = getGeolocation.getCity();
				return target;
			}, HttpExecution.defaultContext()).thenAccept(toSaveTarget ->
				{
					toSaveTarget.save();
					result.put("recentLatitude", toSaveTarget.recentLatitude);
					result.put("recentLongitude", toSaveTarget.recentLongitude);
				});

		return ok(result);
	}

	@Security.Authenticated(TokenAuthenticator.class)
	public Result show(long uid, long targetUid, int email, int name, int dob, int doregister, int rating, int credit,
			int isblocked, int totallent, int recentLocation, int allowGPS) {
		java.util.Map<String, String[]> args = request().queryString();
		// Logger.info("size: "+args.size());
		boolean isSelf = false;
		if (uid == -1) {
			return badRequest(Json.toJson("Parameter Parsing Error"));
		}
		if (targetUid == -1 || targetUid == uid) {
			targetUid = uid;
			isSelf = true;
		}

		User target = User.findByID(targetUid);
		if (target == null) {
			return notFound(Json.toJson("No user found!"));
		}

		if (!target.isVaildEmail) {
			return badRequest(Json.toJson("Illegal request"));
		}

		if (target.inBlock) {
			return badRequest(Json.toJson("Illegal request"));
		}

		ObjectNode result = Json.newObject();
		Field field;
		Class<?> type;
		Object value;
		Method method;
		int temp = 0;
		// Logger.info("size: " + args.size());
		for (int i = 0; i < userVisiableNames.length - 1; i++) {
			temp = 0;
			if (args.containsKey(userVisiableNames[i])) {
				try {
					temp = Integer.parseInt(args.get(userVisiableNames[i])[0]);
				} catch (NumberFormatException nfe) {
					nfe.printStackTrace();
				}
				if (temp == 1) {
					try {
						field = target.getClass().getDeclaredField(userVisiableNames[i]);
						type = field.getType();
						method = target.getClass().getMethod("get" + initialUpperize(userVisiableNames[i]));
						value = method.invoke(target);
						result.put(userVisiableNames[i], String.valueOf(value));
					} catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException
							| SecurityException | NoSuchMethodException | InvocationTargetException e) {
						return internalServerError(Json.toJson("Field find exception"));
					}
				}
			}
		}

		temp = 0;
		if (isSelf && args.containsKey(userVisiableNames[userVisiableNames.length - 1])) {
			try {
				temp = Integer.parseInt(args.get(userVisiableNames[userVisiableNames.length - 1])[0]);
			} catch (NumberFormatException nfe) {
				nfe.printStackTrace();
			}

			if (temp == 1) {
				try {
					field = target.getClass().getDeclaredField(userVisiableNames[userVisiableNames.length - 1]);
					type = field.getType();
					method = target.getClass()
							.getMethod("get" + initialUpperize(userVisiableNames[userVisiableNames.length - 1]));
					value = method.invoke(target);
					// Logger.info("value: " + type.cast(value).toString());
				} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException
						| NoSuchMethodException | InvocationTargetException e) {
					return internalServerError(Json.toJson("Field find exception"));
				}

			}
		}
		return ok(result);
	}

}
