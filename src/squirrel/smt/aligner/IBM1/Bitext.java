package squirrel.smt.aligner.IBM1;

import java.io.Serializable;
import java.util.ArrayList;
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
		question = cleanText(qsplits);
		answer = cleanText(asplits);
		this.hashcode = hashcode;
	}

	private String[] cleanText(String[] splits) {
		// TODO Auto-generated method stub
		ArrayList<String> tempResult= new ArrayList<String>();
		String previous="";
		boolean consecutive= false;
		for(int i=0; i<splits.length; i++){
			if(splits[i].equals(previous)){
				consecutive=true;
			}
			
			if(!consecutive){
				tempResult.add(splits[i]);
				previous=splits[i];
			}else{
				System.out.println("Word removed");
			}
			
			consecutive=false;
		}
		String[] result = new String[tempResult.size()];
		result = tempResult.toArray(result);
		return result;
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
