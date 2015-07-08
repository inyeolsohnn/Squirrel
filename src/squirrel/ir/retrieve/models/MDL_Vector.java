package squirrel.ir.retrieve.models;

import java.util.ArrayList;
import java.util.List;

import squirrel.ir.index.IX_Collection;
import squirrel.ir.index.IX_Term;
import squirrel.ir.index.IX_TermMatch;
import squirrel.ir.retrieve.RT_Result;

public class MDL_Vector extends MDL_GenericModel<MDL_GenericModel.SearchConfig> {

	public MDL_Vector(IX_Collection col) {
		super(col);
	}

	@Override
	protected List<RT_Result> internalSearch(IX_Collection.PreparedQuery pq,
			SearchConfig config) {
		if (config != null) {
			throw new IllegalArgumentException(
					"Search configuration must be null for vector search!");
		}

		ArrayList<RT_Result> acc = new ArrayList<RT_Result>();

		// Weight computation using the inverted index.
		for (IX_Term t : pq.getTerms()) {
			int nt = t.getFreq();
			double idft = this.idftNormal(nt);

			for (IX_TermMatch tm : t.getMatches()) {
				int freq = tm.getCount();
				double tf = this.tfLog(freq);
				double weight = idft * tf;
				boolean flag = false;

				for (RT_Result currentResult : acc) {
					if (currentResult.getDocID() == tm.getDocid()) {
						currentResult.addScore(weight);
						flag = true;
					}
				}

				if (!flag) {
					RT_Result newResult = new RT_Result(tm.getDocid(), 0);
					newResult.addScore(weight);
					acc.add(newResult);
				}
			}
		}

		// Normalization of the scores
		for (RT_Result currentResult : acc) {
			int Ld = col.getDocLength(currentResult.getDocID());
			currentResult.normalizeScore(Ld);
		}

		return acc;
	}

	private double tfLog(int freq) {
		return 1 + Math.log10(freq);
	}

	private double idftNormal(int nt) {
		int N = col.getCount();
		return Math.log((N + nt) / nt);
	}

}
