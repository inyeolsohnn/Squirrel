package squirrel.ir.index;

import java.io.Serializable;
import java.util.Set;

public class IX_Document implements Serializable, Comparable<IX_Document> {

	private static final long serialVersionUID = 1488674646257621361L;
	private int docID;
	private String url;
	private int count;
	private String[] qTerms;
	private Set<String> patterns;

	public IX_Document(int docID, String url, int count, String[] qTerms, Set<String> patterns) {
		this.docID = docID;
		this.url = url;
		this.count = count;
		this.qTerms = qTerms;
		this.patterns = patterns;
	}

	public String[] getqTerms() {
		return qTerms;
	}

	public int getDocID() {
		return docID;
	}

	public String getUrl() {
		return url;
	}

	public int getCount() {
		return count;
	}
	
	public Set<String> getPatterns() {
		return patterns;
	}

	// for Comparable interface, compare the two documents' IDs
	public int compareTo(IX_Document doc) {
		return this.docID - doc.getDocID();
	}

}
