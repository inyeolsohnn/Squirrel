package squirrel.smt.aligner.IBM1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import squirrel.util.UTIL_FileOperations;
import squirrel.util.UTIL_UserInput;

public class PhraseExtractor {
	public static int pc = 0;
	public static int tp = 0;

	public static void findViterbiPath() {
		try {
			System.out.println("name of forward bitext");
			String fwBitext = UTIL_UserInput.fileNameInput();
			ArrayList<Bitext> fwList = (ArrayList<Bitext>) UTIL_FileOperations
					.openObject(fwBitext);

			System.out.println("name of backward bitext");
			String bwBitext = UTIL_UserInput.fileNameInput();
			ArrayList<Bitext> bwList = (ArrayList<Bitext>) UTIL_FileOperations
					.openObject(bwBitext);

			System.out.println("name of forward map of the corpus");
			String fwm = UTIL_UserInput.fileNameInput();
			HashMap<WordPair, Double> fwMap = (HashMap<WordPair, Double>) UTIL_FileOperations
					.openObject(fwm);

			System.out.println("name of backward map of the corpus");
			String bwm = UTIL_UserInput.fileNameInput();
			HashMap<WordPair, Double> bwMap = (HashMap<WordPair, Double>) UTIL_FileOperations
					.openObject(bwm);

			IBM1EM.extractViterbi(fwMap, bwMap, fwList, bwList);
		} catch (Exception e) {
			e.printStackTrace(System.out);
		}
	}

	public static void extractPhrase() {

		System.out.println("name of forward alignment");
		String fwA = UTIL_UserInput.fileNameInput();
		HashMap<Bitext, AlignPair> currentMap = (HashMap<Bitext, AlignPair>) UTIL_FileOperations
				.openObject(fwA);
		System.out.println("name of backward alignment");
		String bwA = UTIL_UserInput.fileNameInput();
		HashMap<Bitext, AlignPair> bwAlignment = (HashMap<Bitext, AlignPair>) UTIL_FileOperations
				.openObject(bwA);
		String mapName = "forward.aMap";
		for (int i = 0; i < 2; i++) {
			if (i == 1) {
				currentMap = bwAlignment;
				mapName = "reverse.aMap";
			}
			// Iterator<Entry<Bitext, AlignPair>> fIt = fwAlignment.entrySet()
			// .iterator();
			HashMap<Bitext, HashMap<Integer, Integer>> alignmentMap = new HashMap<Bitext, HashMap<Integer, Integer>>();
			Iterator<Entry<Bitext, AlignPair>> cait = currentMap.entrySet()
					.iterator();
			while (cait.hasNext()) {
				HashMap<Integer, Integer> sourceTargetPair = new HashMap<Integer, Integer>();
				Map.Entry<Bitext, AlignPair> pair = cait.next();
				AlignPair forwardAlign = pair.getValue();
				Bitext currentBitext = pair.getKey();
				AlignPair backwardAlign = bwAlignment.get(currentBitext);
				ArrayList<WordPair> forwardViterbi = forwardAlign
						.getAlignments();
				Integer sourceIndex = 0;

				ArrayList<Integer> targetsUsed = new ArrayList<Integer>();
				// for forward alignment
				for (String s : currentBitext.getSource()) {
					Integer targetIndex = 0;
					Integer closestTarget = currentBitext.getTarget().length;
					for (WordPair wp : forwardViterbi) {
						if (wp.e.equals(s)) {
							String target = wp.f;
							ArrayList<Integer> targetPositions = new ArrayList<Integer>();
							for (String t : currentBitext.getTarget()) {
								if (t.equals(target)) {
									targetPositions.add(targetIndex);
								}
								targetIndex++;
							}
							Integer source = sourceIndex;

							if (targetPositions.size() > 1) {
								System.out.println("multiple target words");
								Double sourcePercentage = (double) (sourceIndex / currentBitext
										.getSource().length);
								Double smallest = 100000000000000d;
								for (Integer tp : targetPositions) {
									Double targetPercentage = (double) (tp / currentBitext
											.getTarget().length);
									Double distance = Math.abs(sourcePercentage
											- targetPercentage);
									if (smallest > distance) {
										smallest = distance;
										closestTarget = tp;
									}
								}

							} else if (targetPositions.size() == 1) {
								closestTarget = targetPositions.get(0);
							}

							break;
						}

					}
					System.out.println(sourceIndex);
					System.out.println(closestTarget);
					if (closestTarget >= currentBitext.getTarget().length) {
						System.out.println("Something is wrong");
						System.exit(1);
					}
					sourceTargetPair.put(sourceIndex, closestTarget);
					sourceIndex++;
				}
				System.out
						.println("For bitext id: " + currentBitext.hashCode());
				Iterator<Entry<Integer, Integer>> ait = sourceTargetPair
						.entrySet().iterator();
				while (ait.hasNext()) {
					Map.Entry<Integer, Integer> ip = ait.next();
					System.out.println(ip.getKey() + " : " + ip.getValue()
							+ "----" + currentBitext.getSource()[ip.getKey()]
							+ " : " + currentBitext.getTarget()[ip.getValue()]);
				}
				alignmentMap.put(currentBitext, sourceTargetPair);
			}

			UTIL_FileOperations.store(alignmentMap, mapName);

		}

	}

