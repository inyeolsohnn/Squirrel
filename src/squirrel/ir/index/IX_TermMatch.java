package squirrel.ir.index;

import java.io.Serializable;

public class IX_TermMatch implements Serializable {

	private static final long serialVersionUID = 4846999550438082449L;
	private int docid;
	private int count;
	private int ansid;

	public IX_TermMatch(int docid, int ansid) {
		this.docid = docid;
		this.count = 1;
		this.ansid = ansid;
	}

	public int getCount() {
		return count;
	}

	public void increaseCount() {
		count += 1;
	}

	public int getDocid() {
		return docid;
	}

	public int getAnsid() {
		return ansid;
	}
}
