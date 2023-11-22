/**
 * org.matsim.contrib.greedo
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package org.matsim.contrib.greedo;

import java.util.LinkedList;

/**
 * 
 * @author GunnarF
 *
 */
class AmbitionGapSchedule {

	// -------------------- CONSTANTS --------------------

	private final int warmupIterations;

	private final double iterationToLevelExponent;

	// -------------------- MEMBERS --------------------

	private Double initialGap = null;

	private final LinkedList<Double> gaps = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	AmbitionGapSchedule(final int warmupIterations, final double iterationToLevelReductionExponent) {
		if (warmupIterations < 2) {
			throw new IllegalArgumentException("warmupIterations must be >= 2");
		}
		if (iterationToLevelReductionExponent > 0) {
			throw new IllegalArgumentException("iterationToLevelReductionException must be non-positive");
		}
		this.warmupIterations = warmupIterations;
		this.iterationToLevelExponent = iterationToLevelReductionExponent;
	}

	// -------------------- IMPLEMENTATION --------------------

	void registerGap(final double gap) {
		this.gaps.addFirst(gap);
	}

	double getEta(final int iteration, final boolean constrain) {
		final double etaMSA = Math.pow(1 + iteration, this.iterationToLevelExponent);
		final double eta;
		if (iteration < this.warmupIterations) {
			eta = etaMSA;
		} else {
			final double meanGap = this.gaps.subList(0, iteration / 2).stream().mapToDouble(g -> g).average()
					.getAsDouble();
			if (this.initialGap == null) {
				assert (iteration == 2 * this.warmupIterations);
				this.initialGap = meanGap;
			}
			eta = Math.pow(1.0 + iteration, this.iterationToLevelExponent) * (this.initialGap / meanGap);
		}
		if (constrain) {
			return Math.max(0.0, Math.min(1.0, eta));
		} else {
			return eta;
		}
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		AmbitionGapSchedule ags = new AmbitionGapSchedule(10, -0.5);

		System.out.println("gap\teta");
		for (int it = 0; it < 1000; it++) {
			double gap = 200.0 * Math.pow(1 + it, -0.25);
			gap += 0.2 * (Math.random() - 0.5) * gap;

			ags.registerGap(gap);

			System.out.println(gap + "\t" + ags.getEta(it, false));

		}

	}

}
