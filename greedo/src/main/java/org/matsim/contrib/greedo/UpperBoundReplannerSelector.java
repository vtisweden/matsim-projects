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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.greedo.shouldbeelsewhere.Hacks;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
class UpperBoundReplannerSelector extends AbstractReplannerSelector {

	// -------------------- CONSTANTS --------------------

//	private final double eps = 1e-8;

//	private final boolean checkDistance = true;

	private final boolean logReplanningProcess = true;

	private final Function<Double, Double> quadraticDistanceTransformation;

	// private final boolean gapRelativeMSA;
	final GreedoConfigGroup.UpperboundStepSize stepSizeLogic;

	// -------------------- MEMBERS --------------------

	private AbstractPopulationDistance populationDistance = null;

//	private Double initialGap = null;

	private Double sbaytiCounterpartGapThreshold = null;

	// -------------------- CONSTRUCTION --------------------

	UpperBoundReplannerSelector(final Function<Integer, Double> iterationToEta,
			final Function<Double, Double> quadraticDistanceTransformation,
			final GreedoConfigGroup.UpperboundStepSize stepSizeLogic
	// final boolean gapRelativeMSA
	) {
		super(iterationToEta);
		this.quadraticDistanceTransformation = quadraticDistanceTransformation;
		// this.gapRelativeMSA = gapRelativeMSA;
		this.stepSizeLogic = stepSizeLogic;
	}

	// -------------------- INTERNALS --------------------

	private double effectiveEta(final double currentGap) {
//		if (this.gapRelativeMSA) {
//			return Math.min(1.0, this.getTargetReplanningRate() * this.initialGap / currentGap);
//		} else {
//			return this.getTargetReplanningRate();
//		}
		if (GreedoConfigGroup.UpperboundStepSize.Vanilla.equals(this.stepSizeLogic)) {
			return this.getTargetReplanningRate();
		} else if (GreedoConfigGroup.UpperboundStepSize.RelativeToInitialGap.equals(this.stepSizeLogic)) {
			throw new UnsupportedOperationException();
			// return Math.min(1.0, this.getTargetReplanningRate() * this.initialGap /
			// currentGap);
		} else if (GreedoConfigGroup.UpperboundStepSize.SbaytiCounterpart.equals(this.stepSizeLogic)) {
			return (this.sbaytiCounterpartGapThreshold / currentGap);
		} else if (GreedoConfigGroup.UpperboundStepSize.SbaytiCounterpartExact.equals(this.stepSizeLogic)) {

			return (this.sbaytiGsum - this.sbaytiGcrit * this.sbaytiCnt) / currentGap;

		} else {
			throw new RuntimeException("Unknown step size logic: " + this.stepSizeLogic);
		}
	}

//	private double _Q(final double _G, final double _D2, final double epsilon) {
//		final double transformedD = this.quadraticDistanceTransformation.apply(Math.max(_D2, 0.0));
//		return (_G - epsilon) / Math.max(this.eps, transformedD);
//	}

	// --------------- OVERRIDING OF AbstractReplannerSelector ---------------

	@Override
	void setDistanceToReplannedPopulation(final AbstractPopulationDistance populationDistance) {
		this.populationDistance = populationDistance;
	}

	private Double sbaytiGsum = null;
	private Integer sbaytiCnt = null;
	private Double sbaytiGcrit = null;

