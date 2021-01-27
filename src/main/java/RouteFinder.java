import java.util.*;
import java.util.regex.Pattern;

public class RouteFinder implements IRouteFinder {
	@Override
	public Map<String, Map<String, String>> getBusRoutesUrls(char destInitial) {
		return null;
	}

	@Override
	public Map<String, LinkedHashMap<Integer, String>> getRouteStops(String url) {
		return null;
	}

	static final Pattern destinationSectionPattern = Pattern.compile("<hr id=\"[^\"]*\" ?/?>");

	/**
	 * @return All routes, where
	 * __key: destination
	 * __value: mapping of routes in destination, where
	 * ____key: Route ID
	 * ____value: Route URL
	 */
	static HashMap<String, Map<String, String>> allRoutes(String pageStr) {
		/*
		<hr id="arlington" />
		<h3>Arlington</h3>
		<div class="row Community">
		  <div class="col-xs-3 text-nowrap">
			<strong><a href="/schedules/route/201-202">201/202</a></strong>
		  </div>
		  ...
		</div>
		<div class="row Community">
		  <div class="col-xs-3 text-nowrap">
			<strong><a href="/schedules/route/220">220</a></strong>
		  </div>
		  ...
		</div>
		 */
		var scanner = new Scanner(pageStr);
		scanner.useDelimiter(destinationSectionPattern);
		if (scanner.hasNext()) {
			scanner.next();
		}

		var result = new HashMap<String, Map<String, String>>();
		while (scanner.hasNext()) {
			var destinationSection = scanner.next();
			destinationName(destinationSection).ifPresent((destination) -> {
				result.put(destination, destinationRoutes(destinationSection));
			});
		}
		return result;
	}

	static final Pattern destinationPattern = Pattern.compile("(?<=<h3>).*?(?=</h3>)");

	/**
	 * @param destinationSection Part of page that contains the destination routes
	 * @return Name of destination if it exists
	 */
	static Optional<String> destinationName(String destinationSection) {
		var matcher = destinationPattern.matcher(destinationSection);
		if (!matcher.find()) {
			return Optional.empty();
		}
		return Optional.of(matcher.group());
	}

	static final Pattern routeDelimiterPattern = Pattern.compile("<div class=\"row Community\">");
	static final Pattern urlPattern = Pattern.compile("(?<=<strong><a href=\").*?(?=\">)");
	static final Pattern routePattern = Pattern.compile("(?<=>).*?(?=</a>)");

	/**
	 * @param destinationSection Part of page that contains the destination routes
	 * @return Route IDs to Route URLs
	 */
	static HashMap<String, String> destinationRoutes(String destinationSection) {
		var routes = new HashMap<String, String>();

		var scanner = new Scanner(destinationSection);
		scanner.useDelimiter(routeDelimiterPattern);

		while (scanner.hasNext()) {
			var routeSection = scanner.next();
			var urlMatcher = urlPattern.matcher(routeSection);
			if (urlMatcher.find()) {
				var routeMatcher = routePattern.matcher(routeSection)
					.region(urlMatcher.end(), routeSection.length());
				if (routeMatcher.find()) {
					routes.put(routeMatcher.group(), urlMatcher.group());
				}
			}
		}
		return routes;
	}
}
