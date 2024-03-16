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

import od2roundtrips.model.OD2RoundtripsScenario;
import od2roundtrips.model.ODPreference;
import od2roundtrips.model.ODReproductionAnalyzerMultiple;
import od2roundtrips.model.TAZ;
import se.vti.roundtrips.model.DefaultDrivingSimulator;
import se.vti.roundtrips.model.DefaultParkingSimulator;
import se.vti.roundtrips.model.Simulator;
import se.vti.roundtrips.model.VehicleState;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.multiple.MultiRoundTripPreferences;
import se.vti.roundtrips.multiple.MultiRoundTripProposal;
import se.vti.roundtrips.preferences.AllDayTimeConstraintPreference;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.preferences.StrategyRealizationConsistency;
import se.vti.roundtrips.preferences.UniformOverLocationCount;
import se.vti.roundtrips.single.PossibleTransitions;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.single.RoundTripConfiguration;
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

		// Consistency preferences

		final Preferences<RoundTrip<TAZ>, TAZ> consistencyPreferences = new Preferences<>();
		consistencyPreferences.addComponent(new UniformOverLocationCount<>(scenario));
		consistencyPreferences.addComponent(new AllDayTimeConstraintPreference<>());
		consistencyPreferences.addComponent(new StrategyRealizationConsistency<>(scenario));

		// Modeling preferences

		int roundTripCnt = 10;
		final Preferences<RoundTrip<TAZ>, TAZ> modelingPreferences = new Preferences<>();
		ODPreference odPreference = new ODPreference();
		odPreference.setODEntry(a, b, 1.0 * roundTripCnt);
		odPreference.setODEntry(b, a, 2.0 * roundTripCnt);
		odPreference.setODEntry(a, c, 3.0 * roundTripCnt);
		odPreference.setODEntry(c, a, 4.0 * roundTripCnt);
		odPreference.setODEntry(b, c, 5.0 * roundTripCnt);
		odPreference.setODEntry(c, b, 6.0 * roundTripCnt);
		modelingPreferences.addComponent(odPreference);

		// Default physical simulator

		Simulator<TAZ, VehicleState, RoundTrip<TAZ>> simulator = new Simulator<>(scenario, () -> new VehicleState());
		simulator.setDrivingSimulator(new DefaultDrivingSimulator<>(scenario, () -> new VehicleState()));
		simulator.setParkingSimulator(new DefaultParkingSimulator<>(scenario, () -> new VehicleState()));

		// Create MH algorithm

		double locationProposalWeight = 0.5;
		double departureProposalWeight = 0.5;
		final RoundTripConfiguration<TAZ> configuration = new RoundTripConfiguration<>(scenario.getMaxParkingEpisodes(),
				scenario.getBinCnt(), locationProposalWeight, departureProposalWeight, 0.0, 0.0);
		// TODO configuration is still electrification-specific
		configuration.addLocations(scenario.getLocationsView());

		RoundTripProposal<TAZ, RoundTrip<TAZ>> proposal = new RoundTripProposal<>(configuration, simulator);
		proposal.addProposal(
				new RoundTripLocationProposal<RoundTrip<TAZ>, TAZ>(configuration,
						(state, config, allLocs) -> new PossibleTransitions<>(state, config, allLocs)),
				locationProposalWeight);
		proposal.addProposal(new RoundTripDepartureProposal<>(configuration), departureProposalWeight);
		MultiRoundTripProposal<TAZ, RoundTrip<TAZ>> proposalMulti = new MultiRoundTripProposal<>(new Random(),
				proposal);

		final MultiRoundTripPreferences<RoundTrip<TAZ>, TAZ> preferencesMulti = new MultiRoundTripPreferences<>();
		preferencesMulti.addPreferences(consistencyPreferences);
		preferencesMulti.addPreferences(modelingPreferences);

		MHAlgorithm<MultiRoundTrip<TAZ, RoundTrip<TAZ>>> algo = new MHAlgorithm<>(proposalMulti, preferencesMulti,
				new Random());
		
		ODReproductionAnalyzerMultiple odAnalyzer = new ODReproductionAnalyzerMultiple(100 * 1000, 100, odPreference.getTargetOdMatrix());
		algo.addStateProcessor(odAnalyzer);

		final MultiRoundTrip<TAZ, RoundTrip<TAZ>> initialStateMulti = new MultiRoundTrip<>(roundTripCnt);
		for (int i = 0; i < initialStateMulti.size(); i++) {
		RoundTrip<TAZ> initialStateSingle = new RoundTrip<TAZ>(Arrays.asList(a, b), Arrays.asList(6, 18));
		initialStateSingle.setEpisodes(simulator.simulate(initialStateSingle));
		initialStateMulti.setRoundTrip(i, initialStateSingle);
		}
		
		algo.setInitialState(initialStateMulti);

		algo.setMsgInterval(10 * 1000);

		// Run MH algorithm

		algo.run(1000 * 1000);
		
		System.out.println();
		System.out.println(odAnalyzer);
	}
}
