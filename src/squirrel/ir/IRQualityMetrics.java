package squirrel.ir;

import squirrel.ir.retrieve.RT_Query;

/**
 * IRQualityMetrics holds data about the quality of search for a particular
 * query.
 * 
 * @author Steffen Zschaler
 */
public class IRQualityMetrics {

	private RT_Query query;
	private int topK;
	private double precision;
	private double rPrecision;
	private double recall;
	private double patternPrecision;
	private double patternRecall;
	private double patternRPrecision;

	public IRQualityMetrics(RT_Query query, int topK, double precision,
			double patternPrecision, double recall, double patternRecall,
			double rPrecision, double patternRPrecision) {
		this.query = query;
		this.topK = topK;
		this.precision = precision;
		this.rPrecision = rPrecision;
		this.patternPrecision = patternPrecision;
		this.patternRecall = patternRecall;
		this.recall = recall;
		this.patternRPrecision = patternRPrecision;
	}

	/**
	 * Get the query for which these metrics hold.
	 */
	public RT_Query getQuery() {
		return query;
	}

	/**
	 * Get the number of search results that were taken into account when
	 * computing these metrics.
	 */
	public int getTopK() {
		return topK;
	}

	/**
	 * Get the precision of search results at the given top K value.
	 */
	public double getPrecisionAtTopK() {
		return precision;
	}

	/**
	 * Get the R-precision of search results.
	 */
	public double getRPrecision() {
		return rPrecision;
	}

	/**
	 * Return the precision of found patterns within topK documents that are
	 * considered relevant for the query.
	 * 
	 * @return
	 */
	public double getPatternPrecision() {
		return patternPrecision;
	}

	/**
	 * Get pattern r-Precision based on ranked (weighted) list of patterns and
	 * set of relevant patterns for the query.
	 * 
	 * @return
	 */
	public double getPatternRPrecision() {
		return patternRPrecision;
	}

	/**
	 * Get the recall of search results at the given top K value.
	 */
	public double getRecallAtTopK() {
		return recall;
	}

	/**
	 * Return the recall of found patterns within topK documents that are
	 * considered relevant for the query.
	 * 
	 * @return
	 */
	public double getPatternRecall() {
		return patternRecall;
	}

	/**
	 * Compute the F-measure for this query and top K value using the given
	 * weight.
	 * 
	 * @param weight
	 *            ranges from 0.5 to 2.0. Higher values favour recall.
	 */
	public double getFMeasureAtTopK(double weight) {
		double tentativeResult = (1 + weight * weight) * (precision * recall)
				/ ((weight * weight * precision) + recall);
		if (Double.isNaN(tentativeResult))
			return 0;
		else
			return tentativeResult;
	}
}