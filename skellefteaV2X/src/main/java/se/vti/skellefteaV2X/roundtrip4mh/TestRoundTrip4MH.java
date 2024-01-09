/**
 * se.vti.skellefteaV2X
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
package se.vti.skellefteaV2X.roundtrip4mh;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class TestRoundTrip4MH {

	static int sum(RoundTrip<Integer> state) {
		int sum = 0;
		for (int i = 0; i < state.size(); i++) {
			sum += state.getLocation(i);
		}
		return sum;
	}

	public static void main(String[] args) {

		final int totalIts = 10 * 1000 * 1000;

		System.out.println("STARTED ...");

		final Random rnd = new Random();

		final RoundTripScenario<Integer> scenario = new RoundTripScenario<>();
		for (int i = 1; i <= 3; i++) {
			scenario.getAllLocations().add(i);
		}

		RoundTripProposal<Integer> proposal = new RoundTripProposal<>(scenario);

		MHWeight<RoundTrip<Integer>> weight = new MHWeight<>() {

			double occurrenceOfSum[] = new double[] { 0, 1, 1, 3, 2, 2, 8, 4, 6, 4, 2 };

			@Override
			public double logWeight(RoundTrip<Integer> state) {
				final int sum = sum(state);
				return -0.0 * sum - Math.log(this.occurrenceOfSum[sum]);
			}

		};

		MHStateProcessor<RoundTrip<Integer>> prn = new MHStateProcessor<>() {

			int it = 0;
			long[] sumHist = new long[11];

			Map<List<Integer>, Long> seq2cnt = new LinkedHashMap<>();

			@Override
			public void start() {
			}

			@Override
			public void processState(RoundTrip<Integer> state) {
				if (it++ > totalIts / 4) {
					this.sumHist[sum(state)]++;
					List<Integer> locations = state.locationsCopy();
					this.seq2cnt.compute(locations, (locs, cnt) -> cnt == null ? 1 : cnt + 1);
				}
			}

			@Override
			public void end() {
				final double sum = Arrays.stream(this.sumHist).sum();
				System.out.println("sum\tfreq");
				for (int i = 1; i < sumHist.length; i++) {
					System.out.println(i + "\t" + this.sumHist[i] / sum);
				}
				System.out.println();
				System.out.println("seq\tfreq");
				for (Map.Entry<List<Integer>, Long> e : this.seq2cnt.entrySet()) {
					System.out.println(e.getKey() + "\t" + 33.0 * e.getValue() / sum);
				}
			}

		};

		MHAlgorithm<RoundTrip<Integer>> algo = new MHAlgorithm<>(proposal, weight, rnd);
		algo.addStateProcessor(prn);
		algo.setMsgInterval(10000);
		algo.run(totalIts);

//		System.out.println();
//		System.out.println("from\tto\trealized\tpredicted\t(realized/predicted)");
//		for (RoundTrip<Integer> from : proposal.state2visitCnt.keySet()) {
//			for (RoundTrip<Integer> to : proposal.state2visitCnt.keySet()) {
//				Tuple<RoundTrip<Integer>, RoundTrip<Integer>> tuple = new Tuple<>(from, to);
//				double cnt = proposal.state2visitCnt.get(from);
//				double realized = proposal.transition2proposedCnt.getOrDefault(tuple, 0.) / cnt;
//				double predicted = proposal.transition2proposedProbaSum.getOrDefault(tuple, 0.) / cnt;
//				if (realized > 0 || predicted > 0) {
//					System.out.println(from + "\t" + to + "\t" + realized + "\t" + predicted + "\t" + (realized / predicted));
//				}
//			}
//		}

		System.out.println("... DONE");
	}

}
