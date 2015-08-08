package squirrel.smt.aligner.IBM1;

import java.io.Serializable;
import java.util.ArrayList;

public class Phrase implements Serializable{
	/**
	 * 
	 */
	private static final long serialVersionUID = 270656917307330984L;
	ArrayList<String> sourceSequence;
	ArrayList<String> targetSequence;

	@Override
	public String toString() {
		return "Phrase [sourceSequence=" + sourceSequence + ", targetSequence="
				+ targetSequence + "]";
	}

	public Phrase(ArrayList<String> s, ArrayList<String> t) {
		this.sourceSequence = s;
		this.targetSequence = t;

	}

	public ArrayList<String> getSourceSequence() {
		return sourceSequence;
	}

	public void setSourceSequence(ArrayList<String> sourceSequence) {
		this.sourceSequence = sourceSequence;
	}

	public ArrayList<String> getTargetSequence() {
		return targetSequence;
	}

	public void setTargetSequence(ArrayList<String> targetSequence) {
		this.targetSequence = targetSequence;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((sourceSequence == null) ? 0 : sourceSequence.hashCode());
		result = prime * result
				+ ((targetSequence == null) ? 0 : targetSequence.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Phrase other = (Phrase) obj;
		if (sourceSequence == null) {
			if (other.sourceSequence != null)
				return false;
		} else if (!sourceSequence.equals(other.sourceSequence))
			return false;
		if (targetSequence == null) {
			if (other.targetSequence != null)
				return false;
		} else if (!targetSequence.equals(other.targetSequence))
			return false;
		return true;
	}
}
