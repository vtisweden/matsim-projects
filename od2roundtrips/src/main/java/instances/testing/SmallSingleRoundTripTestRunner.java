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
import od2roundtrips.model.ODReproductionAnalyzerSingle;
import od2roundtrips.model.TAZ;
import se.vti.roundtrips.model.DefaultDrivingSimulator;
import se.vti.roundtrips.model.DefaultParkingSimulator;
import se.vti.roundtrips.model.Simulator;
import se.vti.roundtrips.model.VehicleState;
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
public class SmallSingleRoundTripTestRunner {

	public static void main(String[] args) {

		// Construct the scenario

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

		final Preferences<RoundTrip<TAZ>, TAZ> modelingPreferences = new Preferences<>();

		ODPreference odPreference = new ODPreference();
		odPreference.setODEntry(a, b, 1.0);
		odPreference.setODEntry(b, a, 2.0);
		odPreference.setODEntry(a, c, 3.0);
		odPreference.setODEntry(c, a, 4.0);
		odPreference.setODEntry(b, c, 5.0);
		odPreference.setODEntry(c, b, 6.0);
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

		Preferences<RoundTrip<TAZ>, TAZ> allPreferences = new Preferences<>();
		allPreferences.addPreferences(consistencyPreferences);
		allPreferences.addPreferences(modelingPreferences);

		MHAlgorithm<RoundTrip<TAZ>> algo = new MHAlgorithm<>(proposal, allPreferences, new Random());

		ODReproductionAnalyzerSingle odAnalyzer = new ODReproductionAnalyzerSingle(100 * 1000, 100, odPreference.getTargetOdMatrix());
		algo.addStateProcessor(odAnalyzer);
		
		RoundTrip<TAZ> initialState = new RoundTrip<TAZ>(Arrays.asList(a, b), Arrays.asList(6, 18));
		initialState.setEpisodes(simulator.simulate(initialState));
		algo.setInitialState(initialState);
		
		algo.setMsgInterval(1000);
		
		// Run MH algorithm

		algo.run(2 * 1000 * 1000);
		
		System.out.println();
		System.out.println(odAnalyzer);
	}

}
