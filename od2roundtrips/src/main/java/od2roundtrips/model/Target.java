/**
 * od2roundtrips.model
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
 * 
 * VTI = Swedish National Road and Transport Institute
 * LiU = Linköping University, Sweden
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation, either 
 * version 3 of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>. See also COPYING and WARRANTY file.
 */
package od2roundtrips.model;

import java.util.Arrays;
import java.util.function.Function;

import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class Target extends PreferenceComponent<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> {

	private double lastDeviationError;
	private double lastDiscretizationError;

	private double[] target;
	private double targetSize;

	private Function<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>, MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> filter = m -> m;

	public Target() {
	}

	public void setFilter(
			Function<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>, MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> filter) {
		this.filter = filter;
	}

	public double getLastDeviationError() {
		return this.lastDeviationError;
	}

	public double getLastDiscretizationError() {
		return this.lastDiscretizationError;
	}

	@Override
	public double logWeight(MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> multiRoundTrip) {

		multiRoundTrip = this.filter.apply(multiRoundTrip);

		final double[] sample = this.computeSample(multiRoundTrip);
		final double sampleSize = Arrays.stream(sample).sum();

		if (this.target == null) {
			this.target = this.computeTarget();
			this.targetSize = Arrays.stream(this.target).sum();
		}

		double slack = 0.5 / sampleSize;
		this.lastDeviationError = 0.0;
		for (int i = 0; i < this.target.length; i++) {
			this.lastDeviationError += Math.max(0.0,
					Math.abs(sample[i] / sampleSize - this.target[i] / this.targetSize) - slack);
		}
		this.lastDiscretizationError = slack * this.target.length;
		return (-1.0) * multiRoundTrip.size() * (this.lastDeviationError + this.lastDiscretizationError);
	}

	public abstract double[] computeTarget();

	public abstract double[] computeSample(MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> multiRoundTrip);

}
