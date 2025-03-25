package se.vti.roundtrips.preferences;

import java.util.Arrays;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;
import org.apache.commons.math3.util.CombinatoricsUtils;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class MaximumEntropyPriorFactory<L extends Location> {

	// -------------------- CONSTANTS --------------------

	private final int locationCnt;
	private final int timeStepCnt;
	private final int maxRoundTripLength;

	private final double[] logOfRoundTripCntBySize;

	// -------------------- CONSTRUCTION --------------------

	public MaximumEntropyPriorFactory(int locationCnt, int timeStepCnt, int maxRoundTripLength) {
		this.locationCnt = locationCnt;
		this.timeStepCnt = timeStepCnt;
		this.maxRoundTripLength = Math.min(maxRoundTripLength, timeStepCnt);

		this.logOfRoundTripCntBySize = new double[this.maxRoundTripLength + 1];
		for (int j = 0; j <= this.maxRoundTripLength; j++) {
			this.logOfRoundTripCntBySize[j] = CombinatoricsUtils.binomialCoefficientLog(this.timeStepCnt, j)
					+ j * Math.log(this.locationCnt);
		}
	}

	// -------------------- INTERNALS --------------------

	private double[] createSingleLogWeights(double meanRoundTripLength, boolean correctForCombinatorics) {
		assert (meanRoundTripLength >= 0);
		assert (meanRoundTripLength <= this.maxRoundTripLength);
		final BinomialDistribution binDistr = new BinomialDistribution(this.maxRoundTripLength,
				meanRoundTripLength / this.maxRoundTripLength);
		double[] result = new double[this.maxRoundTripLength + 1];
		for (int j = 0; j < result.length; j++) {
			result[j] = correctForCombinatorics ? binDistr.logProbability(j) - this.logOfRoundTripCntBySize[j]
					: binDistr.logProbability(j);
		}
		return result;
	}

	private double[] probasFromLogWeights(double[] logWeights) {
		final double maxLogWeight = Arrays.stream(logWeights).max().getAsDouble();
		final double[] result = new double[logWeights.length];
		double sum = 0.0;
		for (int j = 0; j < result.length; j++) {
			result[j] = Math.exp(logWeights[j] - maxLogWeight);
			sum += result[j];
		}
		assert (sum >= 1e-8);

		for (int j = 0; j < result.length; j++) {
			result[j] /= sum;
		}
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	public PreferenceComponent<RoundTrip<L>> createSingle(double meanJ) {
		return new PreferenceComponent<RoundTrip<L>>() {
			private final double[] logWeights = createSingleLogWeights(meanJ, true);

			@Override
			public double logWeight(RoundTrip<L> state) {
				return this.logWeights[state.locationCnt()];
			}
		};
	}

	public PreferenceComponent<MultiRoundTrip<L>> createSingles(int _N, double meanRoundTripLength) {
		return new SingleToMultiComponent<>(this.createSingle(meanRoundTripLength));
	}

	public PreferenceComponent<MultiRoundTrip<L>> createMultiple(int _N) {

		/*
		 * singleProbasGivenMeanLength[meanLength][j] is the probability of sampling a
		 * particular size-j roundtrip given the mean round trip length "meanLength".
		 */
		final double[][] singleProbasGivenMeanLength = new double[this.maxRoundTripLength + 1][];
		for (int meanLength = 0; meanLength <= this.maxRoundTripLength; meanLength++) {
			singleProbasGivenMeanLength[meanLength] = this
					.probasFromLogWeights(this.createSingleLogWeights(meanLength, false));
		}

		final ChiSquaredDistribution chi2distr = new ChiSquaredDistribution(_N);

		return new PreferenceComponent<MultiRoundTrip<L>>() {

			@Override
			public double logWeight(MultiRoundTrip<L> roundTrips) {
				assert (roundTrips.size() == _N);

				double count = 0;
				int[] realizedLengthFrequencies = new int[maxRoundTripLength + 1];
				for (RoundTrip<L> roundTrip : roundTrips) {
					count += roundTrip.locationCnt();
					realizedLengthFrequencies[roundTrip.locationCnt()]++;
				}
				final double realizedMeanLength = count / roundTrips.size();
				final int realizedMeanLengthFloor = (int) realizedMeanLength;

				final double[] lowerInterpolationSingleProbas = singleProbasGivenMeanLength[realizedMeanLengthFloor];
				final double[] upperInterpolationSingleProbas = singleProbasGivenMeanLength[(realizedMeanLengthFloor == maxRoundTripLength)
						? maxRoundTripLength
						: realizedMeanLengthFloor + 1];
				final double upperInterpolationWeight = realizedMeanLength - realizedMeanLengthFloor;

				double chi2 = 0.0;
				for (int j = 0; j < realizedLengthFrequencies.length; j++) {
					final double sizeProba = (1.0 - upperInterpolationWeight) * lowerInterpolationSingleProbas[j]
							+ upperInterpolationWeight * upperInterpolationSingleProbas[j];
					chi2 += Math.pow(realizedLengthFrequencies[j] - sizeProba * roundTrips.size(), 2.0) / sizeProba;
				}
				return chi2distr.logDensity(chi2);
			}
		};
	}
}
