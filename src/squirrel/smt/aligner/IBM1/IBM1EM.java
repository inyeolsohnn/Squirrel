package squirrel.smt.aligner.IBM1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import squirrel.data.json.JSON_Document;
import squirrel.data.json.JSON_IO;
import squirrel.data.json.JSON_Document.Answer;
import squirrel.ir.index.IX_Collection;
import squirrel.ir.index.IX_Document;
import squirrel.util.UTIL_FileOperations;
import squirrel.util.UTIL_Patterns;
import squirrel.util.UTIL_TextClean;

public class IBM1EM {

	// retrieve qa pairs from current collection, turn them into bitext pairs
	// run IBM1 for all pairs
	// save the translation probability data into a file

	private boolean doExtract = false;
	public static int NUM_ITERATIONS = 15;
	public static double DELTA = 5.0;

	// analyze the document's contents and insert them in the index

	public static void whereAmI() {
		System.out.println("Working Directory = "
				+ System.getProperty("user.dir"));
	}

	public static boolean computeIBMForDir() {
		ArrayList<Bitext> forwardBitext = (ArrayList<Bitext>) UTIL_FileOperations
				.openObject("forward.bitext");
		ArrayList<Bitext> reverseBitext = (ArrayList<Bitext>) UTIL_FileOperations
				.openObject("reverse.bitext");
		return UTIL_FileOperations.store(train(forwardBitext), "qaProb.pmap")
				&& UTIL_FileOperations.store(train(reverseBitext),
						"aqProb.pmap");

	}

	public static HashMap<WordPair, Double> train(ArrayList<Bitext> bitextList) {
		HashSet<String> sourceWords = new HashSet<String>();
		HashSet<String> targetWords = new HashSet<String>();
		HashMap<WordPair, Double> pMap = new HashMap<WordPair, Double>();

		initialiseWords(bitextList, sourceWords, targetWords);
		initialiseProbabilityMap(bitextList, pMap, targetWords);

		double previousPerplexity = 0;
		double perplexity = 0;
		for (int i = 0; i < NUM_ITERATIONS; i++) {

			Double diff = Math.abs(previousPerplexity) - Math.abs(perplexity);

			if (i > 1) {
				if (!Double.isInfinite(diff) && Math.abs(diff) - DELTA < 0.1) {
					System.out.println("probability converged");
					break;
				}
			}

			double perplexitySum = 0;
			System.out.println("Iteration : " + i);

			HashMap<String, Double> total = new HashMap<String, Double>();
			HashMap<String, Double> s_total = new HashMap<String, Double>();

			// count(e|f) = 0 for all e,f
			HashMap<WordPair, Double> count = newCountMap(bitextList);

			// total(f) = 0 for all f
			for (String a : targetWords) {
				total.put(a, (double) 0);
			}

			// also needs to get a data structure (bitext id
			for (Bitext bt : bitextList) {
				String source[] = bt.getSource();
				String target[] = bt.getTarget();

				for (String q : source) {
					s_total.put(q, (double) 0);
					for (String a : target) {
						Double prob = pMap.get(new WordPair(q, a));
						if (prob == null)
							prob = (double) 0;
						s_total.put(q, s_total.get(q) + prob);
					}
				}
				for (String q : source) {
					for (String a : target) {
						count.put(
								new WordPair(q, a),
								count.get(new WordPair(q, a))
										+ (pMap.get(new WordPair(q, a)) / s_total
												.get(q)));
						total.put(
								a,
								total.get(a)
										+ (pMap.get(new WordPair(q, a)) / s_total
												.get(q)));
					}
				}
			}

			// Estimate probabilities
			for (String a : targetWords) {
				for (String q : sourceWords) {
					if (pMap.get(new WordPair(q, a)) != null) {
						pMap.put(new WordPair(q, a),
								count.get(new WordPair(q, a)) / total.get(a));
					}
				}
			}

			// Computation of perplexity

			previousPerplexity = perplexity;
			for (Bitext bt : bitextList) {
				String source[] = bt.getSource();
				String target[] = bt.getTarget();
				double probability = (double) 1.0;
				for (String q : source) {
					double sum = 0;
					for (String a : target) {
						if (pMap.get(new WordPair(q, a)) != null) {
							sum += pMap.get(new WordPair(q, a));
						} else
							System.out.println("!!!???!");
					}
					probability *= sum;
				}
				double mul = Math.log10(probability);
				mul = Math.abs(mul);

				perplexitySum += Math.log10(probability) / Math.log10(2);

			}

			perplexitySum = 0 - perplexitySum;
			perplexity = perplexitySum;

			System.out.println("log(Perplexity)= " + perplexity);

		}

		return pMap;

	}

