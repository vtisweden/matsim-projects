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

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.preferences.MaximumEntropyPriorFactory;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public class TestRoundTrips {

	static void testLocationsAndTimesWithoutLocationConstraints() {
		final long totalIts = 5 * 1000 * 1000;

//		System.out.println("" + (Double.NaN < 0));
//		System.out.println("" + (Double.NaN > 0));
//		System.out.println("" + (Double.NaN == 0));
//		System.exit(0);

//		List<String> aa = new ArrayList<>(Arrays.asList("A", "A"));
//		List<String> a = Arrays.asList("A");
//		aa.removeAll(a);
//		System.out.println(aa);
//		System.exit(0);
		
		final Random rnd = new Random();

		final double locationProba = 0.1;
		final double departureProba = 0.9;

		final Scenario<Location> scenario = new Scenario<>();
		for (int i = 1; i <= 3; i++) {
			scenario.getOrCreateLocationWithSameName(new Location("" + i));
		}
		scenario.setMaxStayEpisodes(10);
		scenario.setTimeBinCnt(24);

//		RoundTripProposal<Location> proposal = new RoundTripProposal<>(roundTrip -> null, scenario.getRandom());
//		proposal.addProposal(new RoundTripLocationProposal<>(scenario), locationProba);
//		proposal.addProposal(new RoundTripDepartureProposal<>(scenario), departureProba);
		SimplifiedRoundTripProposal<Location> proposal = new SimplifiedRoundTripProposal<>(scenario, roundTrip -> null);

		MHStateProcessor<RoundTrip<Location>> prn = new MHStateProcessor<>() {

			long it = 0;
			Map<RoundTrip<Location>, Long> roundTrip2cnt = new LinkedHashMap<>();
			private long[] binCnts = new long[scenario.getBinCnt()];
			Map<Integer, Long> size2cnt = new LinkedHashMap<>();

			@Override
			public void start() {
			}

			@Override
			public void processState(RoundTrip<Location> state) {
				if (it++ > totalIts / 2) {
					this.roundTrip2cnt.compute(state, (s, c) -> c == null ? 1 : c + 1);
					for (int i = 0; i < state.locationCnt(); i++) {
						this.binCnts[state.getDeparture(i)]++;
					}
					this.size2cnt.compute(state.locationCnt(), (s, c) -> c == null ? 1 : c + 1);
				}
			}

			@Override
			public void end() {
				List<Long> counts = new ArrayList<>(this.roundTrip2cnt.size());

				for (Map.Entry<RoundTrip<Location>, Long> e : this.roundTrip2cnt.entrySet()) {
//					System.out.println(e.getKey() + "\t" + e.getValue());
					counts.add(e.getValue());
				}
				System.out.println(this.roundTrip2cnt.size());

				System.out.println("departures");
				for (long cnt : this.binCnts) {
					System.out.println(cnt);
				}
				System.out.println();

				System.out.println("sizes");
				for (int size = 1; size <= Math.min(scenario.getBinCnt(), scenario.getMaxStayEpisodes()); size++) {
					System.out.println(size + "\t" + this.size2cnt.getOrDefault(size, 0l));
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

		Preferences<RoundTrip<Location>> pref = new Preferences<>();
//		pref.addComponent(new MaximumEntropyPrior<>(scenario.getLocationCnt(), scenario.getBinCnt(), 3.0));
		pref.addComponent(new MaximumEntropyPriorFactory<>(scenario.getLocationCnt(), scenario.getBinCnt(), scenario.getMaxStayEpisodes()).createSingle(3.0));
		
		RoundTrip<Location> initialState = new RoundTrip<>(Arrays.asList(scenario.getLocation("1")), Arrays.asList(12));

		MHAlgorithm<RoundTrip<Location>> algo = new MHAlgorithm<>(proposal, 
				pref,
				rnd);

		algo.addStateProcessor(prn);
		algo.setMsgInterval(10000);
		algo.setInitialState(initialState);
		algo.run(totalIts);
	}

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		testLocationsAndTimesWithoutLocationConstraints();

		System.out.println("... DONE");
	}

}
