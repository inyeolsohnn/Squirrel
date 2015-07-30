package squirrel.ir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import squirrel.data.json.JSON_IO;
import squirrel.ir.index.IX_Collection;
import squirrel.ir.index.IX_Term;
import squirrel.ir.index.IX_TermMatch;
import squirrel.ir.retrieve.RT_Query;
import squirrel.ir.retrieve.RT_Result;
import squirrel.ir.retrieve.models.MDL_GenericModel;
import squirrel.ir.retrieve.models.MDL_Vector;
import squirrel.ir.retrieve.models.MDL_XueIBMQR;
import squirrel.ir.retrieve.models.MDL_XueQR;
import squirrel.smt.aligner.IBM1.WordPair;
import squirrel.util.UTIL_Collections;
import squirrel.util.UTIL_FileOperations;
import squirrel.util.UTIL_Patterns;
import squirrel.util.UTIL_UserInput;

@SuppressWarnings("unused")
public class Main {

	public static void main(String[] args) {
		new Main().run();
	}

	private IX_Collection col = null;
	private MDL_Vector vm = null;
	private MDL_XueQR xm = null;
	private MDL_XueIBMQR xi = null;
	private Scanner in = new Scanner(System.in);

	protected void run() {
		// initializations
		int choice;

		// infinite loop, exit on window close or when selected from the menu
		for (;;) {
			System.out.println("\n");
			System.out
					.println("===============================================================");
			System.out.println("                       Squirrel Main Menu");
			System.out
					.println("===============================================================");
			System.out
					.println("To create a new Collection,                             enter 1");
			System.out
					.println("To open an existing Collection,                         enter 2");
			System.out
					.println("To close the opened Collection,                         enter 3");
			System.out
					.println("To insert documents in the opened Collection,           enter 4");
			System.out
					.println("To search the Collection using the Vector Model,        enter 5");
			System.out
					.println("To search the Collection using the Xue QR Model,        enter 6");

			System.out
					.println("To execute all queries using the Xue QR Model,          enter 7");
			System.out
					.println("To print vocabulary,                                    enter 8");
			System.out
					.println("To execute complete test suite,                         enter 9");
			System.out
					.println("To exit the application,                                enter 0");
			System.out
					.println("To search the Collection using the Xue QR Model using IBM,        enter 10");
			System.out
					.println("To execute complete test suite,                         enter 11");
			System.out
					.println("===============================================================");

			System.out.println("Please input your choice, then press enter: ");

			// read the user's menu choice
			choice = UTIL_UserInput.parameterInput(0, 11);

			switch (choice) {

			case 1: {
				createCollection();
				break;
			}

			case 2: {
				openCollection();
				break;
			}

			case 3: {
				closeCollection();
				break;
			}

			case 4: {
				insertDocuments();
				break;
			}

			case 5: {
				doVectorSearch();
				break;
			}

			case 6: {
				doXueSearch();
				break;
			}

			case 7: {
				batchRunXue(col);
				break;
			}

			case 8: {
				printVocab();
				break;
			}

			case 9: {
				executeTestSuite();
				break;
			}
			case 10: {
				doXueIBMSearch();
				break;
			}
			case 11: {
				executeIBMTestSuite();
				break;
			}

			// Exit
			case 0: {
				if (col != null) {
					UTIL_Collections.closeCollection(col);
				}
				System.out.println("Exiting...");
				in.close();
				return;
			}
			}
			System.out.print("Press enter to return to main menu...");
			in.nextLine();

		}
	}

	private void doXueIBMSearch() {
		// TODO Auto-generated method stub
		// loading files of probability map. This could be optimized (do not
		// reload when collection setup doesn't change)
		HashMap<WordPair, Double> pMap;
		HashMap<WordPair, Double> rpMap;
		try {
			FileInputStream fqa = new FileInputStream(new File("qaProb.pmap"));
			FileInputStream faq = new FileInputStream(new File("aqProb.pmap"));
			ObjectInputStream oqa = new ObjectInputStream(fqa);
			ObjectInputStream oaq = new ObjectInputStream(faq);
			pMap = (HashMap<WordPair, Double>) oqa.readObject();
			rpMap = (HashMap<WordPair, Double>) oaq.readObject();

			System.out.println("Map file succesfully loaded");
			if (col != null) {
				if ((xm == null) || (xm.getCollection() != col)) {
					System.out.println("Enter value for delta (0..100):");
					int delta = UTIL_UserInput.parameterInput(0, 100);

					xi = new MDL_XueIBMQR(col, (double) delta / 100, pMap,
							rpMap);
				}

				// Read model parameters from keyboard
				System.out
						.println("Please insert the weight of the question factor (0-100):");
				int alpha = UTIL_UserInput.parameterInput(0, 100);

				int beta = 0;
				if (alpha < 100) {
					System.out
							.println("Please insert the weight of the translation factor (0-"
									+ (100 - alpha) + "):");
					beta = UTIL_UserInput.parameterInput(0, (100 - alpha));
				}

				doInteractiveSearch(xi, new MDL_XueIBMQR.SearchConfig(
						(alpha / 100.0), (beta / 100.0),
						(100 - alpha - beta) / 100.0));
			} else {
				System.out
						.println("There is no collection currently open. Please open a collection first.");
			}
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		}

	}

