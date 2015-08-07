package squirrel.smt.aligner.IBM1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import squirrel.util.UTIL_FileOperations;
import squirrel.util.UTIL_UserInput;

public class PhraseExtractor {
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
		System.out.println("name of backward bitext");
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
				.openObject("backward.aMap");

		Iterator<Entry<Bitext, HashMap<Integer, Integer>>> fmi = fwAlignment
				.entrySet().iterator();

		while (fmi.hasNext()) {
			Map.Entry<Bitext, HashMap<Integer, Integer>> cfp = fmi.next();
			Bitext cb = cfp.getKey();
			HashMap<Integer, Integer> cfa = cfp.getValue();
			HashMap<Integer, Integer> cba = bwAlignment.get(cb);
			if (cba == null) {
				System.out.println("Something went wrong at templateAlignment");
				System.exit(1);
			}
			int[][] templateArray = initialiseTemplate(cb, cfa, cba);

		}
	}

	// @Method initialiseTemplate: takes forward and backward alignment of the
	// bitext and returns the whole template. Word position x-y ==Array[y][x]

	private static int[][] initialiseTemplate(Bitext cb,
			HashMap<Integer, Integer> cfa, HashMap<Integer, Integer> cba) {

		// @Var nsw: number of source words
		int nsw = cb.getSource().length;
		int ntw = cb.getTarget().length;
		int[][] template = new int[ntw][nsw];
		Arrays.fill(template, 0);
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
			template[target][source]++;
		}

		// checking template for strangeness
		for (int i = 0; i < nsw; i++) {
			for (int j = 0; j < ntw; j++) {
				if (template[j][i] > 2) {
					System.out.println("error with templating");
					System.exit(1);
				}
			}
		}
		return template;
	}
}
