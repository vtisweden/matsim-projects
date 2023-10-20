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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.shouldbeelsewhere.Hacks;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class TwoPointUpperBoundReplannerSelector extends AbstractReplannerSelector {

	// -------------------- CONSTANTS --------------------

//	private final boolean logReplanningProcess = true;

	private final Function<Double, Double> quadraticDistanceTransformation;

	final GreedoConfigGroup.UpperboundStepSize stepSizeLogic;

	// -------------------- MEMBERS --------------------

	private Set<Id<Person>> previousReplanners = null;

	private Map<Id<Person>, Double> personId2gap1 = null;
	private Map<Id<Person>, Double> personId2gap2 = null;

	private AbstractPopulationDistance populationDistance1 = null;
	private AbstractPopulationDistance populationDistance2 = null;

	// -------------------- CONSTRUCTION --------------------

	TwoPointUpperBoundReplannerSelector(final Function<Integer, Double> iterationToEta,
			final Function<Double, Double> quadraticDistanceTransformation,
			final GreedoConfigGroup.UpperboundStepSize stepSizeLogic) {
		super(iterationToEta);
		this.quadraticDistanceTransformation = quadraticDistanceTransformation;
		this.stepSizeLogic = stepSizeLogic;

		super.hasReplannedBefore = true; // TODO To avoid initial 100% replanning.
	}

	// -------------------- INTERNALS --------------------

	private double effectiveEta(final double currentGap) {
		if (GreedoConfigGroup.UpperboundStepSize.Vanilla.equals(this.stepSizeLogic)) {
			return this.getTargetReplanningRate();
		} else {
			throw new RuntimeException("Unknown step size logic: " + this.stepSizeLogic);
		}
	}

	private void switchReplanner(final Id<Person> switcher, final Set<Id<Person>> replanners) {
		if (replanners.contains(switcher)) {
			replanners.remove(switcher);
		} else {
			replanners.add(switcher);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	void setPreviousReplanners(final Set<Id<Person>> previousReplanners) {
		this.previousReplanners = previousReplanners;
	}

	void setFirstPoint(final Map<Id<Person>, Double> personId2gap,
			final AbstractPopulationDistance populationDistance) {
		this.personId2gap1 = personId2gap;
		this.populationDistance1 = populationDistance;
	}

	void setSecondPoint(final Map<Id<Person>, Double> personId2gap,
			final AbstractPopulationDistance populationDistance) {
		this.personId2gap2 = personId2gap;
		this.populationDistance2 = populationDistance;
	}

	@Override
	Set<Id<Person>> selectReplannersHook(Map<Id<Person>, Double> IGNORE_personId2gap) {

		/*
		 * (1) Initialize.
		 */

		final Set<Id<Person>> replannerIds1 = new LinkedHashSet<>(this.previousReplanners);
		final TransformedObjectiveFunction obj1 = new TransformedObjectiveFunction(this.quadraticDistanceTransformation,
				this.populationDistance1, this.personId2gap1, replannerIds1);

		// The previous replanners led to the current point, meaning that there is
		// initially no replanning away from that point.
		final Set<Id<Person>> replannerIds2 = new LinkedHashSet<>();
		final TransformedObjectiveFunction obj2 = new TransformedObjectiveFunction(this.quadraticDistanceTransformation,
				this.populationDistance2, this.personId2gap2, replannerIds2);

//		final String logFile = "exact-replanning.log";
//		if (this.logReplanningProcess) {
//			Hacks.append2file(logFile, "strictly positive gaps: "
//					+ ((double) personId2gap.size()) / ((double) personId2gap.size()) + "\n");
//			Hacks.append2file(logFile, "G(lambda)\tD(lambda)\tQ(lambda)\n");
//		}

		final double _Gall1 = personId2gap1.entrySet().stream()
				.mapToDouble(e -> Math.max(Double.NEGATIVE_INFINITY, e.getValue())).sum();
		final double _Gall2 = personId2gap2.entrySet().stream()
				.mapToDouble(e -> Math.max(Double.NEGATIVE_INFINITY, e.getValue())).sum();

		/*
		 * (2) Repeatedly switch (non)replanners.
		 */

		final List<Id<Person>> allCandidateIds = new LinkedList<>(this.personId2gap1.keySet());
		boolean switched = true;

		while (switched) {

//			if (this.logReplanningProcess) {
//				Hacks.append2file(logFile, obj1.getG() + "\t" + Math.sqrt(obj1.getD2()) + "\t"
//						+ obj1.getQ(this.effectiveEta(_Gall) * _Gall) + "\n");
//			}

			switched = false;
			Collections.shuffle(allCandidateIds);

			for (Id<Person> candidateId : allCandidateIds) {

				obj1.setSwitchingCandidate(candidateId, replannerIds1.contains(candidateId),
						this.personId2gap1.get(candidateId));
				obj2.setSwitchingCandidate(candidateId, replannerIds2.contains(candidateId),
						this.personId2gap2.get(candidateId));

				// attention, now we maximize

				final double gamma1 = this.effectiveEta(_Gall1) * _Gall1;
				final double gamma2 = Math.max(0.0, gamma1 + (_Gall2 - _Gall1));

				Hacks.append2file("Q.txt", "gamma1 = " + gamma1 + ", gamma2 = " + gamma2 + "\n");
				Hacks.append2file("Q.txt", "Q1 = " + obj1.getQ(gamma1) + ", deltaQ1 = " + obj1.getDeltaQ(gamma1)
						+ ", Q2 = " + obj2.getQ(gamma2) + ", deltaQ2 = " + obj2.getDeltaQ(gamma2) + "\n\n");

				final double deltaQ = Math.max(obj1.getQ(gamma1) + obj1.getDeltaQ(gamma1),
						obj2.getQ(gamma2) + obj2.getDeltaQ(gamma2)) - Math.max(obj1.getQ(gamma1), obj2.getQ(gamma2));
//				final double deltaQ = obj1.getDeltaQ(gamma1);

				if (deltaQ > 0) {

					this.switchReplanner(candidateId, replannerIds1);
					this.switchReplanner(candidateId, replannerIds2);

					obj1.confirmSwitch(true);
					obj2.confirmSwitch(true);

					switched = true;

				} else {
					obj1.confirmSwitch(false);
					obj2.confirmSwitch(false);
				}
			}
		}

//		if (this.logReplanningProcess) {
//			Hacks.append2file(logFile, "homogeneity = " + (_Gall / Math.sqrt(_D2all)) / (_G / Math.sqrt(_D2)) + "\n");
//			Hacks.append2file(logFile, "\n");
//		}

		return replannerIds1;
	}

	// --------------- OVERRIDING OF AbstractReplannerSelector ---------------

	@Override
	void setDistanceToReplannedPopulation(final AbstractPopulationDistance populationDistance) {
		throw new UnsupportedOperationException();
	}

}
