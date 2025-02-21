/**
 * se.vti.roundtrips.parallel
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.single;

import java.util.LinkedList;
import java.util.List;

import se.vti.roundtrips.model.Scenario;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
class RoundTripTransitionKernel {

	// -------------------- CONSTANTS --------------------

	// for testing
	enum Action {
		INS, REM, FLIP_LOC, FLIP_DEP
	};

	private final RoundTrip<?> from;
	private final Scenario<?> scenario;

	public final double insertProba;
	public final double removeProba;
	public final double flipLocationProba;
	public final double flipDepTimeProba;

	private final double transitionProbaGivenFlipLocation;
	private final double transitionProbaGivenFlipDepTime;

	// -------------------- CONSTRUCTION --------------------

	RoundTripTransitionKernel(RoundTrip<?> from, Scenario<?> scenario, SimplifiedRoundTripProposalParameters params) {
		this.from = from;
		this.scenario = scenario;

		double effectiveInsertWeight = (from.locationCnt() < Math.min(scenario.getMaxStayEpisodes(),
				scenario.getBinCnt()) ? params.insertWeight : 0.0);
		double effectiveRemoveWeight = (from.locationCnt() > 1 ? params.removeWeight : 0.0);
		double effectiveFlipLocationWeight = params.flipLocationWeight;
		double effectiveFlipDepTimeWeight = (from.locationCnt() < scenario.getBinCnt() ? params.flipDepTimeWeight
				: 0.0);
		final double effectiveWeightSum = effectiveInsertWeight + effectiveRemoveWeight + effectiveFlipLocationWeight
				+ effectiveFlipDepTimeWeight;

		this.insertProba = effectiveInsertWeight / effectiveWeightSum;
		this.removeProba = effectiveRemoveWeight / effectiveWeightSum;
		this.flipLocationProba = effectiveFlipLocationWeight / effectiveWeightSum;
		this.flipDepTimeProba = effectiveFlipDepTimeWeight / effectiveWeightSum;

		assert (Math.abs(
				1.0 - this.insertProba - this.removeProba - this.flipLocationProba - this.flipDepTimeProba) < 1e-8);

		this.transitionProbaGivenFlipLocation = 1.0 / from.locationCnt() / (scenario.getLocationCnt() - 1);
		this.transitionProbaGivenFlipDepTime = 1.0 / from.locationCnt() / (scenario.getBinCnt() - from.locationCnt());
	}

	// -------------------- INTERNALS --------------------

	private double numberOfInsertionPoints(List<?> shorter, List<?> longer) {
		assert (shorter.size() + 1 == longer.size());
		int result = 0;
		LinkedList<Object> tmp = new LinkedList<>(shorter);
		for (int i = 0; i < longer.size(); i++) {
			tmp.add(i, longer.get(i));
			if (tmp.equals(longer)) {
				result++;
			}
			tmp.remove(i);
			assert(tmp.equals(shorter));
		}
		return result;
	}

	private double transitionProbaGivenInsert(RoundTrip<?> to) {
		return this.numberOfInsertionPoints(this.from.getLocationsView(), to.getLocationsView())
				/ (this.from.locationCnt() + 1.0) / this.scenario.getLocationCnt()
				/ (this.scenario.getBinCnt() - this.from.locationCnt());
	}

	private double numberOfRemovalPoints(List<?> longer, List<?> shorter) {
		assert(shorter.size() + 1 == longer.size());
		int result = 0;
		LinkedList<Object> tmp = new LinkedList<>(longer);
		for (int i = 0; i < longer.size(); i++) {
			Object removed = tmp.remove(i);
			assert(tmp.size() == shorter.size());
			if (tmp.equals(shorter)) {
				result++;
			}
			tmp.add(i, removed);
			assert(tmp.equals(longer));
		}
		assert(result > 0);
		return result;
	}

	private double transitionProbaGivenRemove(RoundTrip<?> to) {
		double result = this.numberOfRemovalPoints(this.from.getLocationsView(), to.getLocationsView()) 
				/this.from.locationCnt() / this.from.locationCnt();
		assert(result > 0);
		return result;
	}

	// -------------------- IMPLEMENTATION --------------------

	// for testing
	Action identifyAction(RoundTrip<?> to) {
		if (this.from.locationCnt() + 1 == to.locationCnt()) {
			return Action.INS;
		} else if (this.from.locationCnt() - 1 == to.locationCnt()) {
			return Action.REM;
		} else if (!this.from.getLocationsView().equals(to.getLocationsView())) {
			return Action.FLIP_LOC;
		} else if (!this.from.getDeparturesView().equals(to.getDeparturesView())) {
			return Action.FLIP_DEP;
		} else {
			return null;
		}
	}

	// TODO This assumes that the transition from -> to followed the same kernel.
	double transitionProba(RoundTrip<?> to) {
		double result;
		if (this.from.locationCnt() + 1 == to.locationCnt()) {
			assert (this.insertProba * this.transitionProbaGivenInsert(to) > 0);
			result = this.insertProba * this.transitionProbaGivenInsert(to);
		} else if (this.from.locationCnt() - 1 == to.locationCnt()) {
			assert (this.removeProba * this.transitionProbaGivenRemove(to) > 0);
			result = this.removeProba * this.transitionProbaGivenRemove(to);
		} else if (!this.from.getLocationsView().equals(to.getLocationsView())) {
			assert (this.flipLocationProba * this.transitionProbaGivenFlipLocation > 0);
			result = this.flipLocationProba * this.transitionProbaGivenFlipLocation;
		} else if (!this.from.getDeparturesView().equals(to.getDeparturesView())) {
			assert (this.flipDepTimeProba * this.transitionProbaGivenFlipDepTime > 0);
			result = this.flipDepTimeProba * this.transitionProbaGivenFlipDepTime;
		} else {
			throw new UnsupportedOperationException();
		}
		assert(result > 0);
		return result;
	}
}
