package squirrel.ir.index;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import squirrel.data.json.JSON_Document;
import squirrel.data.json.JSON_Document.Answer;
import squirrel.data.json.JSON_IO;
import squirrel.util.UTIL_Patterns;
import squirrel.util.UTIL_TextClean;

public class IX_Collection implements Serializable {

	/**
	 * A query that has been prepared for use with this collection.
	 * 
	 * @author Steffen Zschaler
	 */
	public class PreparedQuery {

		private String sQuery;
		private Set<IX_Term> terms;

		private PreparedQuery(String sQuery) {
			this.sQuery = sQuery.toLowerCase();

			terms = new HashSet<IX_Term>();

			// Extract the query's terms and perform text cleaning; true for
			// stopword removal
			String[] splits = UTIL_TextClean.cleanText(this.sQuery, true);

			// Remove any terms that are only in the query, but not in the
			// collection
			for (String split : splits) {
				IX_Term t = findTerm(split);
				if (t != null) {
					terms.add(t);
				}
			}
		}

		public String getOrigQuery() {
			return sQuery;
		}

		public Set<IX_Term> getTerms() {
			return Collections.unmodifiableSet(terms);
		}

		public IX_Collection getCollection() {
			return IX_Collection.this;
		}
	}

	private static final long serialVersionUID = -4696351422507686603L;

	private String name;
	private boolean fIsChanged;

	/**
	 * Documents in the collection keyed by document ID.
	 */
	private HashMap<Integer, IX_Document> documents = new HashMap<Integer, IX_Document>();

	/**
	 * Term index. This is essentially an inverted index from terms to
	 * documents, but it also includes frequency of term occurrence in each
	 * document.
	 */
	private HashMap<String, IX_Term> terms = new HashMap<String, IX_Term>();

	/**
	 * Inverted index of question terms. This is a subset of the terms
	 * collection above, only showing the document IDs where a certain term
	 * occurs in the question part. The document IDs are stored in a
	 * {@link UniqueableList} rather than a {@link Set} to improve efficiency
	 * when computing term co-occurrences.
	 */
	private HashMap<String, SortedSet<Integer>> qTerms = new HashMap<String, SortedSet<Integer>>();
	/**
	 * Inverted index of answer terms. This is a subset of the terms collection
	 * above, only showing the document IDs where a certain term occurs in the
	 * answer part. The document IDs are stored in a {@link UniqueableList}
	 * rather than a {@link Set} to improve efficiency when computing term
	 * co-occurrences.
	 */
	private HashMap<String, SortedSet<Integer>> aTerms = new HashMap<String, SortedSet<Integer>>();

	/**
	 * For serialisation purposes.
	 */
	public IX_Collection() {
		super();
	}

	public IX_Collection(String name) throws IOException {
		this.name = name;
		this.fIsChanged = true;

		// create the collections dir if it does not exist already
		File f = new File("collections/");
		f.mkdir();

		// try to create this collection's dir
		f = new File("collections/" + name);
		if (f.isDirectory()) {
			throw new IOException("Collection directory does already exist: "
					+ f);
		} else {
			f.mkdir();
		}

		// create the collection's index file
		File b = new File("collections/" + name + "/index");
		b.createNewFile();
	}

	public int getCount() {
		return documents.size();
	}

	public String getName() {
		return name;
	}

	public HashMap<String, IX_Term> getTerms() {
		return terms;
	}

	public HashMap<Integer, IX_Document> getDocuments() {
		return documents;
	}

	public boolean isChanged() {
		return fIsChanged;
	}

	public void setIsChanged(boolean b) {
		fIsChanged = b;
	}

	// Insert a directory of documents in the collection
	public boolean insertMultiDocs() {

		String dirname = "";
		Scanner in = new Scanner(System.in);

		// read the dir
		System.out.println("Please enter the documents directory:");
		dirname = in.nextLine();

		return insertMultiDocs(dirname);
	}

	public boolean insertMultiDocs(String dirname) {
		// if a dir was given indeed, get the list of files
		File dir = new File(dirname);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();

			// insert all documents one by one
			for (File file : files) {
				analyzeDoc(file);
			}
		} else {
			return false;
		}
		// mark the collection as dirty
		fIsChanged = true;
		return true;
	}

	// analyze the document's contents and insert them in the index
	private void analyzeDoc(File fsource) {

		String fsourcename = fsource.getName();

		// extract the id from the filename
		int docID = Integer.parseInt(fsourcename.replaceFirst("[.][^.]+$", ""));

		// Retrieve the file from JSON to java object
		JSON_Document json = JSON_IO.retrieveDocument(fsource, docID);

		// extract document info
		String url = json.getUrl();
		String title = json.getTitle();
		String ques = json.getQuestion();
		ArrayList<Answer> answers = json.getAnswers();

		// initialize document length counter
		int docCount = 0;

		// processing question title and question text
		String q = title + " " + ques;

		// get the filtered words from question title and text
		// (true for stopword removal)
		String[] qsplits = UTIL_TextClean.cleanText(q, true);

		// increase document length
		docCount += qsplits.length;

		// add TermMatches
		// for questions, we insert answer ID 0
		addTerms(qsplits, docID, 0);

		// keep a comprehensive list of all answer terms
		ArrayList<String> ansplits = new ArrayList<String>();

		// processing answers
		for (Answer ans : answers) {

			// get the filtered words from answer text
			// (true for stopword removal)
			String[] asplits = UTIL_TextClean.cleanText(ans.getAns(), true);

			// increase document length
			docCount += asplits.length;

			// add TermMatches
			// for answers, we insert the current answer ID
			addTerms(asplits, docID, ans.getAnsID());

			// add current terms to overall list
			Collections.addAll(ansplits, asplits);
		}

		Set<String> patterns = UTIL_Patterns.findPatterns(json);

		// creating new Document object
		documents.put(docID, new IX_Document(docID, url, docCount, qsplits,
				patterns));

		// appending to qTerms and aTerms indices
		// if a term already exists, add to it, otherwise add the term
		for (String d : qsplits) {
			SortedSet<Integer> list = null;
			if (qTerms.containsKey(d)) {
				list = qTerms.get(d);
			} else {
				list = new TreeSet<Integer>();
			}
			list.add(docID);
			qTerms.put(d, list);
		}

		for (String a : ansplits) {
			SortedSet<Integer> list = null;
			if (aTerms.containsKey(a)) {
				list = aTerms.get(a);
			} else {
				list = new TreeSet<Integer>();
			}
			list.add(docID);
			aTerms.put(a, list);
		}
	}

	// adding TermMatches for the given terms
	private void addTerms(String[] splits, int docID, int ansID) {

		for (String split : splits) {			
			IX_Term term = terms.get(split);
			if (term != null) {
				// Replace term with new one
				term.addMatch(docID, ansID);
				terms.put(split, term);
			} else {
				// Insert new term
				IX_Term newterm = new IX_Term(split, docID, ansID);
				terms.put(split, newterm);
			}
		}
	}

	// return a document's length by ID
	public int getDocLength(int docID) {
		return documents.get(docID).getCount();
	}

	// return a term object given its name
	public IX_Term findTerm(String termName) {
		return terms.get(termName.toLowerCase());
	}

	public HashMap<String, SortedSet<Integer>> getQTerms() {
		return qTerms;
	}

	public HashMap<String, SortedSet<Integer>> getATerms() {
		return aTerms;
	}

	/**
	 * Prepare a query for subsequent use by the search function.
	 */
	public PreparedQuery prepare(String sQuery) {
		return new PreparedQuery(sQuery);
	}
}
