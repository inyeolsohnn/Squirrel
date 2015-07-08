package squirrel.util;

import java.lang.reflect.Method;

import org.tartarus.snowball.SnowballProgram;

public class UTIL_SnowballStemmer {

	protected final static Object[] emptyArgs = new Object[0];

	// Stems the given String's terms and returns the stemmed terms
	@SuppressWarnings("unchecked")
	public static String stem(String str) {

		SnowballProgram stemmer = null;
		Method stemMethod = null;

		String StemLanguage = "english";

		try {
			Class<? extends SnowballProgram> stemClass = (Class<? extends SnowballProgram>) Class
					.forName("org.tartarus.snowball.ext." + StemLanguage + "Stemmer");
			stemmer = (SnowballProgram) stemClass.newInstance();
			stemMethod = stemClass.getMethod("stem", new Class[0]);
		} catch (Exception e) {
			System.out.println("Error, snowball stemmer could not be generated: " + e);
			e.printStackTrace();
		}

		// get the atual terms from the string
		String[] terms = str.split(" ");

		StringBuilder sb = new StringBuilder();

		// loop through, stem and construct result
		for (String term : terms) {
			stemmer.setCurrent(term);
			try {
				stemMethod.invoke(stemmer, emptyArgs); // stemmer.stem();
			} catch (Exception e) {
				System.out.println("Error,snowball stemmer could not stem term: " + term + " " + e);
				e.printStackTrace();
			}

			sb.append(stemmer.getCurrent() + " ");
		}
		return sb.toString();
	}

}
