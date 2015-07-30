package squirrel.smt.aligner.IBM1;

import java.io.Serializable;
import java.util.ArrayList;

public class AlignPair implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5419931335341115272L;
	/**
	 * 
	 */

	private Bitext bitext;
	private ArrayList<WordPair> alignments;

	public AlignPair(Bitext bt, ArrayList<WordPair> alignments) {
		bitext = bt;
		this.setAlignments(alignments);

	}

	public boolean equals(AlignPair al1) {
		Bitext bt1 = al1.getBitext();

		return this.bitext.equals(bt1);
	}

	public Bitext getBitext() {
		// TODO Auto-generated method stub
		return bitext;
	}

	public ArrayList<WordPair> getAlignments() {
		return alignments;
	}

	public void setAlignments(ArrayList<WordPair> alignments) {
		this.alignments = alignments;
	}

}
