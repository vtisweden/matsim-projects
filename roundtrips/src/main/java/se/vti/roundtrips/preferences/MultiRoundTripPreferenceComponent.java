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
package se.vti.roundtrips.preferences;

import java.util.Arrays;
import java.util.function.Function;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.single.Location;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class MultiRoundTripPreferenceComponent<L extends Location> extends PreferenceComponent<MultiRoundTrip<L>> {

	private double lastDeviationError;
	private double lastDiscretizationError;

	private double[] target;
	private double targetSize;

	private Function<MultiRoundTrip<L>, MultiRoundTrip<L>> filter = m -> m;

	public MultiRoundTripPreferenceComponent() {
	}

	public void setFilter(Function<MultiRoundTrip<L>, MultiRoundTrip<L>> filter) {
		this.filter = filter;
	}

	public double getLastDeviationError() {
		return this.lastDeviationError;
	}

	public double getLastDiscretizationError() {
		return this.lastDiscretizationError;
	}

	public MultiRoundTrip<L> filter(MultiRoundTrip<L> multiRoundTrip) {
		return this.filter.apply(multiRoundTrip);
	}

	@Override
	public double logWeight(MultiRoundTrip<L> multiRoundTrip) {

		multiRoundTrip = this.filter(multiRoundTrip);

		final double[] sample = this.computeSample(multiRoundTrip);
		final double sampleSize = Math.max(Arrays.stream(sample).sum(), 1e-8);

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
		this.lastDiscretizationError = 0.5 * slack * this.target.length;
		return (-1.0) * multiRoundTrip.size() * (this.lastDeviationError + this.lastDiscretizationError);
	}

	public abstract String[] createLabels();

	public abstract double[] computeTarget();

	public abstract double[] computeSample(MultiRoundTrip<L> filteredMultiRoundTrip);

}
