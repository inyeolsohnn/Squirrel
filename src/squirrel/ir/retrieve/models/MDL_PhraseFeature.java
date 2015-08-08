package squirrel.ir.retrieve.models;

import java.util.List;

import squirrel.ir.index.IX_Collection;
import squirrel.ir.index.IX_Collection.PreparedQuery;
import squirrel.ir.retrieve.RT_Result;

public class MDL_PhraseFeature extends
		MDL_GenericModel<MDL_XueIBMQR.SearchConfig> {
	protected MDL_PhraseFeature(IX_Collection col) {
		super(col);
		// TODO Auto-generated constructor stub
	}

	public static class SearchConfig extends MDL_GenericModel.SearchConfig {
		public double alpha, beta, gamma;

		public SearchConfig(double alpha, double beta, double gamma) {
			this.alpha = alpha;
			this.beta = beta;
			this.gamma = gamma;
		}
	}

	@Override
	protected List<RT_Result> internalSearch(PreparedQuery pq,
			squirrel.ir.retrieve.models.MDL_XueIBMQR.SearchConfig config) {
		// TODO Auto-generated method stub
		return null;
	}

}
