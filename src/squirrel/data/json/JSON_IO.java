package squirrel.data.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import squirrel.ir.retrieve.RT_Query;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JSON_IO {

	public static void storeDocument(JSON_Document json) {

		// Create a pretty-printing GSON object
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		// Transform the JSON_Document object to a formatted JSON String
		String jsonOutput = gson.toJson(json);

		// Write the file on disk
		File f = new File("data/");
		f.mkdir();

		try {
			FileWriter writer = new FileWriter("data/" + json.getId() + ".json");
			writer.write(jsonOutput);
			writer.close();

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static JSON_Document retrieveDocument(File fJSON, int docID) {
		Gson gson = new Gson();

		// Read a JSON_Document object from the disk
		try {
			BufferedReader br = new BufferedReader(new FileReader(fJSON));
			JSON_Document json = gson.fromJson(br, JSON_Document.class);

			return json;

		} catch (IOException e) {
			System.out.println("File could not be retrieved. " + e.toString());
		}

		return null;
	}

	/**
	 * Read a list of query files from the given directory.
	 * 
	 * @param sBaseDirectory the directory in which to look for the json files.
	 */
	public static ArrayList<RT_Query> retrieveQueries(String sBaseDirectory) {
		Gson gson = new Gson();
		ArrayList<RT_Query> result = new ArrayList<RT_Query>();

		// Read all the JSON files containing test queries
		try {
			// Filter out only JSON files
			FilenameFilter filter = new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return name.endsWith(".json");
				}
			};

			// Get the list of JSON files in this directory
			File folder = new File(sBaseDirectory);
			File[] files = folder.listFiles(filter);

			// Read the objects iteratively
			for (File file : files) {
				BufferedReader br = new BufferedReader(new FileReader(file));
				RT_Query query = gson.fromJson(br, RT_Query.class);
				result.add(query);
			}

			// Sort the queries by id
			Collections.sort(result);
			return result;
		} catch (IOException e) {
			System.out.println("File could not be retrieced. " + e.toString());
		}

		return null;
	}
}
