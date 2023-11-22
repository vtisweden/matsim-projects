/**
 * org.matsim.contrib.emulation
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

import floetteroed.utilities.math.BasicStatistics;

class AmbitionGapSchedule {

	private final int warmupIterations;
	private final double iterationToLevelReductionExponent;

	private final boolean strictReduce;

	private Double initialGap;
	private Double lastEta;

	private final BasicStatistics initialStats = new BasicStatistics();
	private final LinkedList<Double> gaps = new LinkedList<>();

	AmbitionGapSchedule(final int warmupIterations, final double iterationToLevelReductionExponent,
			final boolean strictReduce) {
		this.warmupIterations = warmupIterations;
		this.iterationToLevelReductionExponent = iterationToLevelReductionExponent;
		this.strictReduce = strictReduce;
	}

	void registerGap(final double gap) {
		if (this.initialStats.size() < this.warmupIterations) {
			this.initialStats.add(gap);
		}
		this.gaps.addFirst(gap);
	}

	double getEta(final int iteration, final boolean constrain) {
		final double etaMSA = Math.pow(1 + iteration, this.iterationToLevelReductionExponent);
		if (iteration < 2 * this.warmupIterations) {
			this.lastEta = etaMSA;
		} else {
			final double meanGap = this.gaps.subList(0, iteration / 2).stream().mapToDouble(g -> g).average()
					.getAsDouble();
			if (this.initialGap == null) {
				this.initialGap = meanGap;
			}
			final double newEta = this.initialGap / meanGap
					* Math.pow(0.75 * (1.0 + iteration), this.iterationToLevelReductionExponent);
			if (this.strictReduce && (this.lastEta != null)) {
				this.lastEta = Math.min(this.lastEta, newEta);
			} else {
				this.lastEta = newEta;
			}
		}
		if (constrain) {
			return Math.max(0.0, Math.min(1.0, this.lastEta));
		} else {
			return this.lastEta;
		}
	}

	public static void main(String[] args) {

		AmbitionGapSchedule ags = new AmbitionGapSchedule(10, -1.0, false);
		AmbitionGapSchedule agsStrict = new AmbitionGapSchedule(10, -1.0, true);

		System.out.println("gap\teta\tetastrict");
		for (int it = 0; it < 1000; it++) {
			double gap = 200.0 * Math.pow(1 + it, -0.5);
			gap += 0.2 * (Math.random() - 0.5) * gap;

			ags.registerGap(gap);
			agsStrict.registerGap(gap);

			System.out.println(gap + "\t" + ags.getEta(it, false) + "\t" + agsStrict.getEta(it, false));

		}

	}

}