	public static void templateAlignment() {
		// TODO Auto-generated method stub

		HashMap<Bitext, HashMap<Integer, Integer>> fwAlignment = (HashMap<Bitext, HashMap<Integer, Integer>>) UTIL_FileOperations
				.openObject("forward.aMap");
		HashMap<Bitext, HashMap<Integer, Integer>> bwAlignment = (HashMap<Bitext, HashMap<Integer, Integer>>) UTIL_FileOperations
				.openObject("reverse.aMap");

		Iterator<Entry<Bitext, HashMap<Integer, Integer>>> fmi = fwAlignment
				.entrySet().iterator();
		System.out.println("hi");
		int sum = 0;
		HashMap<Phrase, Integer> phraseCount = new HashMap<Phrase, Integer>();
		while (fmi.hasNext()) {

			Map.Entry<Bitext, HashMap<Integer, Integer>> cfp = fmi.next();
			Bitext cb = cfp.getKey();
			System.out.println("Hash code : " + cb.hashCode());
			HashMap<Integer, Integer> cfa = cfp.getValue();
			HashMap<Integer, Integer> cba = bwAlignment.get(cb);
			if (cba == null) {
				System.out.println("Something went wrong at templateAlignment");
				System.exit(1);
			}
			int[][] mergedTemplate = initialiseTemplate(cb, cfa, cba);
			System.out.println("Growing for " + cb.hashCode());
			int[][] growDiagTemplate = growDiag(mergedTemplate,
					cb.getSource().length, cb.getTarget().length);
			int[][] growDiagFinalTemplate = finalGrow(mergedTemplate,
					growDiagTemplate, cb.getSource().length,
					cb.getTarget().length);
			HashMap<Phrase, Integer> bitextPhraseCount = readPhrase(cb,
					growDiagFinalTemplate, cb.getSource().length,
					cb.getTarget().length);
			Iterator<Entry<Phrase, Integer>> bpci = bitextPhraseCount
					.entrySet().iterator();
			while (bpci.hasNext()) {
				Map.Entry<Phrase, Integer> pair = bpci.next();
				if (phraseCount.get(pair.getKey()) == null) {
					System.out.println("unique phrase");
					pc = pair.getValue() + pc;
					tp = pair.getValue() + tp;
					phraseCount.put(pair.getKey(), pair.getValue());
				} else {
					phraseCount.put(pair.getKey(), pair.getValue()
							+ phraseCount.get(pair.getKey()));
					tp = pair.getValue() + tp;
				}
			}
			sum++;
		}
		Iterator<Entry<Phrase, Integer>> pi = phraseCount.entrySet().iterator();
		String result = "";
		while (pi.hasNext()) {
			Map.Entry<Phrase, Integer> pair = pi.next();
			Phrase cp = pair.getKey();
			ArrayList<String> cps = cp.getSourceSequence();
			ArrayList<String> cpt = cp.getTargetSequence();

			String source = "";
			String target = "";
			for (String s : cps) {
				source = source + s + " ";
			}
			for (String t : cpt) {
				target = target + t + " ";
			}
			result = source + "\\\\\\" + target;
			System.out.println(result);
			System.out.println("Count : " + pair.getValue());

		

		}
		UTIL_FileOperations.store(phraseCount, "phraseCount.pc");
		System.out.println("Bitext Count: " + sum);
		System.out.println("Unique phrases: " + pc);
		System.out.println("Total phrases: " + tp);
	}

