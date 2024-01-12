/**
 * se.vti.skellefeaV2X
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
package se.vti.skellefteaV2X.instances.v0;

import java.util.Arrays;
import java.util.Random;

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.RoundTripSimulator;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.preferences.AllDayTimeConstraintPreference;
import se.vti.skellefteaV2X.preferences.AtHomeOffCampusPreference;
import se.vti.skellefteaV2X.preferences.NonnegativeBatteryStatePreference;
import se.vti.skellefteaV2X.preferences.OnCampusPreference;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.skellefteaV2X.roundtrips.RoundTripConfiguration;
import se.vti.skellefteaV2X.roundtrips.RoundTripProposal;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;

/**
 * 
 * @author GunnarF
 *
 */
public class Runner {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		// BUILD SCENARIO

		final double chargingRate_kW = 11.0;
		final double maxCharge_kWh = 60.0;
		final double consumptionRate_kWh_km = 0.2;

		final double defaultSpeed_km_h = 60.0;
		final int timeBinCnt = 24;

		Scenario scenario = new Scenario(chargingRate_kW, maxCharge_kWh, consumptionRate_kWh_km, defaultSpeed_km_h, timeBinCnt);

		Location boliden = scenario.createAndAddLocation("Boliden", true);
		Location kage = scenario.createAndAddLocation("Kåge", true);
		Location centrum = scenario.createAndAddLocation("Centrum", true);
		Location campus = scenario.createAndAddLocation("Campus", true);
		Location hamn = scenario.createAndAddLocation("Hamn", true);
		Location burea = scenario.createAndAddLocation("Bureå", true);
		Location burtrask = scenario.createAndAddLocation("Burträsk", true);

		scenario.setSymmetricDistance_km(boliden, kage, 38);
		scenario.setSymmetricDistance_km(boliden, centrum, 34);
		scenario.setSymmetricDistance_km(boliden, campus, 34);
		scenario.setSymmetricDistance_km(boliden, hamn, 47);
		scenario.setSymmetricDistance_km(boliden, burea, 55);
		scenario.setSymmetricDistance_km(boliden, burtrask, 48);

		scenario.setSymmetricDistance_km(kage, centrum, 13);
		scenario.setSymmetricDistance_km(kage, campus, 13);
		scenario.setSymmetricDistance_km(kage, hamn, 24);
		scenario.setSymmetricDistance_km(kage, burea, 33);
		scenario.setSymmetricDistance_km(kage, burtrask, 53);

		scenario.setSymmetricDistance_km(centrum, campus, 5);
		scenario.setSymmetricDistance_km(centrum, hamn, 13);
		scenario.setSymmetricDistance_km(centrum, burea, 22);
		scenario.setSymmetricDistance_km(centrum, burtrask, 42);

		scenario.setSymmetricDistance_km(campus, hamn, 13);
		scenario.setSymmetricDistance_km(campus, burea, 22);
		scenario.setSymmetricDistance_km(campus, burtrask, 42);

		scenario.setSymmetricDistance_km(hamn, burea, 11);
		scenario.setSymmetricDistance_km(hamn, burtrask, 46);

		scenario.setSymmetricDistance_km(burea, burtrask, 35);

		// BUILD ROUNDTRIPS SAMPLER

		int maxLocations = 4;
		double locationProposalWeight = 0.1;
		double departureProposalWeight = 0.45;
		double chargingProposalWeight = 0.45;

		final RoundTripConfiguration<Location> roundTrips = new RoundTripConfiguration<>(maxLocations, timeBinCnt,
				locationProposalWeight, departureProposalWeight, chargingProposalWeight);
		roundTrips.addLocations(scenario.getLocationsView());


		// BUILD MH MACHINERY

		RoundTripSimulator simulator = new RoundTripSimulator(scenario);
		
//		TargetWeights targetWeights = new TargetWeights(simulator, campus, 12.0, 1.0, -2.0, +6.0);

		
		Preferences preferences = new Preferences(simulator);
		preferences.addComponent(new AllDayTimeConstraintPreference());
		preferences.addComponent(new NonnegativeBatteryStatePreference());
		preferences.addComponent(new AtHomeOffCampusPreference(campus, -2.0, +6.0));
		preferences.addComponent(new OnCampusPreference(campus, 12.0));
		
		int iterations = 10 * 1000 * 1000;


		RoundTripProposal<Location> proposal = new RoundTripProposal<>(roundTrips);
		RoundTrip<Location> initialState = new RoundTrip<>(Arrays.asList(centrum), Arrays.asList(8),
				Arrays.asList(true));

		MHAlgorithm<RoundTrip<Location>> algo = new MHAlgorithm<>(proposal, preferences, new Random());
		algo.setMsgInterval(iterations / 100);
		algo.addStateProcessor(new StationarityStats(simulator, campus, timeBinCnt, iterations / 100));
		algo.setInitialState(initialState);
		algo.run(iterations);

		System.out.println("... DONE");
	}

}
