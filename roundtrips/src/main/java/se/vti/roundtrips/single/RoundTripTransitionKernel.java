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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import se.vti.roundtrips.common.Node;
import se.vti.roundtrips.common.Scenario;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
class RoundTripTransitionKernel<L extends Node> {

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

	RoundTripTransitionKernel(RoundTrip<L> from, Scenario<L> scenario, RoundTripProposalParameters params) {
		this.from = from;
		this.scenario = scenario;

		double effectiveInsertWeight = (from.size() < Math.min(scenario.getUpperBoundOnStayEpisodes(),
				scenario.getTimeBinCnt()) ? params.insertWeight : 0.0);
		double effectiveRemoveWeight = (from.size() > 1 ? params.removeWeight : 0.0);
		double effectiveFlipLocationWeight = params.flipLocationWeight;
		double effectiveFlipDepTimeWeight = (from.size() < scenario.getTimeBinCnt() ? params.flipDepTimeWeight
				: 0.0);
		final double effectiveWeightSum = effectiveInsertWeight + effectiveRemoveWeight + effectiveFlipLocationWeight
				+ effectiveFlipDepTimeWeight;

		this.insertProba = effectiveInsertWeight / effectiveWeightSum;
		this.removeProba = effectiveRemoveWeight / effectiveWeightSum;
		this.flipLocationProba = effectiveFlipLocationWeight / effectiveWeightSum;
		this.flipDepTimeProba = effectiveFlipDepTimeWeight / effectiveWeightSum;

		assert (Math.abs(
				1.0 - this.insertProba - this.removeProba - this.flipLocationProba - this.flipDepTimeProba) < 1e-8);

		this.transitionProbaGivenFlipLocation = 1.0 / from.size() / (scenario.getLocationCnt() - 1);
		this.transitionProbaGivenFlipDepTime = 1.0 / from.size() / (scenario.getTimeBinCnt() - from.size());
	}

	RoundTripTransitionKernel(RoundTrip<L> from, Scenario<L> scenario) {
		this(from, scenario, new RoundTripProposalParameters());
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
			assert (tmp.equals(shorter));
		}
		return result;
	}

	private double transitionProbaGivenInsert(RoundTrip<?> to) {
		return this.numberOfInsertionPoints(this.from.getLocationsView(), to.getLocationsView())
				/ (this.from.size() + 1.0) / this.scenario.getLocationCnt()
				/ (this.scenario.getTimeBinCnt() - this.from.size());
	}

	private double numberOfRemovalPoints(List<?> longer, List<?> shorter) {
		assert (shorter.size() + 1 == longer.size());
		int result = 0;
		LinkedList<Object> tmp = new LinkedList<>(longer);
		for (int i = 0; i < longer.size(); i++) {
			Object removed = tmp.remove(i);
			assert (tmp.size() == shorter.size());
			if (tmp.equals(shorter)) {
				result++;
			}
			tmp.add(i, removed);
			assert (tmp.equals(longer));
		}
		assert (result > 0);
		return result;
	}

	private double transitionProbaGivenRemove(RoundTrip<?> to) {
		double result = this.numberOfRemovalPoints(this.from.getLocationsView(), to.getLocationsView())
				/ this.from.size() / this.from.size();
		assert (result > 0);
		return result;
	}

	// for testing
	Action identifyAction(RoundTrip<?> to) {
		if (this.from.size() + 1 == to.size()) {
			return Action.INS;
		} else if (this.from.size() - 1 == to.size()) {
			return Action.REM;
		} else if (!this.from.getLocationsView().equals(to.getLocationsView())) {
			return Action.FLIP_LOC;
		} else if (!this.from.getDeparturesView().equals(to.getDeparturesView())) {
			return Action.FLIP_DEP;
		} else {
			return null;
		}
	}

	// TODO NEW
	private boolean locationInsertWasPossible(List<?> shorter, List<?> longer) {
		assert (shorter.size() + 1 == longer.size());
		boolean usedInsert = false;
		for (int indexInShorter = 0; indexInShorter < shorter.size(); indexInShorter++) {
			int indexInLonger = (usedInsert ? indexInShorter + 1 : indexInShorter);
			if (!shorter.get(indexInShorter).equals(longer.get(indexInLonger))) {
				if (usedInsert) {
					return false;
				} else {
					usedInsert = true;
					if (!shorter.get(indexInShorter).equals(longer.get(indexInShorter + 1))) {
						return false;
					}
				}
			}
		}
		return true;
	}

	// TODO NEW
	private boolean locationFlipWasPossible(List<?> a, List<?> b) {
		assert (a.size() == b.size());
		int differenceCnt = 0;
		for (int i = 0; i < a.size(); i++) {
			if (!a.get(i).equals(b.get(i))) {
				differenceCnt++;
				if (differenceCnt > 1) {
					return false;
				}
			}
		}
		return (differenceCnt == 1);
	}

	// TODO NEW
	private boolean departuresInsertWasPossible(List<?> shorter, List<?> longer) {
		assert (shorter.size() + 1 == longer.size());
		return longer.containsAll(shorter);
	}

	// TODO NEW
	private boolean departureFlipWasPossible(List<?> a, List<?> b) {
		assert (a.size() == b.size());

		List<?> diff = new ArrayList<>(a);
		diff.removeAll(b);
		if (diff.size() != 1) {
			return false;
		}

		diff = new ArrayList<>(b);
		diff.removeAll(a);
		return (diff.size() == 1);
	}

	double transitionProbaChecked(RoundTrip<?> to) {

		if (this.from.size() + 1 == to.size()) {

			if (this.locationInsertWasPossible(this.from.getLocationsView(), to.getLocationsView())
					&& this.departuresInsertWasPossible(this.from.getDeparturesView(), to.getDeparturesView())) {
				return this.insertProba * this.transitionProbaGivenInsert(to);
			}

		} else if (this.from.size() - 1 == to.size()) {

			if (this.locationInsertWasPossible(to.getLocationsView(), this.from.getLocationsView())
					&& this.departuresInsertWasPossible(to.getDeparturesView(), this.from.getDeparturesView())) {
				return this.removeProba * this.transitionProbaGivenRemove(to);
			}

		} else if (this.from.size() == to.size()) {

			if (this.from.getDeparturesView().equals(to.getDeparturesView())) {

				if (this.locationFlipWasPossible(this.from.getLocationsView(), to.getLocationsView())) {
					return this.flipLocationProba * this.transitionProbaGivenFlipLocation;
				}

			} else if (this.from.getLocationsView().equals(to.getLocationsView())) {

				if (this.departureFlipWasPossible(this.from.getDeparturesView(), to.getDeparturesView())) {
					return this.flipDepTimeProba * this.transitionProbaGivenFlipDepTime;
				}

			}
		}

		return 0.0;
	}

	// This assumes that the transition from -> to followed the same kernel.
	double transitionProbaUnchecked(RoundTrip<?> to) {
		double result;
		if (this.from.size() + 1 == to.size()) {
			assert (this.insertProba * this.transitionProbaGivenInsert(to) > 0);
			result = this.insertProba * this.transitionProbaGivenInsert(to);
		} else if (this.from.size() - 1 == to.size()) {
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
		assert (result > 0);
		return result;
	}

	public double transitionProba(RoundTrip<L> to) {
		return this.transitionProbaUnchecked(to);
//		return this.transitionProbaChecked(to);
	}
}
