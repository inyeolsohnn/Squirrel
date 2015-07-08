package squirrel.ir.retrieve.models;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import squirrel.ir.IRQualityMetrics;
import squirrel.ir.index.IX_Collection;
import squirrel.ir.index.IX_Document;
import squirrel.ir.index.IX_Term;
import squirrel.ir.index.IX_TermMatch;
import squirrel.ir.retrieve.RT_Query;
import squirrel.ir.retrieve.RT_Result;

public class MDL_XueQR extends MDL_GenericModel<MDL_XueQR.SearchConfig> {

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

	// lambda parameter for background smoothing
	private double lambda = 0.001; // = 0.1;

	// Added [2012-10-24] -- Weighting factor for QA vs AQ probabilities
	private double delta = 0.5d;

	public MDL_XueQR(IX_Collection col, double delta) {
		super(col);
		this.delta = delta;
	}

	/**
	 * Benchmark search the given query.
	 * 
	 * @param query
	 *            query to be executed
	 * 
	 * @return statistics on quality of search results
	 */
	public IRQualityMetrics search(RT_Query query, SearchConfig sc) {
		// process the query
		List<RT_Result> results = search(query.getPreparedQuery(col), sc);

		// check for errors in query processing and if results exist
		if ((results == null) || results.isEmpty()) {
			return null;
		}

		// initialize the measurement values
		double found = 0;
		Set<String> allPatterns = new TreeSet<String>();
		Map<String, Double> patternRanks = new HashMap<String, Double>();

		// [2013-07-08: Added support for computing R-precision
		// loop through the documents found
		double foundRPrecise = 0;
		int currentRank = 0;
		int numRelevant = query.getRelevant().size();
		for (RT_Result rCurrent : results) {
			Set<String> patterns = getDocument(
					results.get(currentRank).getDocID()).getPatterns();

			if (query.getRelevant().contains(rCurrent.getDocID())) {
				// Found a relevant document in the results returned
				if (numRelevant > currentRank) {
					foundRPrecise++;
				}
				if (topK > currentRank) {
					found++;
				}
			}

			if (topK > currentRank) {
				// Identify the patterns found
				allPatterns.addAll(patterns);
			}

			// Create ranked list of patterns found
			for (String pattern : patterns) {
				double patternRank;
				Double dPatternRank = patternRanks.get(pattern);
				if (dPatternRank != null) {
					patternRank = dPatternRank;
				} else {
					patternRank = 0;
				}
				patternRank += 1.0d / (currentRank + 1); // +1 to avoid division by
														// zero
				patternRanks.put(pattern, patternRank);
			}

			currentRank++;
		}
		Set<String> foundPatterns = new TreeSet<String>(query.getPatterns());
		foundPatterns.retainAll(allPatterns);

		int numRelevantPatterns = query.getPatterns().size();
		int numFoundPatterns = 0;
		List<Map.Entry<String, Double>> rankedPatterns = new ArrayList<Map.Entry<String, Double>>(
				patternRanks.entrySet());
		Collections.sort(rankedPatterns,
				new Comparator<Map.Entry<String, Double>>() {
					@Override
					public int compare(Entry<String, Double> l,
							Entry<String, Double> r) {
						return l.getValue().compareTo(r.getValue());
					}
				});
		for (int i = 0; i < numRelevantPatterns; i++) {
			if (query.getPatterns().contains(rankedPatterns.get(i).getKey())) {
				numFoundPatterns++;
			}
		}

		// calculate measurements
		// TODO Add correct calculation of r-precision of patterns
		return new IRQualityMetrics(query, topK, ((double) found / topK),
				((double) foundPatterns.size() / allPatterns.size()),
				((double) found / query.getRelevant().size()),
				((double) foundPatterns.size() / query.getPatterns().size()),
				((double) foundRPrecise / numRelevant),
				((double) numFoundPatterns / numRelevantPatterns));
	}

