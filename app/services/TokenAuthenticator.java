package services;

import java.io.UnsupportedEncodingException;
import java.util.Base64;

import controllers.routes;
import models.User;
import play.mvc.Http.Context;
import play.Logger;
import play.db.ebean.Transactional;
import play.mvc.Result;
import play.mvc.Security.Authenticator;
import play.libs.Json;
public class TokenAuthenticator extends Authenticator {
	String email = "";

	@Override
	public String getUsername(Context ctx) {
		String token = getTokenFromHeader(ctx);
		long uid;
		try {
			uid = getUidFromQuery(ctx);
		} catch (NumberFormatException ex) {
			return null;
		}
		if (token != null && checkToken(token, uid)) {
			return email;
		}
		return null;
	}

	@Override
	public Result onUnauthorized(Context context) {
		return 	unauthorized(Json.toJson("No authorization"));
	}

	private String getTokenFromHeader(Context ctx) {
		String[] authTokenHeaderValues = ctx.request().headers().get("X-AUTH-TOKEN");
		if ((authTokenHeaderValues != null) && (authTokenHeaderValues.length == 1)
				&& (authTokenHeaderValues[0] != null)) {
			return authTokenHeaderValues[0];
		}
		return null;

	}

	private long getUidFromQuery(Context ctx) throws NumberFormatException {
		if (ctx.request().method().equals("GET")||ctx.request().method().equals("DELETE")) {
			String uidStr =  ctx.request().getQueryString("uid");
			return Long.parseLong(uidStr);
		} else {
			String body = ctx.request().body().asJson().get("uid").asText();

			if (body != null) {
				return Long.parseLong(body);
			}
			throw new NumberFormatException();
		}

	}

	@Transactional
	private boolean checkToken(String token, long uid) {
		if (uid<0) return false;
		User target = User.findByID(uid);
		if (target == null)
			return false;

		token = CipherHelper.decryptAES(token);
		String tokenInDb = target.uuid;
		String timestampStr;
		String randomStr;
		String uidStr;
		long timestamp;
		long random;
		long parsedID;

		try {
			if (token != null && tokenInDb != null && CipherHelper.validateBCrypt(token, tokenInDb)) {
				timestampStr = token.substring(0, 13);
				randomStr = token.substring(13, 18);
				uidStr = token.substring(18, token.length());
				try {
					timestamp = Long.parseLong(timestampStr);
					random = Long.parseLong(randomStr);
					parsedID = Long.parseLong(uidStr);
				} catch (NumberFormatException nfe) {
					return false;
				}

				if (parsedID != target.id || timestamp != target.uuidValidUntil || random != target.randomCode
						|| target.uuidValidUntil <= System.currentTimeMillis()) {

					target.uuid = null;
					target.randomCode = -1;
					target.uuidValidUntil = -1;
					target.save();
					return false;
				}
				email = target.email;
				return true;
			} else {
				return false;
			}
		} catch (UnsupportedEncodingException | IllegalArgumentException e) {
			return false;
		}
	}
}
