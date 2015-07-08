package squirrel.ir.retrieve;

public class RT_Result implements Comparable<RT_Result> {

	private int docID;
	private double score;

	public RT_Result(int docID, int score) {
		this.docID = docID;
		this.score = score;
	}

	public double getScore() {
		return score;
	}

	// add to the score
	public void addScore(double score) {
		this.score += score;
	}

	// multiply the score and update with the new value
	public void mulScore(double score) {
		this.score *= score;
	}

	// normalize the score by a factor (division)
	public void normalizeScore(int factor) {
		this.score = (this.score / factor);
	}

	public int getDocID() {
		return docID;
	}

	// for Comparable interface, compare the two results' scores,
	// and if they're the same, compare by their document ID
	public int compareTo(RT_Result r) {
		if (this.score < r.getScore()) {
			return 1;
		} else if (this.score > r.getScore()) {
			return -1;
		} else {
			if (this.docID < r.getDocID()) {
				return -1;
			} else {
				return 1;
			}
		}
	}
}
