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

import se.vti.roundtrips.model.DefaultSimulator;
import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.weights.MaximumEntropyPriorFactory;
import se.vti.roundtrips.weights.PeriodicScheduleWeight;
import se.vti.roundtrips.weights.SamplingWeights;
import se.vti.roundtrips.weights.SingleToMultiWeight;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;

/**
 * 
 * @author GunnarF
 *
 */
public class RunTruckServiceCoverage {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		var scenario = new Scenario<GridNode>();

		/*
		 * A one-hour time discretization with period of 24 time bins. Since all
		 * departure time bins must be distinct, the time configuration implies a
		 * maximum number of 24 stay episodes. We could further limit the number of stay
		 * episodes but do not do so; the minimum of time bin count and max stay
		 * episodes limits the effective maximum number of stays episodes in a round
		 * trip.
		 */
		scenario.setBinSize_h(1.0);
		scenario.setTimeBinCnt(24);
		scenario.setMaxStayEpisodes(Integer.MAX_VALUE);

		/*
		 * Populate the grid world with nodes.
		 */
		int gridSize = 5;
		GridNode[][] nodes = new GridNode[gridSize][gridSize];
		for (int row = 0; row < gridSize; row++) {
			for (int column = 0; column < gridSize; column++) {
				nodes[row][column] = new GridNode(row, column);
				scenario.addLocation(nodes[row][column]);
			}
		}

		/*
		 * Define distances and travel times between grid nodes.
		 */
		double edgeLength_km = 120;
		double edgeTime_h = 2.0;
		for (int row1 = 0; row1 < gridSize; row1++) {
			for (int column1 = 0; column1 < gridSize; column1++) {
				GridNode node1 = nodes[row1][column1];
				for (int row2 = 0; row2 < gridSize; row2++) {
					for (int column2 = 0; column2 < gridSize; column2++) {
						GridNode node2 = nodes[row2][column2];
						int gridDistance = node1.computeGridDistance(node2);
						scenario.setDistance_km(node1, node2, edgeLength_km * gridDistance);
						scenario.setTime_h(node1, node2, edgeTime_h * gridDistance);
					}
				}
			}
		}

		/*
		 * 
		 */

		int fleetSize = 5;

		SamplingWeights<MultiRoundTrip<GridNode>> weights = new SamplingWeights<>();
		weights.add(new MaximumEntropyPriorFactory<>(scenario).createMultiple(fleetSize), 1.0);
		weights.add(new SingleToMultiWeight<>(new PeriodicScheduleWeight<GridNode>(scenario.getPeriodLength_h())), 1.0);
		weights.add(new CoverageWeight(gridSize), 1.0);

		/*
		 * 
		 */

		var simulator = new DefaultSimulator<>(scenario);
		
		MultiRoundTrip<GridNode> initialRoundTrips = new MultiRoundTrip<>(fleetSize);
		for (int n = 0; n < fleetSize; n++) {
			var roundTrip = new RoundTrip<>(new ArrayList<>(Arrays.asList(nodes[0][0])),
					new ArrayList<>(Arrays.asList(0)));
			roundTrip.setEpisodes(simulator.simulate(roundTrip));
			initialRoundTrips.setRoundTripAndUpdateSummaries(n, roundTrip);
		}

		var algo = new MHAlgorithm<>(new MultiRoundTripProposal<>(scenario, simulator), weights, scenario.getRandom());
		algo.setInitialState(initialRoundTrips);
		algo.run(1000);

		System.out.println("... DONE");
	}

}