	@Override
	Set<Id<Person>> selectReplannersHook(Map<Id<Person>, Double> personId2gap) {

//		// only consider strictly positive gaps
//		final Map<Id<Person>, Double> personId2gap = personId2gap_POSSIBLY_NEGATIVE_GAPS.entrySet().stream().filter(e -> e.getValue() > 0.0)
//				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

		/*
		 * (1) Initialize.
		 */

		if (GreedoConfigGroup.UpperboundStepSize.SbaytiCounterpart.equals(this.stepSizeLogic)) {
			final BasicReplannerSelector sbaytiSelector = new BasicReplannerSelector(true, this.iterationToStepSize);
			this.sbaytiCounterpartGapThreshold = sbaytiSelector
					.selectReplanners(personId2gap, this.getReplanIteration()).stream()
					.mapToDouble(id -> personId2gap.get(id)).sum();

		} else if (GreedoConfigGroup.UpperboundStepSize.SbaytiCounterpartExact.equals(this.stepSizeLogic)) {
			final BasicReplannerSelector sbaytiSelector = new BasicReplannerSelector(true, this.iterationToStepSize);
			final Set<Id<Person>> sbaytiReplanners = sbaytiSelector.selectReplanners(personId2gap,
					this.getReplanIteration());
			this.sbaytiGsum = sbaytiReplanners.stream().mapToDouble(id -> personId2gap.get(id)).sum();
			this.sbaytiGcrit = sbaytiReplanners.stream().mapToDouble(id -> personId2gap.get(id)).min().getAsDouble();
			this.sbaytiCnt = sbaytiReplanners.size();
		}

		// Start with a maximum amount of replanning gap.
		final Set<Id<Person>> replannerIds = personId2gap.entrySet().stream().filter(e -> e.getValue() > 0.0)
				.map(e -> e.getKey()).collect(Collectors.toSet());
		if (replannerIds.size() == 0) {
			return Collections.emptySet();
		}

		final TransformedObjectiveFunction obj1 = new TransformedObjectiveFunction(this.quadraticDistanceTransformation,
				this.populationDistance, personId2gap, replannerIds);

//		final Map<Id<Person>, Double> personId2bParam = new LinkedHashMap<>(personId2gap.size());
//		for (Id<Person> personId : personId2gap.keySet()) {
//			double b = 0.0;
//			for (Id<Person> replannerId : replannerIds) {
//				b += this.populationDistance.getACoefficient(replannerId, personId)
//						+ this.populationDistance.getACoefficient(personId, replannerId);
//			}
//			personId2bParam.put(personId, b);
//		}

		final String logFile = "exact-replanning.log";
		if (this.logReplanningProcess) {
			Hacks.append2file(logFile, "strictly positive gaps: "
					+ ((double) personId2gap.size()) / ((double) personId2gap.size()) + "\n");
			Hacks.append2file(logFile, "G(lambda)\tD(lambda)\tQ(lambda)\n");
		}

		final double _Gall = personId2gap.entrySet().stream().mapToDouble(e -> e.getValue()).sum();
//		final double _D2all = personId2bParam.entrySet().stream().mapToDouble(e -> e.getValue()).sum();

//		if (this.initialGap == null) {
//			this.initialGap = _Gall;
//		}

//		double _G = personId2gap.entrySet().stream().filter(e -> replannerIds.contains(e.getKey()))
//				.mapToDouble(e -> e.getValue()).sum();
//		double _D2 = 0.5 * personId2bParam.entrySet().stream().filter(e -> replannerIds.contains(e.getKey()))
//				.mapToDouble(e -> e.getValue()).sum();
//		double _G = replannerIds.stream().mapToDouble(r -> personId2gap.get(r)).sum();
//		double _D2 = 0.5 * replannerIds.stream().mapToDouble(r -> personId2bParam.get(r)).sum();

		/*
		 * (2) Repeatedly switch (non)replanners.
		 */

		final List<Id<Person>> allPersonIds = new LinkedList<>(personId2gap.keySet());
		boolean switched = true;

		while (switched) {

			if (this.logReplanningProcess) {
//				Hacks.append2file(logFile,
//						_G + "\t" + Math.sqrt(_D2) + "\t" + this._Q(_G, _D2, this.effectiveEta(_Gall) * _Gall) + "\n");
				Hacks.append2file(logFile, obj1.getG() + "\t" + Math.sqrt(obj1.getD2()) + "\t"
						+ obj1.getQ(this.effectiveEta(_Gall) * _Gall) + "\n");
			}

			switched = false;
			Collections.shuffle(allPersonIds);

			for (Id<Person> candidateId : allPersonIds) {

				final double candidateGap = personId2gap.get(candidateId);
//				final double a = this.populationDistance.getACoefficient(candidateId, candidateId);
//				final double b = personId2bParam.get(candidateId);
//
//				final double deltaG;
//				final double deltaD2;
//				if (replannerIds.contains(candidateId)) {
//					deltaG = -candidateGap;
//					deltaD2 = -b + a;
//				} else /* candidate is NOT a replanner */ {
//					deltaG = +candidateGap;
//					deltaD2 = +b + a;
//				}

				obj1.setSwitchingCandidate(candidateId, replannerIds.contains(candidateId), candidateGap);

				// attention, now we maximize

//				final double oldQ = this._Q(_G, _D2, this.effectiveEta(_Gall) * _Gall);
//				final double newQ = this._Q(_G + deltaG, _D2 + deltaD2, this.effectiveEta(_Gall) * _Gall);
//
//				if (Math.abs(obj1.getDeltaQ(this.effectiveEta(_Gall) * _Gall) - (newQ - oldQ)) > 1e-8) {
//					throw new RuntimeException("deltaQ is " + (newQ - oldQ) + " but obj1 predicts "
//							+ obj1.getDeltaQ(this.effectiveEta(_Gall) * _Gall));
//				}

				final double deltaQ = obj1.getDeltaQ(this.effectiveEta(_Gall) * _Gall);
				
				if (deltaQ > 0) {
//					if (newQ > oldQ) {
//					_G = Math.max(0.0, _G + deltaG);
//					_D2 = Math.max(0.0, _D2 + deltaD2);

//					final double deltaSign;
					if (replannerIds.contains(candidateId)) {
						replannerIds.remove(candidateId);
//						deltaSign = -1.0;
					} else /* candidate is NOT a replanner */ {
						replannerIds.add(candidateId);
//						deltaSign = +1.0;
					}
//					for (Id<Person> personId : personId2gap.keySet()) {
//						final double deltaB = deltaSign
//								* (this.populationDistance.getACoefficient(candidateId, personId)
//										+ this.populationDistance.getACoefficient(personId, candidateId));
//						personId2bParam.compute(personId, (id, b2) -> b2 + deltaB);
//					}
					
					switched = true;
					obj1.confirmSwitch(true);

//					if (this.checkDistance) {
//						final double _Gchecked = personId2gap.entrySet().stream()
//								.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();
//						final double _D2checkedB = 0.5 * personId2bParam.entrySet().stream()
//								.filter(e -> replannerIds.contains(e.getKey())).mapToDouble(e -> e.getValue()).sum();
//						final boolean gErr = Math.abs(_Gchecked - _G) > 1e-4;
//						final boolean d2ErrB = Math.abs(_D2checkedB - _D2) > 1e-4;
//						if (gErr || d2ErrB) {
//							String msg = "";
//							if (gErr) {
//								msg += "\nrecursive _G = " + _G + ", but checked _G = " + _Gchecked;
//							}
//							if (d2ErrB) {
//								msg += "\nrecursive _D2 = " + _D2 + ", but checked _D2(B) = " + _D2checkedB;
//							}
//							throw new RuntimeException(msg);
//						}
//					}
				} else {
					obj1.confirmSwitch(false);
				}
			}
		}

//		if (this.logReplanningProcess) {
//			Hacks.append2file(logFile, "homogeneity = " + (_Gall / Math.sqrt(_D2all)) / (_G / Math.sqrt(_D2)) + "\n");
//			Hacks.append2file(logFile, "\n");
//		}

		return replannerIds;
	}
}
