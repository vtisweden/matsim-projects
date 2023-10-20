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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;

/**
 * 
 * @author GunnarF
 *
 */
class TransformedObjectiveFunction {

	private final double eps = 1e-8;

	// -------------------- MEMBERS--------------------

	private final Function<Double, Double> quadraticDistanceTransformation;

	private final AbstractPopulationDistance populationDistance;

	private final Map<Id<Person>, Double> personId2gap;

	private final Map<Id<Person>, Double> personId2bParam;

	private Double _G = null;

	private Double _D2 = null;

	private Double deltaG = null;

	private Double deltaD2 = null;

	private Id<Person> candidateId = null;

	private Boolean candidateIsReplanner = null;

	// -------------------- CONSTRUCTION --------------------

	TransformedObjectiveFunction(final Function<Double, Double> quadraticDistanceTransformation,
			final AbstractPopulationDistance populationDistance, Map<Id<Person>, Double> personId2gap,
			Set<Id<Person>> initialReplannerIds) {

		this.quadraticDistanceTransformation = quadraticDistanceTransformation;
		this.populationDistance = populationDistance;
		this.personId2gap = personId2gap;

		this.personId2bParam = new LinkedHashMap<>(personId2gap.size());
		for (Id<Person> personId : personId2gap.keySet()) {
			double b = 0.0;
			for (Id<Person> replannerId : initialReplannerIds) {
				b += this.populationDistance.getACoefficient(replannerId, personId)
						+ this.populationDistance.getACoefficient(personId, replannerId);
			}
			this.personId2bParam.put(personId, b);
		}

		this._G = initialReplannerIds.stream().mapToDouble(r -> personId2gap.get(r)).sum();
		this._D2 = 0.5 * initialReplannerIds.stream().mapToDouble(r -> this.personId2bParam.get(r)).sum();
	}

	// -------------------- INTERNALS --------------------

	private double _Q(final double _G, final double _D2, final double gamma) {
		final double transformedD = this.quadraticDistanceTransformation.apply(Math.max(_D2, 0.0));
		return (_G - gamma) / Math.max(this.eps, transformedD);
	}

	// -------------------- IMPLEMENTATION --------------------

	void setSwitchingCandidate(final Id<Person> candidateId, final boolean isReplanner, final double candidateGap) {
		if (this.candidateId != null) {
			throw new RuntimeException("There is already an unconfirmed candidate being considered.");
		}
		this.candidateId = candidateId;
		this.candidateIsReplanner = isReplanner;
		final double a = this.populationDistance.getACoefficient(candidateId, candidateId);
		final double b = this.personId2bParam.get(candidateId);
		if (isReplanner) {
			this.deltaG = -candidateGap;
			this.deltaD2 = -b + a;
		} else {
			this.deltaG = +candidateGap;
			this.deltaD2 = +b + a;
		}
	}

	Double getG() {
		return this._G;
	}
	
	Double getD2() {
		return this._D2;
	}
	
	double getQ(final double gamma) {
		return this._Q(this._G, this._D2, gamma);
	}

//	double getCandidateQ(final double gamma) {
//		return this._Q(this._G + this.deltaG, this._D2 + this.deltaD2, gamma);
//	}

	double getDeltaQ(final double gamma) {
		return this._Q(this._G + this.deltaG, this._D2 + this.deltaD2, gamma) - this._Q(this._G, this._D2, gamma);
	}

	void confirmSwitch(final boolean isSwitch) {
		if (isSwitch) {
			this._G = Math.max(0.0, this._G + this.deltaG);
			this._D2 = Math.max(0.0, this._D2 + this.deltaD2);

			final double deltaSign;
			if (this.candidateIsReplanner) {
				deltaSign = -1.0;
			} else {
				deltaSign = +1.0;
			}
			for (Id<Person> personId : personId2gap.keySet()) {
				final double deltaB = deltaSign * (this.populationDistance.getACoefficient(this.candidateId, personId)
						+ this.populationDistance.getACoefficient(personId, this.candidateId));
				this.personId2bParam.compute(personId, (id, b2) -> b2 + deltaB);
			}
		}

		// To not mess up the bookkeeping when considering the next switcher.
		this.deltaG = null;
		this.deltaD2 = null;
		this.candidateId = null;
		this.candidateIsReplanner = null;
	}

}
