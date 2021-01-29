import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class Client {
	public static void main(String[] args) throws ExecutionException, InterruptedException {
		var in = new Scanner(System.in);
		var out = new PrintWriter(System.out);

		// Gets the routes list
		var destinationsAsync = CompletableFuture.supplyAsync(() -> {
			try {
				String page = RouteFinder.getURLText(new URL(RouteFinder.TRANSIT_WEB_URL));
				return RouteFinder.allRoutes(page);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		});

		// Main loop
		while (true) {
			Map<String, Map<String, String>> destinations;
			while (true) {
				out.print("Please enter a letter that your destinations start with ");
				out.flush();
				var input = in.nextLine().trim();

				if (input.length() == 1) {
					destinations = RouteFinder.destinationsThatStartWithChar(
						destinationsAsync.get(),
						input.charAt(0)
					);

					if (destinations.isEmpty()) {
						out.println("No matching destination found.");
						out.println();
						out.flush();
					} else {
						break;
					}
				}
			}

			for (var destination : destinations.entrySet()) {
				out.print("Destination: ");
				out.println(destination.getKey());

				destination.getValue().keySet().stream().sorted().forEach((routeID) -> {
					out.print("Bus Number: ");
					out.println(routeID);
				});

				out.println("+++++++++++++++++++++++++++++++++++");
			}
			out.flush();

			Map<String, LinkedHashMap<String, String>> routeStops;
			while (true) {
				out.print("Please enter your destination: ");
				out.flush();
				var destination = in.nextLine().trim();

				if (!destinations.containsKey(destination)) {
					out.println("Destination not found.");
					out.println();
					continue;
				}

				// Preload data
				// Map of route ID -> routeStops future
				var routesAsync = destinations.get(destination).entrySet().stream().map((entry) -> {
					return Map.entry(
						entry.getKey(),
						CompletableFuture.supplyAsync(() -> {
							return new RouteFinder().getRouteStops(entry.getValue());
						})
					);
				}).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

				out.print("Please enter a route ID: ");
				out.flush();
				var routeID = in.nextLine().trim();

				if (destinations.get(destination).containsKey(routeID)) {
					routeStops = routesAsync.get(routeID).get();
					break;
				} else {
					out.println("Route not found.");
					out.println();
				}
			}

			for (var routeDirection : routeStops.entrySet()) {
				out.print("Destination: ");
				out.println(routeDirection.getKey());

				for (var stops : routeDirection.getValue().entrySet()) {
					out.print("Stop number: ");
					out.print(stops.getKey());
					out.print(" is ");
					out.println(stops.getValue());
				}
			}
			out.println();

			out.print(
				"Do you want to check different destination? Please type Y to continue or press any other key to exit "
			);
			out.flush();

			if (!in.nextLine().trim().equalsIgnoreCase("y")) {
				return;
			}
		}
	}
}
