package controllers;
import play.mvc.*;
import views.html.*;
import play.data.*;

public class Application extends Controller {

    public Result main() {
        return ok(main.render());
    }

    public Result logIn() {
        return ok(logIn.render());
    }

    public Result signUp() {
        return ok(signUp.render());
    }

    public Result homePage() {
        return ok(homePage.render());
    }

    public Result profile() {
        return ok(profile.render());
    }

    public Result forgetPassword() {
   		return ok(forgetPassword.render());
    }

    public Result changePassword() {
        return ok(changePassword.render());
    }

    public Result termsOfService() {
        return ok(termsOfService.render());
    }

    public Result privacyPolicy() {
        return ok(privacyPolicy.render());
    }

    public Result aboutUs(){
        return ok(aboutUs.render());
    }

    public Result bookDetail(){
        return ok(bookDetail.render());
    }

    public Result confirmBook(){
        return ok(confirmBook.render());
    }
    public Result welcome(){
        return ok(welcome.render());
    }
    public Result activator(){
        return ok(activator.render());
    }
}
