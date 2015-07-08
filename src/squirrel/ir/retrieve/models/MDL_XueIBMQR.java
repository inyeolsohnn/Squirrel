package squirrel.ir.retrieve.models;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import squirrel.ir.index.IX_Collection;
import squirrel.ir.index.IX_Document;
import squirrel.ir.index.IX_Term;
import squirrel.ir.index.IX_TermMatch;
import squirrel.ir.index.IX_Collection.PreparedQuery;
import squirrel.ir.retrieve.RT_Result;
import squirrel.ir.retrieve.models.MDL_XueQR.SearchConfig;
import squirrel.smt.aligner.IBM1.WordPair;


public class MDL_XueIBMQR extends MDL_GenericModel<MDL_XueIBMQR.SearchConfig> {

	public static class SearchConfig extends MDL_GenericModel.SearchConfig {
		public double alpha, beta, gamma;

		public SearchConfig(double alpha, double beta, double gamma) {
			this.alpha = alpha;
			this.beta = beta;
			this.gamma = gamma;
		}
	}

	// Plin probabilities
	// private HashMap<IX_TermPair, Double> plin;

	private Map<String, Map<String, Double>> plin;
	// a synchronized view on plin
	private Map<String, Map<String, Double>> _plin;

	// q-a
	private HashMap<WordPair, Double> pMap = null;

	// a-q
	private HashMap<WordPair, Double> rpMap = null;

	// lambda parameter for background smoothing
	private double lambda = 0.001; // = 0.1;

	// Added [2012-10-24] -- Weighting factor for QA vs AQ probabilities
	private double delta = 0.5d;

	public MDL_XueIBMQR(IX_Collection col, double delta) {
		super(col);
		this.delta = delta;
		// Currently the process is based on one probability.pmap file. Relation
		// between collection - probability map file needs to be specified

		FileInputStream fqa;
		FileInputStream faq;
		try {
			fqa = new FileInputStream(new File("qaProb.pmap"));
			faq = new FileInputStream(new File("aqProb.pmap"));
			ObjectInputStream oqa = new ObjectInputStream(fqa);
			ObjectInputStream oaq = new ObjectInputStream(faq);
			pMap = (HashMap<WordPair, Double>) oqa.readObject();
			rpMap = (HashMap<WordPair, Double>) oaq.readObject();

			System.out.println("Map file succesfully loaded");
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		}
		
	}

	private double PwD(String w, int docID, SearchConfig sc) {

		double score;
		int length = col.getDocLength(docID);

		// P(w|(q,a)) = (|(q,a)| / |(q,a)| + lambda) * Pmx(w|(q,a)) +
		// (lambda / lambda + |(q,a)|) * Pml(w|C)
		score = ((length / (length + lambda)) * Pmxwqa(w, docID, sc))
				+ ((lambda / (lambda + length)) * PmlwC(w));

		return score;
	}

	private double PmlwC(String w) {

		double freq = col.getTerms().get(w).getCount();
		double length = col.getCount();

		return freq / length;
	}

	private double Pmlwq(String w, int docID) {

		double freq = 0;

		loop: for (IX_TermMatch tm : col.findTerm(w).getMatches()) {

			// We're looking for the q frequency (ansID = 0)
			if ((tm.getDocid() == docID) && (tm.getAnsid() == 0)) {
				freq = tm.getCount();
				break loop; //
			} else if (tm.getDocid() > docID) {
				break loop;
			}
		}

		double length = col.getDocLength(docID);
		double result = freq / length;

		return result;
	}

	private double Pmlwa(String w, int docID) {

		double freq = 0;

		loop: for (IX_TermMatch tm : col.findTerm(w).getMatches()) {

			// We're looking for the a frequency (ansID > 0)
			if ((tm.getDocid() == docID) && (tm.getAnsid() > 0)) {
				freq += tm.getCount();
			} else if (tm.getDocid() > docID) {
				break loop;
			}
		}

		double length = col.getDocLength(docID);

		return freq / length;
	}

	private double Pmxwqa(String w, int docID, SearchConfig sc) {

		// 誇 t溝Q P(w|t) * Pml(t|q)
		double sum = 0.0;
		String[] Qt = col.getDocuments().get(docID).getqTerms();
		for (String t : Qt) {
			sum += Pwt(w, t) * Pmlwq(t, docID);
		}
		// *** modification of the original model to account for
		// excessive document lengths ***
		sum /= Qt.length;

		// Pmx(w|(q,a)) = alpha * Pml(w|q) + beta * 誇 t溝q P(w|t) *
		// Pml(t|q) + gamma * Pml(w|a)f
		double result = sc.alpha * Pmlwq(w, docID) + sc.beta * sum + sc.gamma
				* Pmlwa(w, docID);

		return result;
	}

	private Double Pwt(String w, String t) {

		// currently unidirectional p(q|a) only
		WordPair nwp = new WordPair(w, t);
		Double pwt = pMap.get(nwp);
		Double twp = rpMap.get(nwp);
		if (pwt == null)
			pwt = 0.0d;
		if (twp == null)
			twp = 0.0d;

		return pwt * delta + (1 - delta) * twp;
	}

	@Override
	protected List<RT_Result> internalSearch(IX_Collection.PreparedQuery pq,
			SearchConfig sc) {

		// Accumulator for results: maps doc ids to RT_Result data
		@SuppressWarnings("serial")
		class Accumulator extends HashMap<Integer, RT_Result> {
			/**
			 * Lazily creates result data.
			 * 
			 * @param docID
			 * @param score
			 */
			public void addResult(int docID, double score) {
				RT_Result rt = get(docID);
				if (rt == null) {
					// Initialise as 1, as score will be multiplied...
					rt = new RT_Result(docID, 1);
				}

				// TODO: Verify that I need to multiply here
				// �� w溝Q P(w|(q,a))
				rt.mulScore(score);

				// P(Q|(q,a)) = �� w溝Q P(w|(q,a))
				put(docID, rt);

			}
		}

		Accumulator acc = new Accumulator();

		// Iterate through all documents in the collection
		for (IX_Document doc : col.getDocuments().values()) {

			// Iterate through all terms in the query
			for (IX_Term t : pq.getTerms()) {
				// P(w|(q,a))
				double score = PwD(t.getName(), doc.getDocID(), sc);

				acc.addResult(doc.getDocID(), score);
			}
		}

		// Clean invalid results
		// initial size set to tradeoff speed and memory wastage
		List<RT_Result> results = new ArrayList<RT_Result>(acc.size() / 2);
		for (RT_Result r : acc.values()) {
			if (r.getScore() > 0.0)
				results.add(r);
		}

		return results;

	}

	public double getDelta() {
		return delta;
	}

	public void setDelta(double delta) {
		this.delta = delta;
	}

	/**
	 * Find a Plin given the two terms it refers to. Note that this will never
	 * attempt to lazily compute plin values; doing so would be highly
	 * inefficient at this point.
	 * 
	 * @param w
	 *            source word
	 * @param t
	 *            target word
	 * @return plin value
	 */

}
