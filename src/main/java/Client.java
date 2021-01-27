import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Client {
	public static void main(String[] args) {
		System.out.println(new RouteFinder().getBusRoutesUrls('b'));
	}
}
