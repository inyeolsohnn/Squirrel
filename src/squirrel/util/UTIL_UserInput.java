package squirrel.util;

import java.io.File;
import java.util.InputMismatchException;
import java.util.Scanner;

public class UTIL_UserInput {

	// accepts a parameter from the keyboard, between two given values
	public static int parameterInput(int start, int end) {
		Scanner in = new Scanner(System.in);
		int choice = 0;
		boolean done = false;
		// Read a parameter value (int) from the keyboard
		while (!done) {
			try {
				choice = in.nextInt();
			} catch (InputMismatchException e) {
				in.nextLine();
				System.out.println("Wrong type of input. Please try again.");
				continue;
			}
			if (choice > end || choice < start) {
				System.out.println("Wrong choice, there is no such option. Please try again.");
			} else {
				done = true;
			}
		}
		return choice;
	}

	// accepts a string from the keyboard
	public static String termInput() {
		Scanner in = new Scanner(System.in);
		String result = "";
		boolean done = false;

		// Read query terms (String) from the keyboard
		while (!done) {
			result = in.nextLine();

			if (result.length() < 3) {
				System.out.println("Please enter at least one keyword that's "
						+ "at least 3 characters long:");
			} else {
				done = true;
			}
		}
		return result;
	}

	public static String directoryInput() {
		Scanner in = new Scanner(System.in);
		String result = "";
		boolean done = false;

		do {
			result = in.nextLine();

			File f = new File (result);
			if (!f.isDirectory()) {
				System.out.print ("\nPlease enter a valid directory name:");
			}
			else {
				done = true;
			}
		} while (!done);
		return result;
	}
}
