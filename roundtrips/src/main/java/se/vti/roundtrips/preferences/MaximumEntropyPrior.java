package se.vti.roundtrips.preferences;

import java.util.Arrays;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.util.CombinatoricsUtils;

import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class MaximumEntropyPrior<L extends Location> extends PreferenceComponent<RoundTrip<L>> {

	// -------------------- CONSTANTS --------------------

	public final int _L;
	public final int _K;
	public final double meanJ;
	public final double gamma;

	/*
	 * roundTripLogProbasBySize[J] contains prior log-proba of each individual size
	 * J round trip. To obtain the log-proba of sampling any size J round trip (i.e.
	 * independently of which size J trip that is), add logOfRoundTripCnt(..).
	 */
	private final double[] roundTripLogProbasBySize;

	// -------------------- CONSTRUCTION --------------------

	public MaximumEntropyPrior(int _L, int _K, double meanJ) {
		assert (meanJ < _K);

		this._L = _L;
		this._K = _K;
		this.meanJ = meanJ;

		UnivariateFunction meanJMinusPredictedJ = new UnivariateFunction() {
			@Override
			public double value(double gamma) {
				final double[] args = args(_L, _K, gamma);
				final double maxArg = Arrays.stream(args).max().getAsDouble();
				double num = 0;
				double den = 0;
				for (int _J = 0; _J <= _K; _J++) {
					final double expVal = Math.exp(args[_J] - maxArg);
					num += _J * expVal;
					den += expVal;
				}
				return meanJ - num / den;
			}
		};

		double gamma0 = 0;
		double meanJminusPredictedJ0 = meanJMinusPredictedJ.value(gamma0);
		// Limit argument of exponential function: gamma * K = 15 <=> gamma = 15 / K
		double deltaGamma = (meanJminusPredictedJ0 < 0) ? -(0.1 * _K / 15.0) : +(0.1 * _K / 15.0);
		double gamma1 = gamma0 + deltaGamma;
		double f1 = meanJMinusPredictedJ.value(gamma1);
		final int maxIt = 1000 * 1000;
		int it = 0;
		while ((meanJminusPredictedJ0 * f1 > 0) && (it < maxIt)) {
			gamma0 = gamma1;
			meanJminusPredictedJ0 = f1;
			gamma1 += deltaGamma;
			f1 = meanJMinusPredictedJ.value(gamma1);
			it++;
		}
		if (it == maxIt) {
			throw new RuntimeException("Maximum number of iterations exceeded.");
		}

		final double gammaMin = Math.min(gamma0, gamma1);
		final double gammaMax = Math.max(gamma0, gamma1);
		this.gamma = new BrentSolver().solve(1000 * 1000, meanJMinusPredictedJ, gammaMin, gammaMax);

		this.roundTripLogProbasBySize = new double[_K + 1];
		final double[] args = this.args(_L, _K, gamma);
		final double maxArg = Arrays.stream(args).max().getAsDouble();
		final double logDen = Math.log(Arrays.stream(args).map(arg -> Math.exp(arg - maxArg)).sum());
		for (int _J = 0; _J <= _K; _J++) {
			this.roundTripLogProbasBySize[_J] = (this.gamma * _J - maxArg) - logDen;
		}
	}

	// -------------------- INTERNALS --------------------

	private double logOfRoundTripCnt(int _L, int _K, int _J) {
		return CombinatoricsUtils.binomialCoefficientLog(_K, _J) + _J * Math.log(_L);
	}

	private double[] args(int _L, int _K, double gamma) {
		final double[] args = new double[_K + 1];
		for (int _J = 0; _J <= _K; _J++) {
			args[_J] = gamma * _J + logOfRoundTripCnt(_L, _K, _J);
		}
		return args;
	}

	// -------------------- IMPLEMENTATION --------------------

	public String createTable() {
		final StringBuffer result = new StringBuffer(
				"J\tln(Pr(single roundtrip of size J))\tln(Pr(any round trip size J))\n");
		for (int _J = 0; _J <= this._K; _J++) {
			final double logProba = this.roundTripLogProbasBySize[_J];
			result.append(
					_J + "\t" + logProba + "\t" + (this.logOfRoundTripCnt(this._L, this._K, _J) + logProba) + "\n");
		}
		return result.toString();
	}

	// --------------- IMPLEMENTATION OF PreferenceComponent ---------------

	@Override
	public double logWeight(RoundTrip<L> roundTrip) {
		return this.roundTripLogProbasBySize[roundTrip.locationCnt()];
	}

	// -------------------- MAIN FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		int _K = 24;
//		 for (int _K = 1; _K <= 48; _K++)
		{
			int _L = 100;
//			 for (int _L = 1; _L <= 1000; _L++)
			{
//				for (double meanJ = 0.01 * _K; meanJ <= 0.99 * _K; meanJ += 0.01 * _K) 
				double meanJ = 4;
				{

					System.out.println(new MaximumEntropyPrior<>(_L, _K, meanJ).createTable());

				}
			}
		}
	}

}
