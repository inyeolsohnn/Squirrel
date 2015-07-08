package squirrel.ir.index;

import java.io.Serializable;

public class IX_TermPair implements Serializable, Comparable<IX_TermPair> {

	private static final long serialVersionUID = 4868394376368286721L;
	private String wi;
	private String wj;

	public IX_TermPair(String wi, String wj) {
		super();
		this.wi = wi;
		this.wj = wj;
	}

	public String getWi() {
		return wi;
	}

	public void setWi(String wi) {
		this.wi = wi;
	}

	public String getWj() {
		return wj;
	}

	public void setWj(String wj) {
		this.wj = wj;
	}

	// for object equality, compare the two TermPair objects
	// by both included terms' names
	// useful for hashmap functions
	@Override
	public boolean equals(Object o) {
		if (o instanceof IX_TermPair) {
			IX_TermPair p = (IX_TermPair) o;
			if (p.getWi().equals(this.wi) && p.getWj().equals(this.wj)) {
				return true;
			}
		}
		return false;
	}

	// custom hashcode computation, useful for hashmap functions
	@Override
	public int hashCode() {
		return wi.hashCode() + (17 * wj.hashCode());
	}

	// for Comparable interface, compare the two TermPair objects
	// by both ibcluded terms' names
	@Override
	public int compareTo(IX_TermPair o) {
		return this.wi.compareTo(o.wi) + this.wj.compareTo(o.wj);
	}

}
