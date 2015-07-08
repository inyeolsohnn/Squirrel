package squirrel.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import squirrel.ir.index.IX_TermPair;

public class UTIL_TermProbabilities {

	/**
	 * This is Panos' original method that did not normalise the probabilities. Never actually called from the code.
	 * 
	 * @param qTerms
	 * @param aTerms
	 * @return
	 */
	public static HashMap<IX_TermPair, Double> getPlin_NonNormalised(
			HashMap<String, HashSet<Integer>> qTerms,
			HashMap<String, HashSet<Integer>> aTerms) {

		// delta parameter for plin calculations
		final double delta = 0.5;

		// plin, the combined probabilty of semantic relation
		// between two terms wi and wj
		HashMap<IX_TermPair, Double> plin = new HashMap<IX_TermPair, Double>();

		// calculate all pQA and pAQ probabilities for all term couples

		// iterate through all wj terms
		for (Map.Entry<String, HashSet<Integer>> aTerm : aTerms.entrySet()) {
			// iterate through all wi terms
			for (Map.Entry<String, HashSet<Integer>> qTerm : qTerms.entrySet()) {

				// Initialize the probabilities
				// (from question to answer and inverse)
				double pqa = 0.0;
				double paq = 0.0;

				// Get the wi and wj terms
				String wi = qTerm.getKey();
				String wj = aTerm.getKey();

				// Get the sets of documents in which each term appears
				HashSet<Integer> wjAset = new HashSet<Integer>(aTerm.getValue());
				HashSet<Integer> wiQset = new HashSet<Integer>(qTerm.getValue());

				// calculate the intersection of the sets
				// and the pqa probability
				wiQset.retainAll(wjAset);
				pqa = (double) wiQset.size() / wjAset.size();

				// calculate inverse probability
				// both terms need to be present in the opposite
				// lists to calculate paq > 0
				if (aTerms.containsKey(wi) && qTerms.containsKey(wj)) {

					// Get the sets of documents in which each term appears
					HashSet<Integer> wiAset = new HashSet<Integer>(
							aTerms.get(wi));
					HashSet<Integer> wjQset = new HashSet<Integer>(
							qTerms.get(wj));

					// calculate the intersection of the sets
					// and the paq probability
					wiAset.retainAll(wjQset);
					paq = wiAset.size() / wjQset.size();
				}

				// calculate the combined probability Plin
				// Plin(wi|wj) = (1-δ)*P(wi,Q|wj,A) + δ*P(wi,A|wj,Q)
				double probability = (1 - delta) * pqa + delta * paq;

				// The current pair of terms
				IX_TermPair pair = new IX_TermPair(wi, wj);

				// Store the combined probability in the plin map
				// if the probability is higher than 0 (it holds information)
				if (probability > 0) {
					plin.put(pair, probability);
				}
			}
		}

		return plin;
	}

}
