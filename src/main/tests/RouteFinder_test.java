import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@RunWith(Enclosed.class)
public class RouteFinder_test {
	public static class allRoutes {
		@Test
		public void test() throws IOException {
			Assert.assertTrue(RouteFinder.allRoutes("").isEmpty());
			var pageStr = Files.readString(
				Path.of("Bus Schedules & Route Maps | Community Transit.html")
			);
			var result = RouteFinder.allRoutes(pageStr);
			Assert.assertFalse(result.isEmpty());
			var expectedArlington = new HashMap<>(Map.of(
				"201/202", "/schedules/route/201-202",
				"220", "/schedules/route/220",
				"227", "/schedules/route/227",
				"240", "/schedules/route/240",
				"230", "/schedules/route/230"
			));
			Assert.assertEquals(expectedArlington, result.get("Arlington"));
			Assert.assertFalse(result.get("Bellevue").isEmpty());
		}
	}

	public static class destinationName {
		@Test
		public void test() {
			Assert.assertTrue(RouteFinder.destinationName("").isEmpty());
			var result = RouteFinder.destinationName("<h3>Arlington</h3>");
			Assert.assertEquals(Optional.of("Arlington"), result);
		}
	}

	@RunWith(Parameterized.class)
	public static class destinationRoutes {
		@Parameterized.Parameters
		public static Collection<Object> data() {
			return Arrays.asList(new Object[][]{
				{
					"",
					new HashMap<>(),
				},
				{
					"""
						<div class="row Community">
							<div class="col-xs-3 text-nowrap">
								<strong><a href="/schedules/route/201-202">201/202</a></strong>
							</div>
							<div class="col-xs-8 col-xs-offset-1">Smokey Point to Lynnwood</div>
						</div>
						<div class="row Community">
							<div class="col-xs-3 text-nowrap">
								<strong><a href="/schedules/route/220">220</a></strong>
							</div>
							<div class="col-xs-8 col-xs-offset-1">Arlington to Smokey Point</div>
						</div>
					""",
					new HashMap<>(Map.of(
						"201/202", "/schedules/route/201-202",
						"220", "/schedules/route/220"
					)),
				},
			});
		}

		@Parameterized.Parameter
		public String destinationSection;

		@Parameterized.Parameter(1)
		public HashMap<String, String> expected;

		@Test
		public void test() {
			var result = RouteFinder.destinationRoutes(destinationSection);
			Assert.assertEquals(expected, result);
		}
	}

	public static class routeStops {
		@Test
		public void test() throws IOException {
			Assert.assertTrue(RouteFinder.routeStops("").isEmpty());
			var pageStr = Files.readString(
				Path.of("Bus Route 201_202 - Smokey Point to Lynnwood | Community Transit.html")
			);
			var result = RouteFinder.routeStops(pageStr);
			Assert.assertEquals(2, result.size());
			var toLynnwood = result.get("To Lynnwood Transit Center");
			Assert.assertEquals(7, toLynnwood.size());
			Assert.assertEquals("Smokey Point Transit Center Bay 1", toLynnwood.get(1));
		}
	}

	public static class routeTableDestination {
		@Test
		public void test() {
			Assert.assertTrue(RouteFinder.routeTableDestination("").isEmpty());
			var page = """
				<div id="Weekday201-202s" style="" class="RouteChart">
					<div class="table-responsive">
						<table class="table table-bordered table-hover">
							<thead>
								<tr>
									<td colspan="8">
										<h2>Weekday<small>To Lynnwood Transit Center</small></h2>
									</td>
				""";
			var result = RouteFinder.routeTableDestination(page);
			Assert.assertEquals(Optional.of("To Lynnwood Transit Center"), result);
		}
	}

	public static class stops {
		@Test
		public void test() {
			Assert.assertTrue(RouteFinder.stops("").isEmpty());
			var page = """
				<div id="Weekday201-202s" style="" class="RouteChart">
				<div class="table-responsive">
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
										 <i class="fa fa-circle-thin fa-stack-2x"></i>
										 <strong class="fa fa-stack-1x">1</strong>
									 </span>
									 <p>Smokey Point Transit Center Bay 1</p>
								 </th>
								 <th class="text-center">
									 <span class="fa-stack">
										 <i class="fa fa-circle-thin fa-stack-2x"></i>
										 <strong class="fa fa-stack-1x">2</strong>
									 </span>
									 <p>State Ave &amp; 88th St NE</p>
								 </th>
				""";
			var expected = new LinkedHashMap<>(Map.of(
				1, "Smokey Point Transit Center Bay 1",
				2, "State Ave &amp; 88th St NE"
			));
			Assert.assertEquals(expected, RouteFinder.stops(page));
		}
	}
}