	private static void initialiseWords(ArrayList<Bitext> bitextList,
			HashSet<String> sourceWords, HashSet<String> targetWords) {

		for (Bitext bt : bitextList) {

			String[] question = bt.getSource();
			String[] answer = bt.getTarget();
			for (int i = 0; i < question.length; i++)
				sourceWords.add(question[i]);

			for (int i = 0; i < answer.length; i++)
				targetWords.add(answer[i]);
		}

		System.out.println(" Question size: " + sourceWords.size()
				+ "Answer size: " + targetWords.size());
	}

	private static void initialiseProbabilityMap(ArrayList<Bitext> bitextList,
			HashMap<WordPair, Double> pMap, HashSet<String> targetWords) {

		Double initialValue = (double) (1.0 / targetWords.size());

		for (Bitext bt : bitextList) {
			String[] question = bt.getSource();
			String[] answer = bt.getTarget();
			for (int i = 0; i < question.length; i++) {
				for (int j = 0; j < answer.length; j++) {
					pMap.put(new WordPair(question[i], answer[j]), initialValue);
				}
			}
		}

		System.out.println("Probability map size: " + pMap.size());
	}

	private static HashMap<WordPair, Double> newCountMap(
			ArrayList<Bitext> bitextList) {

		HashMap<WordPair, Double> countMap = new HashMap<WordPair, Double>();

		for (Bitext bt : bitextList) {
			String[] question = bt.getSource();
			String[] answer = bt.getTarget();
			for (int i = 0; i < question.length; i++) {
				for (int j = 0; j < answer.length; j++) {
					countMap.put(new WordPair(question[i], answer[j]),
							(double) 0);
				}
			}
		}

		return countMap;
	}

	public static void extractViterbi(HashMap<WordPair, Double> forwardMap,
			HashMap<WordPair, Double> backwardMap,
			ArrayList<Bitext> fwbitextList, ArrayList<Bitext> bwbitextList) {
		// one to one alignment
		for (int k = 0; k < 2; k++) {
			System.out.println("k==" + k);
			HashMap<WordPair, Double> currentMap = forwardMap;
			String directoryName = "forwardAlignment.vPath";
			ArrayList<Bitext> bitextList = fwbitextList;
			HashMap<Bitext, AlignPair> corpusAlignment = new HashMap<Bitext, AlignPair>();
			if (k == 1) {
				currentMap = backwardMap;
				directoryName = "backwardAlignment.vPath";
				bitextList = bwbitextList;
			}
			int btcounter = 0;
			for (Bitext bt : bitextList) {
				System.out.println("bitext counter: " + btcounter++);

				String[] question = bt.getSource();
				String[] answer = bt.getTarget();
				ArrayList<WordPair> candidates = new ArrayList<WordPair>();
				for (int i = 0; i < question.length; i++) {
					ArrayList<WordPair> allPairs = new ArrayList<WordPair>();
					// all the word pairs for the current source word
					for (int j = 0; j < answer.length; j++) {
						allPairs.add(new WordPair(question[i], answer[j]));
					}
					Double highest = 0d;
					ArrayList<WordPair> sourceAlignments = new ArrayList<WordPair>();
					System.out.println("Current source word: " + question[i]);
					for (WordPair wp : allPairs) {
						System.out.println("Pair: " + wp.e + "-" + wp.f);
						Double currentProb = currentMap.get(wp);
						if (currentProb == null) {
							System.out.println("Null probability Pair: " + wp.e
									+ "-" + wp.f);
						} else {
							if (currentProb > highest) {
								sourceAlignments.clear();
								highest = currentProb;
								sourceAlignments.add(wp);
							} else if (currentProb == highest) {
								if (sourceAlignments.size() == 1) {
									WordPair cb = sourceAlignments.get(0);
									if (cb.equals(wp)) {
										System.out.println("same pair");
									} else {
										System.out
												.println("Conflicting with pair: "
														+ wp.e + "-" + wp.f);
									}
								}

							}
						}
					}
					if (sourceAlignments.size() != 0) {
						System.out.println("Added " + sourceAlignments.get(0).e
								+ "-" + sourceAlignments.get(0).f);
						candidates.add(sourceAlignments.get(0));
					}
				}
				corpusAlignment.put(bt, new AlignPair(bt, candidates));
			}
			UTIL_FileOperations.store(corpusAlignment, directoryName);
		}
	}
}
