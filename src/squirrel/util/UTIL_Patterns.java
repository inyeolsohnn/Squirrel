package squirrel.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import squirrel.data.json.JSON_Document;

public class UTIL_Patterns {

	// list of known patterns
	private static final List<String> patternList = Arrays.asList("Abstract Factory",
			"Factory Method", "Factory", "Singleton", "Chain of Responsibility", "Builder",
			"Prototype", "Adapter", "Bridge", "Composite", "Decorator", "Facade", "Flyweight",
			"Proxy", "Command", "Interpreter", "Iterator", "Mediator", "Memento", "MVC", "MVP",
			"MVVM", "Observer", "Strategy", "Template Method", "Visitor", "Unit of Work");

	// find pattern names in a document
	public static TreeSet<String> findPatterns(JSON_Document json) {

		// initialize result and counter variables used for resolving
		// conflicts with the three "Factory" patterns
		TreeSet<String> result = new TreeSet<String>();
		int countAF = 0;
		int countFM = 0;
		int countF = 0;

		// retrieve the documents object and get is text
		String s = json.getTitle() + " " + json.getQuestion() + " " + json.getAnswersText();

		// find the set of patterns that exist in the document
		for (String p : UTIL_Patterns.patternList) {
			int matches = match(s, p);
			// workaround for finding correct instances of all three "Factory" patterns
			if ((matches > 0) && (p.contains("Factory"))) {
				if (p.equals("Abstract Factory")) {
					countAF = matches;
					result.add(p);
				} else if (p.equals("Factory Method")) {
					countFM = matches;
					result.add(p);
				} else if (p.equals("Factory")) {
					countF = matches;
					if (countAF + countFM < countF) {
						result.add(p);
					}
				}
			} else if (matches > 0) {
				result.add(p);
			}
		}

		return result;
	}

	// the actual regex pattern matcher
	private static int match(String text, String pattern) {
		Pattern p = Pattern.compile("(\\W|\\s|^)" + pattern + "(\\W|\\s|$)",
				Pattern.CASE_INSENSITIVE);
		Matcher matcher = p.matcher(text);

		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	// takes the list of all patterns found in a result set
	// and returns a sorted list of unique patterns by order
	// of descending frequency
	public static String sortedPatterns(ArrayList<String> patterns) {

		Map<String, Integer> wordCount = new HashMap<String, Integer>();

		Set<String> unique = new HashSet<String>(patterns);
		for (String key : unique) {
			wordCount.put(key, Collections.frequency(patterns, key));
		}

		// Convert map to list of <String,Integer> entries
		List<Map.Entry<String, Integer>> list = new ArrayList<Map.Entry<String, Integer>>(
				wordCount.entrySet());

		// Sort list by integer values
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				// compare o2 to o1, instead of o1 to o2, to get descending freq. order
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		// Create the result String
		String result = "";
		for (Map.Entry<String, Integer> entry : list) {
			result += (entry.getKey() + ", ");
		}
		// Omit last ', ' substring
		return result.substring(0, result.length() - 2);

	}
}
