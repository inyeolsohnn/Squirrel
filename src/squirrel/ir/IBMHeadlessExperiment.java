package squirrel.ir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import squirrel.data.json.JSON_IO;
import squirrel.ir.index.IX_Collection;
import squirrel.ir.retrieve.RT_Query;
import squirrel.ir.retrieve.models.MDL_XueIBMQR;
import squirrel.ir.retrieve.models.MDL_XueQR;
import squirrel.smt.aligner.IBM1.WordPair;
import squirrel.util.UTIL_Collections;

public class IBMHeadlessExperiment {

	public void run(IX_Collection col, String qf)
			throws IllegalArgumentException, IOException {
		// requires collection, probability map, and query folder as the input

		// START:experiment setup
		final IX_Collection batchCollection = col;
		final ArrayList<RT_Query> queries = JSON_IO.retrieveQueries(qf);
		int nExperimentNum = 4;
		final int nQueryThreadNum = 4;
		long nTime = System.currentTimeMillis();
		final HashMap<WordPair, Double> pMap;
		final HashMap<WordPair, Double> rpMap;
		try {
			FileInputStream fqa = new FileInputStream(new File("qaProb.pmap"));
			FileInputStream faq = new FileInputStream(new File("aqProb.pmap"));
			ObjectInputStream oqa = new ObjectInputStream(fqa);
			ObjectInputStream oaq = new ObjectInputStream(faq);
			pMap = (HashMap<WordPair, Double>) oqa.readObject();
			rpMap = (HashMap<WordPair, Double>) oaq.readObject();

			System.out.println("Map file succesfully loaded");
			double[] deltaValues = { 0.0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7,
					0.8, 0.9, 1.0 };
			ExecutorService es = Executors.newFixedThreadPool(nExperimentNum);
			for (final double currentDelta : deltaValues) {
				es.submit(new Runnable() {
					@Override
					public void run() {
						// TODO Iterate F-Levels
						System.out.println("Starting run for delta "
								+ currentDelta);

						long nTime = System.currentTimeMillis();

						try {
							batchRunIBMXueNoIO(batchCollection, queries,
									currentDelta, 2.0, nQueryThreadNum, pMap,
									rpMap);
							System.out
									.println("Successfully finished run for delta "
											+ currentDelta);
						} catch (Throwable t) {
							System.err.println("Exception in run for delta "
									+ currentDelta + ": "
									+ t.getLocalizedMessage());
						}

						System.out.println("Run for delta " + currentDelta
								+ " took "
								+ (System.currentTimeMillis() - nTime)
								+ " milliseconds.");
					}
				});
			}
			es.shutdown();
			try {
				while (!es.awaitTermination(1, TimeUnit.SECONDS)) {
				}
			} catch (InterruptedException ie) {
			}
			System.out.println("Successfully completed run in "
					+ (System.currentTimeMillis() - nTime) + " milliseconds.");

			UTIL_Collections.closeCollection(batchCollection);
		} catch (FileNotFoundException e) {

			e.printStackTrace();
		} catch (IOException e) {

			e.printStackTrace();
		} catch (ClassNotFoundException e) {

			e.printStackTrace();
		}
		// END:experiment setup

	}