	private static int[][] finalGrow(int[][] mergedTemplate,
			int[][] growDiagTemplate, int nsw, int ntw) {

		for (int i = 0; i < ntw; i++) {
			for (int j = 0; j < nsw; j++) {
				if (mergedTemplate[i][j] == 1) {
					// check row
					boolean rowEmpty = true;
					boolean columnEmpty = true;
					for (int r = 0; r < ntw; r++) {
						if (growDiagTemplate[r][j] == 2) {
							rowEmpty = false;
							break;
						}
					}
					for (int c = 0; c < nsw; c++) {
						if (growDiagTemplate[i][c] == 2) {
							columnEmpty = false;
							break;
						}
					}
					// check column
					if (rowEmpty && columnEmpty) {
						growDiagTemplate[i][j] = 2;
					}
				}
			}
		}

		for (int i = 0; i < ntw; i++) {
			for (int j = 0; j < nsw; j++) {

				System.out.print(growDiagTemplate[i][j] + " ");

			}
			System.out.println();
		}
		return growDiagTemplate;
	}

	private static HashMap<Phrase, Integer> readPhrase(Bitext cb,
			int[][] finalTemplate, int nsw, int ntw) {
		// 0, 0, 2, 0
		// 0, 0, 0, 2
		// 0, 2, 0, 0
		System.out.println("Starting reading off phrase for bitext :"
				+ cb.hashCode());
		HashMap<Phrase, Integer> bpc = new HashMap<Phrase, Integer>();
		int phraseCount = 0;
		for (int phraseLength = 1; phraseLength < 6; phraseLength++) {
			// for all source words in phrases of length phraseLength
			boolean unaligned = false;
			System.out.println("Length: " + phraseLength);
			for (int sourceIndex = 0; sourceIndex < nsw - phraseLength + 1; sourceIndex++) {
				System.out.println("Source Index: " + sourceIndex);
				int lastIndex = sourceIndex + phraseLength - 1;
				int globalHighest = -1;
				int globalLowest = ntw;
				System.out.println(lastIndex);
				targetloop: for (int lengthCount = 1; lengthCount <= phraseLength; lengthCount++) {
					int currentWord = sourceIndex + lengthCount - 1;
					int smallest = -1;
					int highest = ntw;

					// Searching for phrase block of current source word
					// for all possible current word-target words pair
					for (int targetIndex = 0; targetIndex < ntw; targetIndex++) {
						int point = finalTemplate[targetIndex][currentWord];
						// if the point is in alignment
						if (point == 2) {
							smallest = targetIndex;
							if (smallest <= globalLowest) {
								globalLowest = smallest;
							}
							break;
						}
					}
					for (int targetIndex = ntw - 1; targetIndex >= 0; targetIndex--) {
						int point = finalTemplate[targetIndex][currentWord];
						// if the point is in alignment
						if (point == 2) {
							highest = targetIndex;
							if (highest >= globalHighest) {
								globalHighest = highest;
							}
							break;
						}
					}
					if (smallest >= 0 && highest < ntw) {
						// checking for other alignments
						System.out.println("violation check");
						for (int rangeIndex = smallest; rangeIndex <= highest; rangeIndex++) {

							for (int si = 0; si < nsw; si++) {
								if (finalTemplate[rangeIndex][si] == 2) {
									if (si < sourceIndex || si > lastIndex) {
										unaligned = true;
										System.out.println("unalignment found");
										break targetloop;
										// found source word alignment out of
										// the
										// source phrase range
										// the current phrase in question cannot
										// be
										// used;
									}
								}
							}
						}
					}

				}
				if (!unaligned) {
					// (source start, source end)X(lowest, highest) phrase
					// aligned, can be stored;
					// index ranges into ArrayList<String>, ArrayList<String>
					// data structure and save

					System.out.println("adding phrase");
					ArrayList<String> tsp = new ArrayList<String>();
					ArrayList<String> ttp = new ArrayList<String>();
					for (int s = sourceIndex; s <= lastIndex; s++) {
						String currentWord = cb.getSource()[s];
						tsp.add(currentWord);
					}
					System.out.println(globalLowest);
					System.out.println(globalHighest);

					for (int t = globalLowest; t <= globalHighest; t++) {
						String currentWord = cb.getTarget()[t];
						ttp.add(currentWord);
					}
					if (ttp.size() >= 1) {
						Phrase np = new Phrase(tsp, ttp);
						System.out.println(np.toString());
						try {
							System.in.read();
						} catch (IOException e) {
							e.printStackTrace();
						}
						if (bpc.get(np) == null) {
							bpc.put(np, 1);
						} else {
							bpc.put(np, bpc.get(np) + 1);

						}

					}
				}
			}

		}

		return bpc;
	}

