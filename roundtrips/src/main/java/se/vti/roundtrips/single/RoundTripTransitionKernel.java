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

import se.vti.roundtrips.model.Scenario;

/**
 * 
 * @author GunnarF
 *
 * @param <L>
 */
public class RoundTripTransitionKernel {

	// -------------------- CONSTANTS --------------------

	private final RoundTrip<?> from;

	public final double insertProba;
	public final double removeProba;
	public final double flipLocationProba;
	public final double flipDepTimeProba;

	public final double transitionProbaGivenInsert;
	public final double transitionProbaGivenRemove;
	public final double transitionProbaGivenFlipLocation;
	public final double transitionProbaGivenFlipDepTime;

	// -------------------- CONSTRUCTION --------------------

	public RoundTripTransitionKernel(RoundTrip<?> from, Scenario<?> scenario,
			SimplifiedRoundTripProposalParameters params) {
		this.from = from;

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

		this.transitionProbaGivenInsert = 1.0 / (from.locationCnt() + 1.0) / scenario.getLocationCnt()
				/ (scenario.getBinCnt() - from.locationCnt());
		this.transitionProbaGivenRemove = 1.0 / from.locationCnt();
		this.transitionProbaGivenFlipLocation = 1.0 / from.locationCnt() / (scenario.getLocationCnt() - 1);
		this.transitionProbaGivenFlipDepTime = 1.0 / from.locationCnt() / (scenario.getBinCnt() - from.locationCnt());
	}

	// -------------------- IMPLEMENTATION --------------------

	public double transitionProba(RoundTrip<?> to) {
		if (this.from.locationCnt() + 1 == to.locationCnt()) {
			return this.insertProba * this.transitionProbaGivenInsert;
		} else if (this.from.locationCnt() - 1 == to.locationCnt()) {
			return this.removeProba * this.transitionProbaGivenRemove;
		} else if (!this.from.getLocationsView().equals(to.getLocationsView())) {
			return this.flipLocationProba * this.transitionProbaGivenFlipLocation;
		} else if (!this.from.getDeparturesView().equals(to.getDeparturesView())) {
			return this.flipDepTimeProba * this.transitionProbaGivenFlipDepTime;
		} else {
			return 0.0;
		}
	}
}
