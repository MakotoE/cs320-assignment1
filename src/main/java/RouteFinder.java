import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

// TODO comments
public class RouteFinder implements IRouteFinder {
	@Override
	public Map<String, Map<String, String>> getBusRoutesUrls(char destInitial) {
		String page = null;
		try {
			page = getURLText(new URL(TRANSIT_WEB_URL));
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		if (!String.valueOf(destInitial).toLowerCase().matches("[a-z]")) {
			throw new RuntimeException("destInitial is not a letter");
		}

		return destinationsThatStartWithChar(allRoutes(page), destInitial);
	}

	static Map<String, Map<String, String>> destinationsThatStartWithChar(
		HashMap<String, Map<String, String>> destinations,
		char c
	) {
		return destinations.entrySet().stream().filter((entry) -> {
			return entry.getKey().toLowerCase().startsWith(String.valueOf(c).toLowerCase());
		}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@Override
	public Map<String, LinkedHashMap<String, String>> getRouteStops(String url) {
		if (url.startsWith("/")) {
			url = "https://www.communitytransit.org" + url;
		}

		String page;
		try {
			page = getURLText(new URL(url));
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return routeStops(page);
	}

	static String getURLText(URL url) throws IOException {
		var connection = url.openConnection();
		connection.setRequestProperty("user-Agent", "Mozilla/5.0");
		var streamReader = new InputStreamReader(
			connection.getInputStream(),
			StandardCharsets.UTF_8
		);
		try (var reader = new BufferedReader(streamReader)) {
			String s = reader
				.lines()
				.collect(Collectors.joining("\n"));
			return s.replaceAll("&amp;", "&");
		}
	}

	static final Pattern destinationSectionDelimiter = Pattern.compile("<hr id=\"[^\"]*\" ?/?>");

	/**
	 * @param pageStr HTML of schedules page
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
			<strong><a href="/schedules/route/201-202" ...>201/202</a></strong>
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
		scanner.useDelimiter(destinationSectionDelimiter);
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

	static final Pattern routeDelimiter = Pattern.compile("<div class=\"row Community");
	static final Pattern urlPattern = Pattern.compile("(?<=<strong><a href=\").*?(?=\")");
	static final Pattern routePattern = Pattern.compile("(?<=>).*?(?=</a>)");

	/**
	 * @param destinationSection Part of page that contains the destination routes
	 * @return Route IDs to Route URLs
	 */
	static HashMap<String, String> destinationRoutes(String destinationSection) {
		var routes = new HashMap<String, String>();

		var scanner = new Scanner(destinationSection);
		scanner.useDelimiter(routeDelimiter);

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

	static final Pattern routeTableDelimiter = Pattern.compile(
		"<div id=\".*?\".*?class=\"RouteChart\">");

	/**
	 * @param pageStr HTML of route page
	 * @return All weekday stops in given route page, where
	 * __key: Destination ("To ____")
	 * __value: Map of stops, where
	 * ____key: Stop number
	 * ____value: Address
	 */
	static Map<String, LinkedHashMap<String, String>> routeStops(String pageStr) {
		/*
		<div id="Weekday201-202s" style="" class="RouteChart">
		  <table class="table table-bordered table-hover">
			<thead>
			  <tr>
				<td colspan="8">
				  <h2>Weekday<small>To Lynnwood Transit Center</small></h2>
				</td>
			  </tr>
			  <tr>
				<th class="text-center">Route</th>
				<th class="text-center">
				  <span class="fa-stack">
				    ...
				    <strong class="fa fa-stack-1x">1</strong>
				  </span>
				  <p>Smokey Point Transit Center Bay 1</p>
				</th>
				<th class="text-center">
				  <span class="fa-stack">
				    ...
				    <strong class="fa fa-stack-1x">2</strong>
				  </span>
				  <p>State Ave &amp; 88th St NE</p>
				</th>
			  </tr>
			</thead>
		  </table>
		  ...
		</div>
		...
		<div id="Weekday201-202s" style="" class="RouteChart">
		  <table class="table table-bordered table-hover">
			<thead>
			  <tr>
				<td colspan="8">
				  <h2>Saturday<small>To Smokey Point</small></h2>
		...
		<div id="Saturday201-202s" style="display:none;" class="RouteChart">
		  <!-- Saturday schedules start -->
		 */
		var result = new HashMap<String, LinkedHashMap<String, String>>();

		var scanner = new Scanner(pageStr);
		scanner.useDelimiter(routeTableDelimiter);
		if (scanner.hasNext()) {
			scanner.next();
		}

		while (scanner.hasNext()) {
			var routeTable = scanner.next();
			if (!routeTable.contains("<h2>Weekday")) {
				break;
			}

			routeTableDestination(routeTable).ifPresent((destination) -> {
				result.put(destination, stops(routeTable));
			});
		}
		return result;
	}

	static final Pattern routeTableDestinationPattern
		= Pattern.compile("(?<=<h2>Weekday<small>).*?(?=</small></h2>)");

	static Optional<String> routeTableDestination(String routeTable) {
		var matcher = routeTableDestinationPattern.matcher(routeTable);
		if (!matcher.find()) {
			return Optional.empty();
		}

		return Optional.of(matcher.group());
	}

	static final Pattern stopDelimiter = Pattern.compile("<th class=\"text-center\">");
	static final Pattern stopNumberPattern =
		Pattern.compile("(?<=<strong class=\"fa fa-stack-1x\">).*?(?=</strong>)");
	static final Pattern stopNamePattern = Pattern.compile("(?<=<p>).*?(?=</p>)");

	static LinkedHashMap<String, String> stops(String pageStr) {
		var stops = new LinkedHashMap<String, String>();

		var scanner = new Scanner(pageStr);
		scanner.useDelimiter(stopDelimiter);

		while (scanner.hasNext()) {
			var stop = scanner.next();
			var stopNumberMatcher = stopNumberPattern.matcher(stop);
			var stopNameMatcher = stopNamePattern.matcher(stop);
			if (stopNumberMatcher.find() && stopNameMatcher.find()) {
				stops.put(stopNumberMatcher.group(), stopNameMatcher.group());
			}
		}

		return stops;
	}
}