	private static int[][] growDiag(int[][] mergedTemplate, int nsw, int ntw) {
		// int[ntw][nsw]
		// TODO Auto-generated method stub

		int[][] currentPoints = new int[ntw][nsw];

		for (int i = 0; i < ntw; i++) {
			for (int j = 0; j < nsw; j++) {
				if (mergedTemplate[i][j] == 2)
					currentPoints[i][j] = 2;
				else
					currentPoints[i][j] = 0;
			}
		}
		for (int i = 0; i < ntw; i++) {
			for (int j = 0; j < nsw; j++) {

				System.out.print(currentPoints[i][j] + " ");

			}
			System.out.println();
		}

		boolean added = true;
		int[][] temp;
		while (added) {
			added = false;
			temp = currentPoints.clone();
			for (int i = 0; i < ntw; i++) {
				for (int j = 0; j < nsw; j++) {
					if (currentPoints[i][j] == 2) {
						System.out.println("Found intersection---" + j + " : "
								+ i);
						int miny = i - 1;
						int maxy = i + 1;
						int minx = j - 1;
						int maxx = j + 1;
						System.out.println("Searching neighbours");
						int nc = 1;
						int sc = 1;

						for (int k = miny; k < maxy + 1; k++) {
							for (int l = minx; l < maxx + 1; l++) {
								System.out.println("Count : " + nc);
								System.out.println(l + " : " + k);
								nc++;
								boolean selfCheck = (k == i) && (l == j);
								if (!selfCheck) {
									// loop over neighbours
									System.out.println("Not self : " + sc);
									sc++;
									try {

										if (mergedTemplate[k][l] == 1
												&& (!(covered(l, currentPoints,
														"Source")) || !(covered(
														k, currentPoints,
														"Target")))) {
											temp[k][l] = 2;
											System.out.println("new point----"
													+ l + " : " + k);
											added = true;

										}
									} catch (ArrayIndexOutOfBoundsException aio) {
										System.out.println("AIO----" + l
												+ " : " + k);
									}
								}
							}
						}
						System.out.println("Neighbour search for point " + j
								+ " : " + i + " complete");
					}
				}
			}
			System.out.println("Inner loop complete");
			currentPoints = temp.clone();
		}
		System.out.println("Outer loop complete");

		return currentPoints;
	}

	private static boolean covered(int s, int[][] mergedTemplate, String string) {
		// TODO Auto-generated method stub
		int row = mergedTemplate.length;
		int col = mergedTemplate[0].length;
		boolean found = false;
		if (string.equals("Source")) {
			// check if the row has a 2
			for (int i = 0; i < row; i++) {
				if (mergedTemplate[i][s] == 2) {
					found = true;
					break;
				}
			}
		} else if (string.equals("Target")) {
			// check if the column has a 2
			for (int i = 0; i < col; i++) {
				if (mergedTemplate[s][i] == 2) {
					found = true;
					break;
				}
			}
		}
		return found;
	}

	// @Method initialiseTemplate: takes forward and backward alignment of the
	// bitext and returns the whole template. Word position x-y ==Array[y][x]

	private static int[][] initialiseTemplate(Bitext cb,
			HashMap<Integer, Integer> cfa, HashMap<Integer, Integer> cba) {

		// @Var nsw: number of source words
		int nsw = cb.getSource().length;
		int ntw = cb.getTarget().length;
		int[][] template = new int[ntw][nsw];
		for (int i = 0; i < nsw; i++) {
			for (int j = 0; j < ntw; j++) {
				template[j][i] = 0;
			}
		}
		Iterator<Entry<Integer, Integer>> cfai = cfa.entrySet().iterator();
		while (cfai.hasNext()) {
			Map.Entry<Integer, Integer> cfaie = cfai.next();
			int source = cfaie.getKey();
			int target = cfaie.getValue();
			template[target][source]++;
		}

		Iterator<Entry<Integer, Integer>> cbai = cba.entrySet().iterator();
		while (cbai.hasNext()) {
			Map.Entry<Integer, Integer> cbaie = cbai.next();
			int source = cbaie.getKey();
			int target = cbaie.getValue();
			template[source][target]++;
		}

		return template;
	}
}