	@Override
	protected List<RT_Result> internalSearch(IX_Collection.PreparedQuery pq,
			SearchConfig sc) {

		// Lazily compute plin values for any query terms for which they haven't
		// yet been computed
		synchronized (this) {
			if (plin == null) {
				plin = new HashMap<String, Map<String, Double>>();
				_plin = Collections.synchronizedMap(plin);
			}
			for (IX_Term term : pq.getTerms()) {
				if (!plin.containsKey(term.getName())) {
					// System.out.println("Lazily computing plin values for "
					// + term.getName());
					Map<String, Map<String, Double>> mpQA = new HashMap<String, Map<String, Double>>();
					computeTranslationProbabilityRow(term.getName(), col
							.getQTerms().get(term.getName()), col.getATerms(),
							mpQA);

					Map<String, Map<String, Double>> mpAQ = new HashMap<String, Map<String, Double>>();
					computeTranslationProbabilityRow(term.getName(), col
							.getATerms().get(term.getName()), col.getQTerms(),
							mpAQ);

					computePLinFor(term.getName(), mpQA, mpAQ);
				}
			}
		}
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

	private double Pwt(String w, String t) {
		doCalcPLin();
		return findPlin(w, t);
	}

	/**
	 * Compute P_lin values (the combined probabilty of semantic relation
	 * between two terms wi and wj) for all terms in the collection and return
	 * the appropriate matrix. This is Panos' method, but with correct
	 * normalisation of probabilities.
	 */
	public synchronized void doCalcPLin() {

		if (plin != null) {
			return;
		}

		plin = new HashMap<String, Map<String, Double>>();
		_plin = Collections.synchronizedMap(plin);

		// calculate all pQA and pAQ probabilities for all term couples

		long nTime = System.currentTimeMillis();
		System.out.println(delta
				+ ": Calculating QA translation probabilities...");
		Map<String, Map<String, Double>> pQA = computeDirectTranslationProbabilities(
				col.getQTerms(), col.getATerms());
		System.out.println(delta + ": Done in "
				+ (System.currentTimeMillis() - nTime) + " milliseconds.");

		nTime = System.currentTimeMillis();
		System.out.println(delta
				+ ": Calculating AQ translation probabilities...");
		Map<String, Map<String, Double>> pAQ = computeDirectTranslationProbabilities(
				col.getATerms(), col.getQTerms());
		System.out.println(delta + ": Done in "
				+ (System.currentTimeMillis() - nTime) + " milliseconds.");

		nTime = System.currentTimeMillis();
		System.out.println(delta + ": Combining P_lin values...");

		// Compute combined P_lin values
		for (String currentTerm : col.getTerms().keySet()) {
			computePLinFor(currentTerm, pQA, pAQ);
		}

		System.out.println(delta + ": Done in "
				+ (System.currentTimeMillis() - nTime) + " milliseconds ("
				+ plin.size() + ") p_LIN values.");
	}

	/**
	 * Compute plin for one source term.
	 * 
	 * @param srcTerm
	 *            the source term for which to compute plin values.
	 * 
	 * @param pQA
	 *            direct question--answer mappings; must contain mappings for at
	 *            least srcTerm
	 * 
	 * @param pAQ
	 *            direct answer--question mappings; must contain mappings for at
	 *            least srcTerm
	 */
	private void computePLinFor(final String srcTerm,
			Map<String, Map<String, Double>> pQA,
			Map<String, Map<String, Double>> pAQ) {
		final Map<String, Double> mpQA = pQA.get(srcTerm);
		final Map<String, Double> mpAQ = pAQ.get(srcTerm);

		final Set<String> allTranslations = new HashSet<String>();
		if (mpQA != null) {
			allTranslations.addAll(mpQA.keySet());
		} else {
			// Make sure we always consider self-translation
			allTranslations.add(srcTerm);
		}
		if (mpAQ != null) {
			allTranslations.addAll(mpAQ.keySet());
		} else {
			// Make sure we always consider self-translation
			allTranslations.add(srcTerm);
		}

		ExecutorService es = Executors.newFixedThreadPool(4);
		List<Future<Double>> lfdSumResults = new LinkedList<Future<Double>>();
		for (String transTerm : allTranslations) {
			final String _transTerm = transTerm;
			lfdSumResults.add(es.submit(new Callable<Double>() {
				@Override
				public Double call() throws Exception {
					double pqa = getProbability(mpQA);
					double paq = getProbability(mpAQ);

					double pLin = (1 - delta) * pqa + delta * paq;

					if (pLin > 0) {
						Map<String, Double> currTermMap;
						synchronized (_plin) {
							currTermMap = _plin.get(srcTerm);

							if (currTermMap == null) {
								currTermMap = Collections
										.synchronizedMap(new HashMap<String, Double>());
								_plin.put(srcTerm, currTermMap);
							}
						}
						currTermMap.put(_transTerm, pLin);

						return pLin;
					}

					return 0.0d;
				}

				public double getProbability(Map<String, Double> matrix) {
					if (matrix != null) {
						Double d = matrix.get(_transTerm);
						if (d != null) {
							return d;
						}
					} else if (_transTerm.equals(srcTerm)) {
						// Special case: If there are no translations at all
						// for currentTerm, make sure we use a probability
						// of 1 for the self-translation case
						return 1.0d;
					}
					return 0.0d;
				}
			}));

		}

		es.shutdown();
		double sum = 0;
		for (Future<Double> f : lfdSumResults) {
			try {
				sum += f.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		if (Math.ulp(1) < Math.abs(1 - sum)) {
			// This should never be printed!
			System.out.println("Overall pLin sum for " + srcTerm + " (not 1): "
					+ sum);
		}
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
	public double findPlin(String w, String t) {
		Map<String, Double> wRow = plin.get(w);
		if (wRow != null) {
			Double dVal = wRow.get(t);

			if (dVal != null) {
				return dVal;
			}
		}

		return 0.0d;
	}

	/**
	 * Compute direct translation probabilities from srcTerms to tgtTerms. Note
	 * that the result will only contain translations for terms in srcTerms. It
	 * will contain normalised probabilities, so if there are no co-occurences
	 * at all, it will set the self-translation probability to one.
	 * 
	 * @param tgtTerms
	 * @param srcTerms
	 * @return
	 */
	private Map<String, Map<String, Double>> computeDirectTranslationProbabilities(
			Map<String, SortedSet<Integer>> tgtTerms,
			Map<String, SortedSet<Integer>> srcTerms) {
		final Map<String, Map<String, Double>> mpResult = Collections
				.synchronizedMap(new HashMap<String, Map<String, Double>>());

		// iterate through all src terms
		for (Map.Entry<String, SortedSet<Integer>> srcTermIdx : srcTerms
				.entrySet()) {
			computeTranslationProbabilityRow(srcTermIdx.getKey(),
					srcTermIdx.getValue(), tgtTerms, mpResult);
		}

		return mpResult;
	}

	/**
	 * Compute direct translation probabilities for one source term, using the
	 * given term index as the translation target.
	 * 
	 * @param srcTerm
	 *            the source term for which to compute direct translation
	 *            probabilities
	 * @param srcTermDocumentIDs
	 *            list of ids of documents in which srcTerm occurs
	 * @param tgtTerms
	 *            translation target term data
	 * @param mpResult
	 *            translation probability map to which results should be added.
	 */
	private void computeTranslationProbabilityRow(final String srcTerm,
			final SortedSet<Integer> srcTermDocumentIDs,
			Map<String, SortedSet<Integer>> tgtTerms,
			final Map<String, Map<String, Double>> mpResult) {

		class MPHolder {
			private Map<String, Double> mp = mpResult.get(srcTerm);

			public Map<String, Double> getMP() {
				return mp;
			}

			public synchronized void setMP(Map<String, Double> mp) {
				if (this.mp != null) {
					throw new ConcurrentModificationException(
							"You really messed up: mp wasn't null!");
				}

				this.mp = mp;
			}
		}
		final MPHolder mph = new MPHolder();

		ExecutorService es = Executors.newFixedThreadPool(4);
		List<Future<Long>> lflSumResults = new LinkedList<Future<Long>>();

		// iterate through all tgt terms
		for (Map.Entry<String, SortedSet<Integer>> tgtTermIdx : tgtTerms
				.entrySet()) {
			final Map.Entry<String, SortedSet<Integer>> _tgtTermIdx = tgtTermIdx;
			lflSumResults.add(es.submit(new Callable<Long>() {

				@Override
				public Long call() throws Exception {
					String tgtTerm = _tgtTermIdx.getKey();

					/*
					 * Count the number of documents in both source and target
					 * collection; that is, the number of co-occurrences of the
					 * two terms. Keep in mind both lists are unique and sorted.
					 * 
					 * This algorithm should do the same thing as set
					 * intersection, but hopefully be more efficient as it won't
					 * have to keep copying sets about.
					 */
					long nTranslate = 0;
					if (srcTermDocumentIDs != null) {
						Iterator<Integer> iTgt = _tgtTermIdx.getValue()
								.iterator();
						Integer curTgt = null;
						if (iTgt.hasNext()) {
							curTgt = iTgt.next();

							for (Integer curSrc : srcTermDocumentIDs) {
								do {
									int cmp = curSrc.compareTo(curTgt);

									if (cmp == 0) {
										nTranslate++;
										if (iTgt.hasNext()) {
											curTgt = iTgt.next();
										} else {
											curTgt = null;
										}

										break;
									} else if (cmp > 0) {
										// advance curTgt, but not curSrc
										if (iTgt.hasNext()) {
											curTgt = iTgt.next();
										} else {
											curTgt = null;
										}
									} else {
										// advance curSrc
										break;
									}
								} while (curTgt != null);
								if (curTgt == null) {
									break;
								}
							}
						}
					}

					if (nTranslate > 0) {
						synchronized (mph) {
							if (mph.getMP() == null) {
								mph.setMP(Collections
										.synchronizedMap(new HashMap<String, Double>()));
								mpResult.put(srcTerm, mph.getMP());
							}
						}

						mph.getMP().put(tgtTerm, (double) nTranslate);
						return nTranslate;
					}

					return 0L;
				}
			}));
		}

		es.shutdown();

		long sum = 0;
		for (Future<Long> f : lflSumResults) {
			try {
				// Note this will wait for the computation to finish, so the
				// loop as a whole will wait for the executor service to
				// finish
				sum += f.get();
			} catch (ExecutionException e) {
				e.printStackTrace();
			} catch (InterruptedException ie) {
				ie.printStackTrace();
			}
		}
		// Now normalise
		if (sum > 0) {
			double debugSum = 0;
			for (Map.Entry<String, Double> entry : mph.getMP().entrySet()) {
				entry.setValue(entry.getValue() / sum);
				debugSum += entry.getValue();
			}
			if (Math.ulp(1) < Math.abs(1 - debugSum)) {
				// This should never be printed!
				System.out.println("Overall probability sum (not 1): "
						+ debugSum);
			}
		} else {
			// We didn't even find the self-translation. Artificially add it
			// to guarantee sum-of-probabilities invariant
			mph.setMP(new HashMap<String, Double>());
			mpResult.put(srcTerm, mph.getMP());
			mph.getMP().put(srcTerm, 1.0d);
		}
	}
}