	/**
	 * Run tests with standard collection for all values of delta
	 * 
	 */
	protected void executeTestSuite() {
		if (col != null) {
			System.out
					.println("Closing current collection to avoid interference.");
			UTIL_Collections.closeCollection(col);
		}

		Properties config = new Properties();

		System.out
				.println("Please insert directory of collection of documents: ");
		config.setProperty(HeadlessExperiment.PROP_DOCUMENT_DIRECTORY,
				UTIL_UserInput.directoryInput());

		System.out
				.print("\nEnter the directory containing test queries to be run: ");
		config.setProperty(HeadlessExperiment.PROP_QUERY_DIRECTORY,
				UTIL_UserInput.directoryInput());

		System.out
				.print("\nEnter number of threads to use from [1, 20] (Larger numbers may be faster, but will consume more memory): ");
		int nThreadNum = UTIL_UserInput.parameterInput(1, 20);
		config.setProperty(
				HeadlessExperiment.PROP_NUMBER_OF_PARALLELL_EXPERIMENTS,
				Integer.toString(nThreadNum));

		try {
			new HeadlessExperiment().run(config);
		} catch (IllegalArgumentException | IOException e) {
			System.err
					.println("Error processing experiment: " + e.getMessage());
			e.printStackTrace();
		}
	}

	protected void executeIBMTestSuite() {
		String folderQueries;
		if (col == null) {
			System.out.println("Requires an open collection ");

		} else {
			System.out
					.print("\nEnter the directory containing test queries to be run: ");
			folderQueries = UTIL_UserInput.directoryInput();
			try {
				new IBMHeadlessExperiment().run(col, folderQueries);
				UTIL_Collections.closeCollection(col);
			} catch (IllegalArgumentException | IOException e) {
				System.err.println("Error processing experiment: "
						+ e.getMessage());
				e.printStackTrace();
			}
		}
	
	}

	protected void printVocab() {
		if (col != null) {
			long start = System.currentTimeMillis();

			// Get all term data
			ArrayList<IX_Term> terms = new ArrayList<IX_Term>(col.getTerms()
					.values());

			// Sort it
			Collections.sort(terms);

			// Print out the whole dictionary
			for (IX_Term term : terms) {
				System.out.print(term.getName() + " (" + term.getCount() + ","
						+ term.getFreq() + "): ");
				for (IX_TermMatch tm : term.getMatches()) {
					System.out.print("(" + tm.getDocid() + "," + tm.getCount()
							+ "," + tm.getAnsid() + ")" + "  ");
				}
				System.out.println("");
			}

			long end = System.currentTimeMillis();
			System.out.println("(execution time " + (end - start) + "msec)");
		} else {
			System.out.println("There is no collection currently open."
					+ " Please open a collection first.");
		}
	}

	protected void doXueSearch() {
		if (col != null) {
			if ((xm == null) || (xm.getCollection() != col)) {
				System.out.println("Enter value for delta (0..100):");
				int delta = UTIL_UserInput.parameterInput(0, 100);

				xm = new MDL_XueQR(col, (double) delta / 100);
			}

			// Read model parameters from keyboard
			System.out
					.println("Please insert the weight of the question factor (0-100):");
			int alpha = UTIL_UserInput.parameterInput(0, 100);

			int beta = 0;
			if (alpha < 100) {
				System.out
						.println("Please insert the weight of the translation factor (0-"
								+ (100 - alpha) + "):");
				beta = UTIL_UserInput.parameterInput(0, (100 - alpha));
			}

			doInteractiveSearch(xm, new MDL_XueQR.SearchConfig((alpha / 100.0),
					(beta / 100.0), (100 - alpha - beta) / 100.0));
		} else {
			System.out
					.println("There is no collection currently open. Please open a collection first.");
		}
	}

