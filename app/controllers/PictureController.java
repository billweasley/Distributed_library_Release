package controllers;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

import org.apache.commons.io.FileUtils;

import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Http.MultipartFormData.FilePart;
import play.mvc.Result;

public class PictureController extends Controller {
	private final String JSON = "application/json; charset=UTF-8";
	private static Random randomProducer = new Random();
	private String root = "/public/imgs";
	
	
	public Result uploadPic() {
		MultipartFormData<File> body = request().body().asMultipartFormData();
		FilePart<File> picture = body.getFile("img");
		if (picture != null) {
			String fileName = picture.getFilename();
			fileName = fileName + System.currentTimeMillis() + randomProducer.nextInt();
			Base64.getEncoder().encodeToString(fileName.getBytes());
			String contentType = picture.getContentType();
			File file = picture.getFile();
			if (!contentType.startsWith("image/")) {
				file.delete();
				return badRequest(Json.toJson("Not a image"));
			}
			try {
				File dest = new File("root" + "/" + fileName);
				FileUtils.moveFile(file, dest);
				return ok(Json.newObject().put("Status", "successful").put("address", dest.getPath()));
			} catch (IOException ioe) {
				file.delete();
				return internalServerError("Failed to upload the file");
			}

		} else {
			return badRequest(Json.toJson("Missing file"));
		}
	}

}
