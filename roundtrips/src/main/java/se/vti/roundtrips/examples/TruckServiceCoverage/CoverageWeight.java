/**
 * se.vti.roundtrips.examples.TruckServiceCoverage
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
package se.vti.roundtrips.examples.TruckServiceCoverage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import se.vti.roundtrips.model.MoveEpisode;
import se.vti.roundtrips.model.StayEpisode;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.weights.Weight;

/**
 * 
 * @author GunnarF
 *
 */
class CoverageWeight extends Weight<MultiRoundTrip<GridNode>> {

	private final int gridSize;

	CoverageWeight(int gridSize) {
		this.gridSize = gridSize;
	}

	@Override
	public double logWeight(MultiRoundTrip<GridNode> multiRoundTrips) {

		/*
		 * Extract from the round trips all movements between grid nodes and sort these
		 * by increasing arrival time.
		 */

		List<MoveEpisode<GridNode>> allMoveEpisodes = new ArrayList<>();
		for (var roundTrip : multiRoundTrips) {
			var episodes = roundTrip.getEpisodes();
			// Every other episode is a move episode.
			for (int i = 1; i < episodes.size(); i += 2) {
				allMoveEpisodes.add((MoveEpisode<GridNode>) episodes.get(i));
			}
		}
		Collections.sort(allMoveEpisodes, new Comparator<MoveEpisode<GridNode>>() {
			@Override
			public int compare(MoveEpisode<GridNode> move1, MoveEpisode<GridNode> move2) {
				return Double.compare(move1.getEndTime_h(), move2.getEndTime_h());
			}
		});

		/*
		 * Compute earliest arrival time at all grid nodes.
		 */

		double[][] earliestArrivalTime_h = new double[this.gridSize][this.gridSize];
		for (int row = 0; row < this.gridSize; row++) {
			Arrays.fill(earliestArrivalTime_h[row], Double.POSITIVE_INFINITY);
		}

		var distributionRoundTrip = multiRoundTrips.getRoundTrip(0);
		GridNode depot = (GridNode) ((StayEpisode<?>) distributionRoundTrip.getEpisodes().get(0)).getLocation();
		earliestArrivalTime_h[depot.row][depot.column] = 0.0;

		for (var move : allMoveEpisodes) {
			GridNode from = move.getOrigin();
			GridNode to = move.getDestination();
			if (earliestArrivalTime_h[from.row][from.column] < move.getEndTime_h() - move.getDuration_h()) {
				earliestArrivalTime_h[to.row][to.column] = Math.min(earliestArrivalTime_h[to.row][to.column],
						move.getEndTime_h());
			}
		}

		/*
		 * Cound how many grid nodes were not reached within 24h. The sampling weight is
		 * exp(minus the number of missed nodes), so the log weight to be returned is
		 * minus the number of missed nodes.
		 */

		int missed = 0;
		for (int row = 0; row < this.gridSize; row++) {
			missed += Arrays.stream(earliestArrivalTime_h[row]).filter(t -> t >= 24.0).count();
		}

		return -missed;
	}

}
