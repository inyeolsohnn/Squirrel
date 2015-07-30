package squirrel.smt.aligner.IBM1;

import java.io.Serializable;


public class WordPair implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3059485459480218421L;
	public String e;
	public String f;

	public WordPair(String e, String f) {
		this.e = e;
		this.f = f;
	}

	public String toString() {
		return e + "|" + f;
	}
	public void setPAir(String e, String f){
		
	}

	@Override
	public int hashCode() {
		return (e + "|" + f).hashCode();
	}

	@Override
	public boolean equals(Object obj) {

		WordPair wp = (WordPair) obj;
		return this.e.equals(wp.e) && this.f.equals(wp.f);
	}

}
