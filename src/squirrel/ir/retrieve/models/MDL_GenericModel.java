package squirrel.ir.retrieve.models;

import java.util.Collections;
import java.util.List;

import squirrel.ir.index.IX_Collection;
import squirrel.ir.index.IX_Document;
import squirrel.ir.retrieve.RT_Result;

public abstract class MDL_GenericModel<SC extends MDL_GenericModel.SearchConfig> {

	public static final int topK = 20;

	protected IX_Collection col;

	/**
	 * A class, potentially to be sub-classed by sub-classes and to be used to
	 * describe the configuration parameters for an invocation of {@see #search}
	 * .
	 * 
	 * @author Steffen Zschaler
	 */
	public static abstract class SearchConfig {
	}

	protected MDL_GenericModel(IX_Collection col) {
		this.col = col;
	}

	/**
	 * Return the collection this model is associated with.
	 * 
	 * @return
	 */
	public IX_Collection getCollection() {
		return col;
	}

	public IX_Document getDocument(int docid) {
		return col.getDocuments().get(docid);
	}

	/**
	 * The central search function. Sub-classes should override {@see
	 * #internalSearch} to define the search algorithm
	 * 
	 * @param pq
	 *            the query
	 * @param config
	 *            configuration data; may be null
	 * @return list of results sorted by rank or null if no results
	 */
	public final List<RT_Result> search(IX_Collection.PreparedQuery pq,
			SC config) {
		if (pq.getCollection() != col) {
			throw new IllegalStateException(
					"Query presented was prepared for a different collection.");
		}

		if (pq.getTerms().isEmpty()) {
			return null;
		}

		List<RT_Result> alResults = internalSearch(pq, config);

		if (alResults != null) {
			Collections.sort(alResults);
		}

		return alResults;
	}

	/**
	 * Sub-classes should override this method to define their own search
	 * algorithms. It should return a list of {@see RT_Result} instances, but
	 * they do not need to be sorted by rank yet.
	 * 
	 * @param pq
	 *            the query. This will be guaranteed to be for this model and to
	 *            contain at least one legal term
	 * @param config
	 *            configuration data; may be null
	 * @return list of results or null if no results
	 */
	protected abstract List<RT_Result> internalSearch(
			IX_Collection.PreparedQuery pq, SC config);
}
