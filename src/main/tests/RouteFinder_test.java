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
}
