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

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.PopulationGroupFilter;
import se.vti.roundtrips.single.Location;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class MultiRoundTripTargetDeviationPreferenceComponent<L extends Location>
		extends PreferenceComponent<MultiRoundTrip<L>> {

	// -------------------- MEMBERS --------------------

	private PopulationGroupFilter<L> filter = null;

	private double[] target;

	private double targetSize;

	private double lastDeviationError;

	private double lastDiscretizationError;

	// -------------------- CONSTRUCTION --------------------

	public MultiRoundTripTargetDeviationPreferenceComponent() {
	}

	// -------------------- SETTERS & GETTERS --------------------

	public void setFilter(PopulationGroupFilter<L> filter) {
		this.filter = filter;
	}

	public PopulationGroupFilter<L> getFilter() {
		return this.filter;
	}

	public double getLastDeviationError() {
		return this.lastDeviationError;
	}

	public double getLastDiscretizationError() {
		return this.lastDiscretizationError;
	}
	
	public double[] computeTargetIfAbsent() {
		if (this.target == null) {
			this.target = this.computeTarget();
			this.targetSize = Arrays.stream(this.target).sum();
		}
		return this.target;
	}

	// --------------- IMPLEMENTATION OF MHPreferenceComponent ---------------

	@Override
	public double logWeight(MultiRoundTrip<L> multiRoundTrip) {

		final double[] sample = this.computeSample(multiRoundTrip, this.filter);
		final double sampleSize = Math.max(Arrays.stream(sample).sum(), 1e-8);

//		if (this.target == null) {
//			this.target = this.computeTarget();
//			this.targetSize = Arrays.stream(this.target).sum();
//		}
		this.computeTargetIfAbsent();
		
		double slack = 0.5 / sampleSize;
		this.lastDeviationError = 0.0;
		for (int i = 0; i < this.target.length; i++) {
			this.lastDeviationError += Math.max(0.0,
					Math.abs(sample[i] / sampleSize - this.target[i] / this.targetSize) - slack);
		}
		this.lastDiscretizationError = 0.5 * slack * this.target.length;
		return (-1.0) * (this.filter == null ? multiRoundTrip.size() : this.filter.getGroupSize())
				* (this.lastDeviationError + this.lastDiscretizationError);
	}

	// --------------- ABSTRACT FUNCTIONS ---------------
	
	public abstract String[] createLabels();

	public abstract double[] computeTarget();

	public abstract double[] computeSample(MultiRoundTrip<L> multiRoundTrip, PopulationGroupFilter<L> filter);

}
