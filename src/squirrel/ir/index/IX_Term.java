package squirrel.ir.index;

import java.io.Serializable;
import java.util.ArrayList;

public final class IX_Term implements Comparable<IX_Term>, Serializable {

	private static final long serialVersionUID = 311420744954482410L;
	private String name;
	private int count;
	private int freq;

	private ArrayList<IX_TermMatch> matches = new ArrayList<IX_TermMatch>();

	public IX_Term(String name, int docid, int ansid) {
		this.name = name;
		this.matches.add(new IX_TermMatch(docid, ansid));// add first match
		this.count = 1;
		this.freq = 1;
	}

	public ArrayList<IX_TermMatch> getMatches() {
		return matches;
	}

	public int getCount() {
		return count;
	}

	public String getName() {
		return name;
	}

	public int getFreq() {
		return freq;
	}

	/**
	 * TODO This makes strong assumptions about how it will be called!
	 * 
	 * @param docid
	 * @param ansid
	 */
	public void addMatch(int docid, int ansid) {
		int length = matches.size();
		IX_TermMatch tm = matches.get(length - 1);// take the last match
		// if it is the same match, increase its count
		if ((tm.getDocid() == docid) && (tm.getAnsid() == ansid)) {
			tm.increaseCount();
		}// if new ansid, add new match
		else if ((tm.getDocid() == docid) && (tm.getAnsid() != ansid)) {
			IX_TermMatch tmatch = new IX_TermMatch(docid, ansid);
			matches.add(tmatch);
		} else {// if it's a new document, add new match
				// and increase frequency
			IX_TermMatch tmatch = new IX_TermMatch(docid, ansid);
			matches.add(tmatch);
			freq++;// if this is a new doc
		}
		// increase match counter
		count++;
	}

	// for Comparable interface, compare the two terms' names
	public int compareTo(IX_Term term) {
		int a = name.compareToIgnoreCase(term.getName());
		if (a == 0) {
			return 0;
		} else if (a < 0) {
			return -1;
		} else {
			return 1;
		}
	}
}
