package squirrel.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import squirrel.ir.index.IX_Collection;

/**
 * Helper class for creating and managing collections.
 * 
 * @author Steffen Zschaler
 */
public class UTIL_Collections {

	/**
	 * Create or open a collection, asking the user to provide a name for the
	 * collection.
	 * 
	 * @return the new collection object
	 */
	public static IX_Collection createOrOpenCollection() {
		// get the list of existing collections
		File[] dirs = {};
		File dir = new File("collections/");
		if (dir.exists()) {
			dirs = dir.listFiles();
		}

		// list available collections
		System.out.println("\nAvailable Collections are:");
		if (dirs.length > 0) {
			for (File dirname : dirs) {
				if (dirname.isDirectory()) {
					System.out.println(dirname.getName());
				}
			}
		} else {
			System.out.println("No collections available.");
		}

		Scanner in = new Scanner(System.in);
		System.out.println("Please enter the collection's name:");
		String name = in.nextLine();

		// Check whether collection already exists
		boolean fOpen = false;
		for (File dirname : dirs) {
			if (name.equals(dirname.getName())) {
				fOpen = true;
				break;
			}
		}
		if (fOpen) {
			return openCollection(name);
		} else {
			try {
				return new IX_Collection(name);
			} catch (IOException e) {
				System.err.println("Issue creating collection: "
						+ e.getLocalizedMessage());

				return null;
			}
		}
	}

	/**
	 * Open an existing collection
	 * 
	 * @param name
	 *            the name of the collection to open
	 * 
	 * @return the collection object or <code>null</code> if the collection
	 *         doesn't exist
	 */
	private static IX_Collection openCollection(String name) {

		IX_Collection col = null;

		try {
			ObjectInputStream in = new ObjectInputStream(new GZIPInputStream(
					new FileInputStream("collections/" + name + "/index")));

			col = (IX_Collection) in.readObject();
			col.setIsChanged(false);

			in.close();

			in = null;

			return col;
		} catch (IOException e) {
			System.out
					.println("There seems to be a problem handling the file: "
							+ e.getMessage());
			return null;
		} catch (ClassNotFoundException e) {
			System.out
					.println("There seems to be a problem handling the file: "
							+ e.getMessage());
			return null;
		}
	}

	/**
	 * Close a collection and save it to disk if necessary.
	 * 
	 * @param col
	 *            the collection to be closed.
	 */
	public static void closeCollection(IX_Collection col) {

		String name = col.getName();

		if (col.isChanged()) {
			// Ensure there is a file with information about the collection in
			// human-readable form
			try {
				BufferedWriter bw = new BufferedWriter(new FileWriter(
						"collections/" + name + "/README.txt"));
				bw.write("[default]\n");
				bw.write("collection_name = " + col.getName() + "\n");
				bw.write("collection_size = " + col.getCount() + "\n");
				//bw.write("collection_delta = " + col.getDelta() + "\n");
				bw.close();
			} catch (IOException ioe) {
				System.err.println("Problems writing README.txt: "
						+ ioe.getLocalizedMessage());
			}

			// Save the actual collection
			try {
				ObjectOutputStream out = new ObjectOutputStream(
						new GZIPOutputStream(new FileOutputStream(
								"collections/" + name + "/index")));

				out.writeObject(col);

				out.close();
				out = null;

			} catch (IOException e) {
				System.out
						.println("There seems to be a problem handling the file:"
								+ e.getMessage());
				return;
			}

			System.out.println("Collection: '" + col.getName()
					+ "' closed and saved to disk.");
		} else {
			System.out.println("Collection: '" + col.getName() + "' closed.");
		}
	}

}