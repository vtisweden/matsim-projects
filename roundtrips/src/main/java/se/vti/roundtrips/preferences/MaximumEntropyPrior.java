package se.vti.roundtrips.preferences;

import java.util.Arrays;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.util.CombinatoricsUtils;

import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class MaximumEntropyPrior extends PreferenceComponent<RoundTrip<?>> {

	public final int _L;
	public final int _K;
	public final double meanJ;

	public final double gamma;

	private final double[] roundTripLogProbasBySize;

	public MaximumEntropyPrior(int _L, int _K, double meanJ) {
		assert (meanJ < _K);

		this._L = _L;
		this._K = _K;
		this.meanJ = meanJ;

		UnivariateFunction _Jdiff = new UnivariateFunction() {
			@Override
			public double value(double gamma) {

				final double[] args = new double[_K + 1];
				for (int _J = 0; _J <= _K; _J++) {
					args[_J] = gamma * _J + CombinatoricsUtils.binomialCoefficientLog(_K, _J) + _J * Math.log(_L);
				}
				final double maxArg = Arrays.stream(args).max().getAsDouble();

				double num = 0;
				double den = 0;
				for (int _J = 0; _J <= _K; _J++) {
					final double expVal = Math.exp(args[_J] - maxArg);
					num += _J * expVal;
					den += expVal;
				}

				return num / den - meanJ;
			}
		};

		double gamma0 = 0;
		double f0 = _Jdiff.value(gamma0);

		// gamma * K = 15 <=> gamma = 15 / K
		double dir = f0 < 0 ? +(0.05 * _K / 15.0) : -(0.05 * _K / 15.0);
		double gamma1 = gamma0 + dir;
		double f1 = _Jdiff.value(gamma1);
		while (f0 * f1 > 0 && Math.abs(gamma1) < 30) {
			gamma0 = gamma1;
			f0 = f1;
			gamma1 += dir;
			f1 = _Jdiff.value(gamma1);
		}

		double gammaMin = Math.min(gamma0, gamma1);
		double gammaMax = Math.max(gamma0, gamma1);

		BrentSolver solver = new BrentSolver();
		this.gamma = solver.solve(100 * 1000, _Jdiff, gammaMin, gammaMax);

		this.roundTripLogProbasBySize = new double[_K + 1];

		final double[] args = new double[_K + 1];
		for (int _J = 0; _J <= _K; _J++) {
			args[_J] = this.gamma * _J + CombinatoricsUtils.binomialCoefficientLog(_K, _J) + _J * Math.log(_L);
		}
		final double maxArg = Arrays.stream(args).max().getAsDouble();
		final double den = Arrays.stream(args).map(arg -> Math.exp(arg - maxArg)).sum();

		for (int _J = 0; _J <= _K; _J++) {
			this.roundTripLogProbasBySize[_J] = (this.gamma * _J - maxArg) - Math.log(den);
			final double logSizeProba = (args[_J] - maxArg) - Math.log(den);
			System.out.println(_J + "\t" + this.roundTripLogProbasBySize[_J] + "\t" + logSizeProba);
		}
	}

	@Override
	public double logWeight(RoundTrip<?> roundTrip) {
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

					new MaximumEntropyPrior(_L, _K, meanJ);

				}
			}
		}
	}

}
