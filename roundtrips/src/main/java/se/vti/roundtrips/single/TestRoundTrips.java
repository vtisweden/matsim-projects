/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public class TestRoundTrips {

	static void testLocationsAndTimesAndCharging() {
		final int totalIts = 10 * 1000 * 1000;

		final Random rnd = new Random();

		final double locationProba = 0.1;
		final double departureProba = 0.7;
		final double chargingProba = 0.19;
		final double doNothingProba = 0.01;
		final RoundTripConfiguration<Integer> scenario = new RoundTripConfiguration<>(4, 8, locationProba,
				departureProba, chargingProba, doNothingProba, new Random());
		for (int i = 1; i <= 3; i++) {
			scenario.addLocation(i);
		}

		RoundTripProposal<Integer, RoundTrip<Integer>> proposal = new RoundTripProposal<>(scenario, roundTrip -> null);
		proposal.addProposal(
				new RoundTripLocationProposal<>(scenario,
						(state, config, allLocations) -> new PossibleTransitions<>(state, config, allLocations)),
				scenario.getLocationProposalProbability());
		proposal.addProposal(new RoundTripDepartureProposal<>(scenario), scenario.getDepartureProposalProbability());

		MHStateProcessor<RoundTrip<Integer>> prn = new MHStateProcessor<>() {

			int it = 0;
			Map<RoundTrip<Integer>, Long> roundTrip2cnt = new LinkedHashMap<>();
			private long[] binCnts = new long[scenario.getTimeBinCnt()];

			@Override
			public void start() {
			}

			@Override
			public void processState(RoundTrip<Integer> state) {
				if (it++ > totalIts / 2) {
					this.roundTrip2cnt.compute(state, (s, c) -> c == null ? 1 : c + 1);
					for (int i = 0; i < state.locationCnt(); i++) {
						this.binCnts[state.getDeparture(i)]++;
					}
				}
			}

			@Override
			public void end() {
				List<Long> counts = new ArrayList<>(this.roundTrip2cnt.size());

				for (Map.Entry<RoundTrip<Integer>, Long> e : this.roundTrip2cnt.entrySet()) {
					System.out.println(e.getKey() + "\t" + e.getValue());
					counts.add(e.getValue());
				}
				System.out.println(this.roundTrip2cnt.size());

				System.out.println("departures");
				for (long cnt : this.binCnts) {
					System.out.println(cnt);
				}

				Collections.sort(counts);
				try {
					PrintWriter w = new PrintWriter("counts.txt");
					for (long c : counts) {
						w.println(c);
					}
					w.flush();
					w.close();
				} catch (Exception e) {
					throw new RuntimeException();
				}

			}

		};

		RoundTrip<Integer> initialState = new RoundTrip<>(Arrays.asList(1, 2, 3, 2), Arrays.asList(1, 3, 5, 7));

		MHAlgorithm<RoundTrip<Integer>> algo = new MHAlgorithm<>(proposal, state -> 0.0, rnd);
		algo.addStateProcessor(prn);
		algo.setMsgInterval(10000);
		algo.setInitialState(initialState);
		algo.run(totalIts);
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		testLocationsAndTimesAndCharging();

		System.out.println("... DONE");
	}

}
