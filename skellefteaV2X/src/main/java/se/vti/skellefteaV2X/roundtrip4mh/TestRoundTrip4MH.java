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

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		final Random rnd = new Random();

		final RoundTripScenario<Integer> scenario = new RoundTripScenario<>();
		for (int i = 1; i <= 100; i++) {
			scenario.getAllLocations().add(i);
		}

		RoundTripProposal<Integer> proposal = new RoundTripProposal<>(scenario);

		MHWeight<RoundTrip<Integer>> weight = new MHWeight<>() {

			@Override
			public double logWeight(RoundTrip<Integer> State) {
				return 0.0;
			}

		};

		MHStateProcessor<RoundTrip<Integer>> prn = new MHStateProcessor<>() {

			@Override
			public void start() {
			}

			@Override
			public void processState(RoundTrip<Integer> state) {
				System.out.println(state);
			}

			@Override
			public void end() {
			}

		};

		MHAlgorithm<RoundTrip<Integer>> algo = new MHAlgorithm<>(proposal, weight, rnd);
//		algo.addStateProcessor(prn);
		algo.run(100000);

		System.out.println("... DONE");
	}

}