	/**
	 * Interactively search based on the given search model and configuration.
	 * 
	 * @param mdl
	 * @param sc
	 */
	private <T extends MDL_GenericModel.SearchConfig> void doInteractiveSearch(
			MDL_GenericModel<T> mdl, T sc) {
		System.out.println("Please insert the search query:");
		String query = UTIL_UserInput.termInput();

		List<RT_Result> results = mdl.search(col.prepare(query), sc);

		if ((results == null) || (results.isEmpty())) {
			System.out
					.println("\nSorry, no documents matching the given terms were found...");
		} else {
			// If there are valid results

			// Initialize the set of patterns found in the results
			ArrayList<String> patternList = new ArrayList<String>();

			// Temp list
			ArrayList<Integer> temp = new ArrayList<Integer>();

			// Print the results
			System.out.println("\n    		Results: " + MDL_GenericModel.topK);
			System.out
					.println("======================================================");
			for (int i = 0; i < results.size() && i < MDL_GenericModel.topK; i++) {

				int id = results.get(i).getDocID();

				// append item to temp list
				temp.add(id);

				double score = results.get(i).getScore();
				Set<String> patterns = mdl.getDocument(
						results.get(i).getDocID()).getPatterns();

				// add the current patterns found in the overall set
				patternList.addAll(patterns);

				// print result list
				System.out.println((i + 1) + ".	Document " + id + " || Score: "
						+ score + "\t|| Patterns: "
						+ ((patterns.size() == 0) ? ("[None]") : (patterns)));
			}
			// print pattern set
			System.out
					.println("\nSet of all patterns found (in descending frequency): ");
			System.out.print("[ " + UTIL_Patterns.sortedPatterns(patternList)
					+ " ]");

			// FIXME: This no longer works since we have stopped making copies
			// read any document IDs from keyboard and open the appropriate
			// files
			// if 0 is read then return to normal
			int choice;
			String path = "collections/" + col.getName() + "/";
			do {
				System.out
						.println("\nEnter any document ID you wish to open or press 0 to return...");
				choice = UTIL_UserInput.parameterInput(0, Integer.MAX_VALUE);
				if (temp.contains(choice)) {
					UTIL_FileOperations.openFileWindow(path + choice + ".json");
				} else if (choice != 0) {
					System.out
							.println("\nThe document ID you inserted was not in the results. Please try again...");
				}
			} while (choice != 0);
		}
	}

	protected void doVectorSearch() {
		if (col != null) {
			if ((vm == null) || (vm.getCollection() != col)) {
				vm = new MDL_Vector(col);
			}

			// Query insert from keyboard
			boolean done = false;
			String query = "";
			System.out.println("Please insert the search query:");
			query = UTIL_UserInput.termInput();

			doInteractiveSearch(vm, null);
		} else {
			System.out
					.println("There is no collection currently open. Please open a collection first.");
		}
	}

	protected void insertDocuments() {
		if (col != null) {
			if (col.insertMultiDocs()) {
				System.out
						.println("All documents ("
								+ col.getCount()
								+ ") from the specified directory were inserted to the collection. ");
			} else {
				System.out
						.println("The directory you specified does not exist. Please try again.");
			}
		} else {
			System.out
					.println("There is no collection currently open. Please open a collection first.");
		}
	}

	protected void closeCollection() {
		if (col != null) {
			UTIL_Collections.closeCollection(col);

			col = null;
			vm = null;
			xm = null;
		} else {
			System.out
					.println("There is no collection currently open. Please open a collection first.");
		}
	}

	protected void openCollection() {
		if (col == null) {
			col = UTIL_Collections.createOrOpenCollection();

			if (col != null) {
				System.out.println("Collection: '" + col.getName() + "' with "
						+ col.getCount() + " documents opened.");
			}
		} else {
			System.out.println("There is a collection already open."
					+ " Please close that collection before opening another.");
		}
	}

	protected void createCollection() {
		openCollection();
	}

	private static void batchRunXue(IX_Collection col) {
		if (col != null) {
			System.out.println("Enter value for delta (0..100):");
			int delta = UTIL_UserInput.parameterInput(0, 100);

			// Read F-measure parameter from keyboard
			System.out
					.println("\nEnter the F-measure factor (5 - 20; larger values weigh recall most):");
			double fLevel = UTIL_UserInput.parameterInput(5, 20) / 10.0;
			System.out.println("\nThis will probably take a minute...");

			System.out
					.println("\nEnter the directory containing test queries to be run: ");
			String sQueryDirectory = UTIL_UserInput.directoryInput();
			ArrayList<RT_Query> queries = JSON_IO
					.retrieveQueries(sQueryDirectory);

			File f = new HeadlessExperiment().batchRunXueNoIO(col, queries,
					(double) delta / 100, fLevel, 4, false);

			if (f != null) {
				System.out.println("\nDone! Results can be found in "
						+ f.getName());
				// Open the file in a system window
				UTIL_FileOperations.openFileWindow(f.getAbsolutePath());
			}

		} else {
			System.out
					.println("There is no collection currently open. Please open a collection first.");
		}
	}

}
