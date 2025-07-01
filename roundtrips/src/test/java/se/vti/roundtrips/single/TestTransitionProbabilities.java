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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import se.vti.roundtrips.model.Scenario;
import se.vti.utils.misc.metropolishastings.MHTransition;

/**
 * 
 * @author GunnarF
 *
 */
public class TestTransitionProbabilities {

	static int samples = 100;
	static long itsPerSample = 100 * 1000;
	static int locCnt = 2;
	static int binCnt = 10;
	static int maxStayEpisodes = binCnt;

	static boolean growShrink = true;
	static boolean flip = true;

	static RoundTripTransitionKernel.Action action = RoundTripTransitionKernel.Action.INS;

	public static void main(String[] args) {
		Scenario<Node> scenario = new Scenario<>();
		for (int i = 1; i <= locCnt; i++) {
			scenario.getOrCreateLocationWithSameName(new Node("" + i));
		}
		scenario.setMaxStayEpisodes(maxStayEpisodes);
		scenario.setTimeBinCnt(binCnt);

		RoundTripProposalParameters params = new RoundTripProposalParameters(growShrink ? 1 : 0,
				growShrink ? 1 : 0, flip ? 1 : 0, flip ? 1 : 0);
		RoundTripProposal<Node> proposal = new RoundTripProposal<>(params, scenario,
				roundTrip -> null);

		int failures = 0;
		for (int sample = 0; sample < samples; sample++) {

			RoundTrip<Node> from;
			MHTransition<RoundTrip<Node>> target;
			RoundTripTransitionKernel<Node> fwdKernel;
			do {
				int size = scenario.getRandom().nextInt(1, Math.min(maxStayEpisodes, binCnt));
				List<Node> locations = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					locations.add(
							scenario.getLocationsView().get(scenario.getRandom().nextInt(scenario.getLocationCnt())));
				}
				List<Integer> departures = new ArrayList<>(IntStream.range(0, binCnt).boxed().toList());
				Collections.shuffle(departures);
				departures = departures.subList(0, size);
				Collections.sort(departures);
				from = new RoundTrip<>(locations, departures);
				fwdKernel = new RoundTripTransitionKernel<>(from, scenario, params);
				target = proposal.newTransition(from);
			} while (!fwdKernel.identifyAction(target.getNewState()).equals(action));


			if (Math.abs(Math.exp(target.getFwdLogProb()) - fwdKernel.transitionProba(target.getNewState())) > 1e-8) {
				System.out.println("    ERROR: target proba = " + Math.exp(target.getFwdLogProb())
						+ " but kernel proba = " + fwdKernel.transitionProba(target.getNewState()));
			}

			System.out.println(target.getOldState() + "  -->  " + target.getNewState());

			int recoveredCnt = 0;
			for (int it = 0; it < itsPerSample; it++) {
				RoundTrip<Node> trial = proposal.newTransition(from).getNewState();
				if (trial.getLocationsView().equals(target.getNewState().getLocationsView())
						&& trial.getDeparturesView().equals(target.getNewState().getDeparturesView())) {
					recoveredCnt++;
				}
			}

			double _S = recoveredCnt;
			double _T = itsPerSample;
			double sigmaS = Math.sqrt(_S * (1.0 - _S / _T));
			double dev = _S - Math.exp(target.getFwdLogProb()) * _T;
			if (Math.abs(dev) < 2.0 * sigmaS) {
				System.out.print("Success:");
			} else {
				failures++;
				System.out.print("!!! FAILURE:");
			}
			System.out.println("\t realized=" + (_S / _T) + "\ttheoretical=" + Math.exp(target.getFwdLogProb()) + "\tdeviation="
					+ (_S / _T - Math.exp(target.getFwdLogProb())));
			System.out.println();
			
		}
		System.out.println("Failure probability = " + ((double) failures) / samples);
		System.out.println();
	}

}
