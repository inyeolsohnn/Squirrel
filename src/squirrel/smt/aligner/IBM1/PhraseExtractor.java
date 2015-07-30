package squirrel.smt.aligner.IBM1;

import java.util.ArrayList;
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
		HashMap<Bitext, AlignPair> fwAlignment = (HashMap<Bitext, AlignPair>) UTIL_FileOperations
				.openObject(fwA);

		System.out.println("name of backward bitext");
		String bwA = UTIL_UserInput.fileNameInput();
		HashMap<Bitext, AlignPair> bwAlignment = (HashMap<Bitext, AlignPair>) UTIL_FileOperations
				.openObject(bwA);
		// Iterator<Entry<Bitext, AlignPair>> fIt = fwAlignment.entrySet()
		// .iterator();
		HashMap<Bitext, HashMap<Integer, Integer>> alignmentMap = new HashMap<Bitext, HashMap<Integer, Integer>>();
		Iterator<Entry<Bitext, AlignPair>> fwait = fwAlignment.entrySet()
				.iterator();
		while (fwait.hasNext()) {
			HashMap<Integer, Integer> sourceTargetPair = new HashMap<Integer, Integer>();
			Map.Entry<Bitext, AlignPair> pair = fwait.next(); 
			AlignPair forwardAlign = pair.getValue();
			Bitext currentBitext = pair.getKey();
			AlignPair backwardAlign = bwAlignment.get(currentBitext);
			ArrayList<WordPair> forwardViterbi = forwardAlign.getAlignments();
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
							Double smallest = 100000000d;
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
			System.out.println("For bitext id: " + currentBitext.hashCode());
			Iterator<Entry<Integer, Integer>> ait = sourceTargetPair.entrySet()
					.iterator();
			while (ait.hasNext()) {
				Map.Entry<Integer, Integer> ip = ait.next();
				System.out.println(ip.getKey() + " : " + ip.getValue() + "----"
						+ currentBitext.getSource()[ip.getKey()] + " : "
						+ currentBitext.getTarget()[ip.getValue()]);
			}
			alignmentMap.put(currentBitext, sourceTargetPair);
		}

		UTIL_FileOperations.store(alignmentMap, "forward.aMap");

	}

	private static void phraseTemplating(Bitext fwBit, AlignPair fwAl,
			AlignPair bwAl) {
		// TODO Auto-generated method stub
		for (String st : fwBit.getSource()) {

		}

	}

}
