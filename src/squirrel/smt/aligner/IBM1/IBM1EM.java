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

import squirrel.data.json.JSON_Document;
import squirrel.data.json.JSON_IO;
import squirrel.data.json.JSON_Document.Answer;
import squirrel.ir.index.IX_Collection;
import squirrel.ir.index.IX_Document;
import squirrel.util.UTIL_Patterns;
import squirrel.util.UTIL_TextClean;

public class IBM1EM {

	// retrieve qa pairs from current collection, turn them into bitext pairs
	// run IBM1 for all pairs
	// save the translation probability data into a file

	public static int NUM_ITERATIONS = 10;
	public static double DELTA = 5.0;

	// analyze the document's contents and insert them in the index

	public static void whereAmI() {
		System.out.println("Working Directory = "
				+ System.getProperty("user.dir"));
	}

	public static boolean computeIBMForDir() {

		ArrayList<Bitext> forwardBitext = null;
		ArrayList<Bitext> reverseBitext = null;
		String dirName = "";
		Scanner in = new Scanner(System.in);

		// read the dir
		System.out.println("Please enter the documents directory:");
		dirName = in.nextLine();
		File dir = new File(dirName);
		if (dir.isDirectory()) {
			File[] files = dir.listFiles();
			forwardBitext = new ArrayList<Bitext>();
			reverseBitext = new ArrayList<Bitext>();
			// create bitext pairs from the directory and store them in array
			// list
			// example q1-a1,a2,a3 is turned into 3 pairs of bitext q1a1, q1a2,
			// q1a3

			for (File file : files) {
				int docID = Integer.parseInt(file.getName().replaceFirst(
						"[.][^.]+$", ""));
				JSON_Document json = JSON_IO.retrieveDocument(file, docID);

				// extract document info
				String url = json.getUrl();
				String title = json.getTitle();
				String ques = json.getQuestion();
				ArrayList<Answer> answers = json.getAnswers();
				String q = title + " " + ques;
				String[] qsplits = UTIL_TextClean.cleanText(q, true);

				ArrayList<String> ansplits = new ArrayList<String>();
				for (Answer ans : answers) {

					// get the filtered words from answer text
					// (true for stopword removal)
					String[] asplits = UTIL_TextClean.cleanText(ans.getAns(),
							true);

					// create new Bitext and add to @bitextList

					// Xue pooling strategy
					/*
					 * bitextList.add(new Bitext(qsplits, asplits));
					 * bitextList.add(new Bitext(asplits, qsplits));
					 */

					// currently unidirectional q->a
					forwardBitext.add(new Bitext(qsplits, asplits));
					reverseBitext.add(new Bitext(asplits, qsplits));
				}

			}
		}

		// train and store the pairs
		return store(train(forwardBitext), "qaProb.pmap")
				&& store(train(reverseBitext), "aqProb.pmap");

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

	private static boolean store(HashMap<WordPair, Double> pMap, String dn) {
		// TODO Auto-generated method stub
		if (pMap == null)
			return false;
		else {
			try {
				FileOutputStream fileOut = new FileOutputStream(dn);
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
				out.writeObject(pMap);
				out.close();
				fileOut.close();
				System.out.printf("map saved as " + dn);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return false;
			}
			return true;
		}
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

}