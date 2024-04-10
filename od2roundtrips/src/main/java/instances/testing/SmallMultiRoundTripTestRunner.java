/**
 * instances.testing
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package instances.testing;

import java.util.Arrays;
import java.util.Random;

import se.vti.od2roundtrips.model.MultiRoundTripWithOD;
import se.vti.od2roundtrips.model.OD2RoundtripsScenario;
import se.vti.od2roundtrips.model.SingleToMultiComponent;
import se.vti.od2roundtrips.model.TAZ;
import se.vti.od2roundtrips.targets.ODTarget;
import se.vti.roundtrips.model.DefaultDrivingSimulator;
import se.vti.roundtrips.model.DefaultParkingSimulator;
import se.vti.roundtrips.model.Simulator;
import se.vti.roundtrips.model.VehicleState;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.preferences.AllDayTimeConstraintPreference;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.preferences.StrategyRealizationConsistency;
import se.vti.roundtrips.preferences.UniformOverLocationCount;
import se.vti.roundtrips.single.PossibleTransitions;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripDepartureProposal;
import se.vti.roundtrips.single.RoundTripLocationProposal;
import se.vti.roundtrips.single.RoundTripProposal;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;

/**
 * 
 * @author GunnarF
 *
 */
public class SmallMultiRoundTripTestRunner {

	public static void main(String[] args) {
		System.out.println("STARTED ...");

		OD2RoundtripsScenario scenario = new OD2RoundtripsScenario();

		TAZ a = scenario.createAndAddLocation("A");
		TAZ b = scenario.createAndAddLocation("B");
		TAZ c = scenario.createAndAddLocation("C");

		scenario.setSymmetricDistance_km(a, b, 10.0);
		scenario.setSymmetricDistance_km(a, c, 10.0);
		scenario.setSymmetricDistance_km(b, c, 10.0);

		scenario.setSymmetricTime_h(a, b, 0.1);
		scenario.setSymmetricTime_h(a, c, 0.1);
		scenario.setSymmetricTime_h(b, c, 0.1);

		scenario.setMaxParkingEpisodes(4);
		scenario.setTimeBinCnt(24);

		final Preferences<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> allPreferences = new Preferences<>();

		// Consistency preferences

		allPreferences.addComponent(new SingleToMultiComponent(new UniformOverLocationCount<>(scenario)));
		allPreferences.addComponent(new SingleToMultiComponent(new AllDayTimeConstraintPreference<>()));
		allPreferences.addComponent(new SingleToMultiComponent(new StrategyRealizationConsistency<>(scenario)));

		// Modeling preferences

		ODTarget odPreference = new ODTarget();
		odPreference.setODEntry(a, b, 1.0);
		odPreference.setODEntry(b, a, 2.0);
		odPreference.setODEntry(a, c, 3.0);
		odPreference.setODEntry(c, a, 4.0);
		odPreference.setODEntry(b, c, 5.0);
		odPreference.setODEntry(c, b, 6.0);
		allPreferences.addComponent(odPreference, 100);

		// Default physical simulator

		Simulator<TAZ, VehicleState, RoundTrip<TAZ>> simulator = new Simulator<>(scenario, () -> new VehicleState());
		simulator.setDrivingSimulator(new DefaultDrivingSimulator<>(scenario, () -> new VehicleState()));
		simulator.setParkingSimulator(new DefaultParkingSimulator<>(scenario, () -> new VehicleState()));

		// Create MH algorithm

		int roundTripCnt = 10;
		double locationProposalWeight = 0.5;
		double departureProposalWeight = 0.5;

		RoundTripProposal<RoundTrip<TAZ>> proposal = new RoundTripProposal<>(simulator, scenario.getRandom());
		proposal.addProposal(new RoundTripLocationProposal<RoundTrip<TAZ>, TAZ>(scenario,
				(state, scen) -> new PossibleTransitions<TAZ>(state, scen)), locationProposalWeight);
		proposal.addProposal(new RoundTripDepartureProposal<>(scenario), departureProposalWeight);
		MultiRoundTripProposal<RoundTrip<TAZ>, MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> proposalMulti = new MultiRoundTripProposal<>(
				scenario.getRandom(), proposal);

		MHAlgorithm<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> algo = new MHAlgorithm<>(proposalMulti, allPreferences,
				new Random());

		final MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> initialStateMulti = new MultiRoundTripWithOD<>(roundTripCnt);
		for (int i = 0; i < initialStateMulti.size(); i++) {
			RoundTrip<TAZ> initialStateSingle = new RoundTrip<TAZ>(Arrays.asList(a, b), Arrays.asList(6, 18));
			initialStateSingle.setEpisodes(simulator.simulate(initialStateSingle));
			initialStateMulti.setRoundTrip(i, initialStateSingle);
		}

		algo.setInitialState(initialStateMulti);

		algo.setMsgInterval(10 * 1000);

		// Run MH algorithm

		algo.run(1000 * 1000);
	}
}
