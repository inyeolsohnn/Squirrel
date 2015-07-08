package squirrel.ir.retrieve;

import java.util.Set;
import java.util.TreeSet;

import squirrel.ir.index.IX_Collection;

public class RT_Query implements Comparable<RT_Query> {

	private int id;
	private String query;
	private IX_Collection.PreparedQuery pQuery;
	private Set<String> patterns;
	private Set<Integer> relevant;

	public RT_Query(int id, String query, Set<String> patterns,
			Set<Integer> relevant) {
		this.id = id;
		this.query = query;
		this.patterns = new TreeSet<String>(patterns);
		this.relevant = relevant;
	}

	public int getId() {
		return id;
	}

	public String getQuery() {
		return query;
	}

	public synchronized IX_Collection.PreparedQuery getPreparedQuery(
			IX_Collection col) {
		if (pQuery == null) {
			pQuery = col.prepare(query);
		}
		return pQuery;
	}

	public Set<String> getPatterns() {
		return patterns;
	}

	public Set<Integer> getRelevant() {
		return relevant;
	}

	@Override
	public String toString() {
		return id + ": '" + query + "' (" + patterns + ") ";
	}

	// for Comparable interface, compare the two queries' IDs
	@Override
	public int compareTo(RT_Query query) {
		return this.id - query.id;
	}
}
