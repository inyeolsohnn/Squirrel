package squirrel.smt.aligner.IBM1;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import squirrel.data.json.JSON_Document;
import squirrel.data.json.JSON_IO;
import squirrel.data.json.JSON_Document.Answer;
import squirrel.util.UTIL_FileOperations;
import squirrel.util.UTIL_TextClean;

public class IBM1Driver {

	public static void main(String args[]) {
		String[] eng = { "the book", "a house", "the big book", "a small book",
				"the big house", "a small house" };
		String[] de = { "das buch", "ein haus", "das grosse buch",
				"ein kleines buch", "das grosse haus", "ein kleines haus" };
		ArrayList<Bitext> ftb = new ArrayList<Bitext>();
		ArrayList<Bitext> btb = new ArrayList<Bitext>();
		for (int i = 0; i < eng.length; i++) {
			String ce = eng[i];
			String cd = de[i];
			String[] cea = ce.split(" ");
			String[] cda = cd.split(" ");
			ftb.add(new Bitext(cea, cda, i));
			btb.add(new Bitext(cda, cea, i));
		}
		IBM1EM.whereAmI();

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
			int i = 0;
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
					forwardBitext.add(new Bitext(qsplits, asplits, i));

					reverseBitext.add(new Bitext(asplits, qsplits, i));
					i++;
				}

			}
		}

		// train and store the pairs
		UTIL_FileOperations.store(ftb, "forward.bitext");
		UTIL_FileOperations.store(btb, "reverse.bitext");
		IBM1EM.computeIBMForDir();
	}

}
