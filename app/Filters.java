import javax.inject.*;
import play.*;
import play.mvc.EssentialFilter;
import play.http.HttpFilters;
import play.filters.headers.SecurityHeadersFilter;
import play.http.DefaultHttpFilters;
import play.filters.cors.CORSFilter;
import play.mvc.*;

/**
 * This class configures filters that run on every request. This class is
 * queried by Play to get a list of filters.
 *
 * Play will automatically use filters from any class called
 * <code>Filters</code> that is placed the root package. You can load filters
 * from a different class by adding a `play.http.filters` setting to the
 * <code>application.conf</code> configuration file.
 */
@Singleton
public class Filters implements HttpFilters {

	private final Environment env;
	//private final SecurityHeadersFilter securityHeadersFilter;
	private final CORSFilter corsFilter;
	@Inject
	public Filters(Environment env, SecurityHeadersFilter securityHeadersFilter,CORSFilter corsFilter) {
//public Filters(Environment env) {
		this.corsFilter = corsFilter;
		//this.securityHeadersFilter = securityHeadersFilter;
		this.env = env;
	}

	@Override
	public EssentialFilter[] filters() {
		// Use the example filter if we're running development mode. If
		// we're running in production or test mode then don't use any
		// filters at all.
		if (env.mode().equals(Mode.DEV)) {
			return new EssentialFilter[] { corsFilter.asJava()};
			//return new EssentialFilter[] { };
		} else {
			return new EssentialFilter[] { corsFilter.asJava() };
			//return new EssentialFilter[] { };
		}
	}

}