	File batchRunIBMXueNoIO(final IX_Collection col,
			final List<RT_Query> queries, final double delta,
			final double fLevel, int nThreadNums, HashMap<WordPair, Double> rp,
			HashMap<WordPair, Double> rpm) {

		// Open csv file for result output
		File f = new File("results/");
		if (!f.isDirectory()) {
			f.mkdir();
		}

		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm");
		f = new File("results/" + col.getName() + "_" + "delta"
				+ Double.toString(delta) + "_" + dateFormat.format(new Date())
				+ ".csv");
		FileWriter fw = null;
		try {
			fw = new FileWriter(f);
		} catch (IOException e) {
			System.err.println("Specified file " + f.getName()
					+ " could not be opened.");
			return null;
		}

		// Write csv header
		BufferedWriter bw = new BufferedWriter(fw);
		final String l = ", ";
		try {
			String q1 = "";
			String q2 = "";
			for (int i = 1; i <= queries.size(); i++) {
				if (i < queries.size() - 1) {
					q1 += "Query " + i + l + l + l + l + l + l;
				} else {
					q1 += "Query " + i;
				}
				q2 += "precision@10" + l + "patternPrecision@10" + l
						+ "recall@10" + l + "patternRecall@10" + l
						+ "F-measure@" + fLevel + l + "R-precision" + l
						+ "pattern-R-precision" + l;
			}
			bw.write(l + l + l + q1 + "\n");
			bw.write("alpha" + l + "beta" + l + "gamma" + l + q2
					+ "Avg precision@10" + l + "Avg pattern precision@10" + l
					+ "Avg recall@10" + l + "Avg pattern recall@10" + l
					+ "Avg F-measure@" + fLevel + l + "Avg R-precision" + l
					+ "Avg pattern-R-precision" + "\n");
		} catch (IOException e) {
			System.out.println("Error during writing to csv.");
			try {
				bw.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			return null;
		}

		// Execute the searches in threads making use of modern multi-core CPUs
		ExecutorService es = Executors.newFixedThreadPool(nThreadNums);

		// Loop through all possible sets of parameters and run all queries for
		// each set
		final MDL_XueIBMQR xm = new MDL_XueIBMQR(col, delta, rp, rpm);
		long nTime = System.currentTimeMillis();

		nTime = System.currentTimeMillis();
		List<Future<String>> lsCSVLines = new ArrayList<Future<String>>(
				(11 * 10) / 2);
		for (int nAlpha = 0; nAlpha <= 10; nAlpha++) {
			for (int nBeta = 0; nAlpha + nBeta <= 10; nBeta++) {
				int nGamma = 10 - (nAlpha + nBeta);

				final double alpha = nAlpha / 10.0d;
				final double beta = nBeta / 10.0d;
				final double gamma = nGamma / 10.0d;

				lsCSVLines.add(es.submit(new Callable<String>() {
					@Override
					public String call() {
						// Initialize the F-measure metric and the total
						// (for average)
						IRQualityMetrics metrics;
						double totalFMeasure = 0;
						double totalRPrecision = 0;
						double totalPrecision = 0;
						double totalPatternPrecision = 0;
						double totalPatternRPrecision = 0;
						double totalRecall = 0;
						double totalPatternRecall = 0;

						StringBuffer sb = new StringBuffer("" + alpha)
								.append(l).append(beta).append(l).append(gamma)
								.append(l);
						for (RT_Query query : queries) {
							System.out.println("(" + delta + ", " + alpha
									+ ", " + beta + ", " + gamma
									+ "): Starting work on query " + query);
							metrics = xm.search(query,
									new MDL_XueIBMQR.SearchConfig(alpha, beta,
											gamma));

							if (metrics != null) {
								double precision = metrics.getPrecisionAtTopK();
								totalPrecision += precision;
								sb.append(precision).append(l);

								double patternPrecision = metrics
										.getPatternPrecision();
								totalPatternPrecision += patternPrecision;
								sb.append(patternPrecision).append(l);

								double recall = metrics.getRecallAtTopK();
								totalRecall += recall;
								sb.append(recall).append(l);

								double patternRecall = metrics
										.getPatternRecall();
								totalPatternRecall += patternRecall;
								sb.append(patternRecall).append(l);

								double fmeasure = metrics
										.getFMeasureAtTopK(fLevel);
								totalFMeasure += fmeasure;
								sb.append(fmeasure).append(l);

								double rPrecision = metrics.getRPrecision();
								totalRPrecision += rPrecision;
								sb.append(rPrecision).append(l);

								double patternRPrecision = metrics
										.getPatternRPrecision();
								totalPatternRPrecision += patternRPrecision;
								sb.append(patternRPrecision).append(l);
							} else {
								sb.append(0).append(l).append(0).append(l)
										.append(0).append(l).append(0)
										.append(l).append(0).append(l)
										.append(0).append(l).append(0)
										.append(l);
							}
						}
						totalPrecision = totalPrecision / queries.size();
						totalPatternPrecision = totalPatternPrecision
								/ queries.size();
						totalRecall = totalRecall / queries.size();
						totalPatternRecall = totalPatternRecall
								/ queries.size();
						totalFMeasure = totalFMeasure / queries.size();
						totalRPrecision = totalRPrecision / queries.size();
						totalPatternRPrecision = totalPatternRPrecision
								/ queries.size();
						sb.append(totalPrecision).append(l)
								.append(totalPatternPrecision).append(l)
								.append(totalRecall).append(l)
								.append(totalPatternRecall).append(l)
								.append(totalFMeasure).append(l)
								.append(totalRPrecision).append(l)
								.append(totalPatternRPrecision).append("\n");

						return sb.toString();
					}
				}));
			}
		}

		es.shutdown();
		for (Future<String> fsCurrentCSVLine : lsCSVLines) {
			try {
				bw.write(fsCurrentCSVLine.get());
			} catch (IOException | InterruptedException | ExecutionException e) {
				System.err.println("Error writing to csv: " + e);
				e.printStackTrace();
			}
		}

		System.out.println(delta + ": Finished running queries in "
				+ (System.currentTimeMillis() - nTime) + " milliseconds.");

		// Close the csv file now that we're done
		try {
			bw.close();
			return f;

		} catch (IOException e) {
			System.err.println("Could not close csv file.");
			return null;
		}
	}
}
