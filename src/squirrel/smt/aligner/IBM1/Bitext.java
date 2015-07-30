package squirrel.smt.aligner.IBM1;

import java.io.Serializable;
import java.util.Arrays;

public class Bitext implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5528085664383824624L;
	private int hashcode;
	private String[] question;
	private String[] answer;

	public Bitext(String[] qsplits, String[] asplits, int hashcode) {
		// TODO Auto-generated constructor stub
		question = qsplits;
		answer = asplits;
		this.hashcode = hashcode;
	}

	public String[] getSource() {
		return question;
	}

	public String[] getTarget() {
		return answer;
	}

	public String toString() {
		return question + "|" + answer;
	}

	public boolean equals(Object o) {
		// TODO Auto-generated method stub
		if (o instanceof Bitext) {
			Bitext bt2 = (Bitext) o;
			return this.hashcode == bt2.hashcode;
		} else
			return false;
	}

	public int hashCode() {
		return hashcode;
	}

}
