package squirrel.smt.aligner.IBM1;


public class Bitext {
	private String[] question;
	private String[] answer;

	public Bitext(String[] qsplits, String[] asplits) {
		// TODO Auto-generated constructor stub
		question=qsplits;
		answer=asplits;
	}

	public String[] getSource(){
		return question;
	}
	public String[] getTarget(){
		return answer;
	}

	public String toString() {
		return question + "|" + answer;
	}




}
