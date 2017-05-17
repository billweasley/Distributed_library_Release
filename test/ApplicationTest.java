import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.*;

import play.mvc.*;
import play.mvc.Http.RequestBuilder;
import play.test.*;
import play.Application;
import play.Logger;
import play.Mode;
import play.data.DynamicForm;
import play.data.validation.ValidationError;
import play.data.validation.Constraints.RequiredValidator;
import play.i18n.Lang;
import play.inject.guice.GuiceApplicationBuilder;
import play.libs.F;
import play.libs.F.*;
import play.twirl.api.Content;
import services.CipherHelper;

import static play.test.Helpers.*;
import static org.junit.Assert.*;


/**
 *
 * Simple (JUnit) tests that can call all parts of a play app.
 * If you are interested in mocking a whole application, see the wiki for more details.
 *
 */
public class ApplicationTest {

 /*   @Test
    public void simpleCheck() {
        int a = 1 + 1;
        assertEquals(2, a);
    }

    @Test
    public void renderTemplate() {
        Content html = views.html.index.render("Your new application is ready.");
        assertEquals("text/html", html.contentType());
        assertTrue(html.body().contains("Your new application is ready."));
    }

*/
	protected Application application;

	  @Before
	  public void startApp() throws Exception {
	    @SuppressWarnings("deprecation")
		ClassLoader classLoader = FakeApplication.class.getClassLoader();
	    application = new GuiceApplicationBuilder().in(classLoader)
	            .in(Mode.TEST).build();
	    Helpers.start(application);
	  }
	  @After
	  public void stopApp() throws Exception {
	    Helpers.stop(application);
	  }
	//Get method test for token authenticator...
	@Test
	public void validationTest(){
		String url = controllers.routes.BookController.queryById(5,1).url();
		Logger.info("Testing "+url);
	    RequestBuilder request = new RequestBuilder().method("GET")
	           .header("X-AUTH-TOKEN", "VgA0Gftdoibxo9NcVbDKK651EwLDkWcLamA82xezn14").uri(url);
	    Result result = route(request);
	    assertEquals(result.status(),OK);

	}
	//Put method test for token authenticator...
	@Test
	public void validationPostTest() throws JsonProcessingException, IOException{
		JsonNode js = (new ObjectMapper()).readTree("{ \"uid\": \"1\" }");
	    RequestBuilder request = new RequestBuilder().method("PUT").header("X-AUTH-TOKEN", "VgA0Gftdoibxo9NcVbDKK651EwLDkWcLamA82xezn14")
	           .bodyJson(js).uri(controllers.routes.UserController.update(1).url());
	   
	    Result result = route(request);
	    assertEquals(result.status(),OK);

	}
}
