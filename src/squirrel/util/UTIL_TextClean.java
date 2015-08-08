package squirrel.util;

import java.util.Arrays;
import java.util.List;
import java.util.StringTokenizer;

public class UTIL_TextClean {

	public static String[] cleanText(String str, boolean stopwords) {

		// Pipes and Filters

		// Cleaning punctuation etc.
		str = filterText(str);

		// Removing stopwords
		if (stopwords) {
			str = lookup(str);
		}

		// Snowball stemming
		str = UTIL_SnowballStemmer.stem(str);
		// Removing stopwords
		if (stopwords) {
			str = lookup(str);
		}

		return str.split(" ");
	}

	// check for stopwords
	private static String lookup(String str) {

		String[] splitdocument = str.split(" ");
		StringBuilder temp = new StringBuilder();

		for (String token : splitdocument) {
			if (!(stopWords.contains(token) || token.length() < 3)) {
				temp.append(" " + token);
			}
		}

		return stripWhiteSpace(temp.toString());
	}

	// remove unnecessary characters from terms (non alphanumeric)
	private static String filterText(String s) {
		StringTokenizer token = new StringTokenizer(s);
		StringBuilder resultOutput = new StringBuilder();
		while (token.hasMoreTokens()) {
			char[] w = token.nextToken().toCharArray();
			StringBuilder text = new StringBuilder();
			for (int i = 0; i < w.length; i++) {
				int ch = w[i];
				if (Character.isSpaceChar((char) ch)
						|| Character.isWhitespace((char) ch)
						|| Character.isLetter/* OrDigit */((char) ch)) {
					text.append((char) ch);
				} else {
					text.append(" ");
				}
			}
			resultOutput.append(" " + text);
		}
		return stripWhiteSpace(resultOutput.toString());
	}

	// remove extra whitespace
	private static String stripWhiteSpace(String text) {
		StringTokenizer token = new StringTokenizer(text);
		StringBuilder resultOutput = new StringBuilder();
		while (token.hasMoreTokens()) {
			resultOutput.append(token.nextToken().toLowerCase() + " ");
		}
		return resultOutput.toString();
	}

	// stopword list
	public static final List<String> stopWords = Arrays.asList("and", "about",
			"after", "afterwards", "again", "against", "all", "almost",
			"along", "already", "also", "although", "always", "among",
			"amongst", "amoungst", "amount", "and", "another", "any", "anyhow",
			"anyone", "anything", "anyway", "anywhere", "are", "around",
			"back", "became", "because", "become", "becomes", "becoming",
			"been", "before", "beforehand", "behind", "being", "beside",
			"besides", "beyond", "bill", "both", "bottom", "but", "call",
			"can", "cannot", "cant", "con", "could", "couldnt", "cry",
			"describe", "detail", "dig", "done", "down", "due", "during",
			"eight", "either", "eleven", "else", "elsewhere", "enough", "etc",
			"even", "ever", "everyone", "everything", "everywhere", "except",
			"few", "fifteen", "fify", "fill", "find", "fire", "five", "for",
			"former", "formerly", "forty", "found", "four", "from", "front",
			"full", "further", "get", "give", "had", "has", "hasnt", "have",
			"hence", "her", "here", "hereafter", "hereby", "herein",
			"hereupon", "hers", "herself", "him", "himself", "his", "how",
			"however", "hundred", "inc", "indeed", "interest", "into", "its",
			"itself", "keep", "last", "latter", "latterly", "least", "less",
			"ltd", "made", "many", "may", "meanwhile", "might", "mill", "mine",
			"moreover", "most", "mostly", "move", "much", "must", "myself",
			"name", "namely", "neither", "never", "nevertheless", "next",
			"nine", "nobody", "none", "noone", "nor", "not", "nothing", "now",
			"nowhere", "off", "often", "once", "one", "only", "onto", "other",
			"others", "otherwise", "our", "ours", "ourselves", "out", "over",
			"own", "per", "perhaps", "please", "put", "rather", "see", "seem",
			"seemed", "seeming", "seems", "serious", "several", "she",
			"should", "show", "side", "since", "sincere", "six", "sixty",
			"some", "somehow", "someone", "something", "sometime", "sometimes",
			"somewhere", "still", "such", "system", "take", "ten", "than",
			"that", "the", "their", "them", "themselves", "then", "thence",
			"there", "thereafter", "thereby", "therefore", "therein",
			"thereupon", "these", "they", "thickv", "thin", "third", "this",
			"those", "though", "three", "through", "throughout", "thru",
			"thus", "together", "too", "top", "toward", "towards", "twelve",
			"twenty", "under", "until", "upon", "very", "via", "was", "well",
			"were", "what", "whatever", "when", "whence", "whenever", "where",
			"whereafter", "whereas", "whereby", "wherein", "whereupon",
			"wherever", "whether", "which", "while", "whither", "wonder",
			"who", "whoever", "whole", "whom", "whose", "why", "will", "with",
			"without", "would", "yet", "you", "your", "yours", "yourself",
			"yourselves");
}
