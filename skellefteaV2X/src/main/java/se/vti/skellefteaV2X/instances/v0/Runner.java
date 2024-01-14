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

import se.vti.skellefteaV2X.analysis.LocationVisitAnalyzer;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.Simulator;
import se.vti.skellefteaV2X.preferences.AllDayTimeConstraintPreference;
import se.vti.skellefteaV2X.preferences.AtHomePreference;
import se.vti.skellefteaV2X.preferences.LocalChargingAmountPrefence;
import se.vti.skellefteaV2X.preferences.NonnegativeBatteryStatePreference;
import se.vti.skellefteaV2X.preferences.NotHomePreference;
import se.vti.skellefteaV2X.preferences.StrategyRealizationConsistency;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.skellefteaV2X.simulators.V2GParkingSimulator;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;

/**
 * 
 * @author GunnarF
 *
 */
public class Runner {

	public static void main(String[] args) {

		/*
		 * Define study region.
		 * 
		 */

		final double scale = 1.0;

		// Scenario has setters for non-default scenario parameters.
		Scenario scenario = new Scenario();

		Location boliden = scenario.createAndAddLocation("Boliden", true);
		Location kage = scenario.createAndAddLocation("Kåge", true);
		Location centrum = scenario.createAndAddLocation("Centrum", true);
		Location campus = scenario.createAndAddLocation("Campus", true);
		Location hamn = scenario.createAndAddLocation("Hamn", true);
		Location burea = scenario.createAndAddLocation("Bureå", true);
		Location burtrask = scenario.createAndAddLocation("Burträsk", true);

		// Scenario has setters for direction-specific distances.
		// By default, travel times are inferred from distances.
		// Scenario also has setters for arbitrary travel times.
		scenario.setSymmetricDistance_km(boliden, kage, scale * 38);
		scenario.setSymmetricDistance_km(boliden, centrum, scale * 34);
		scenario.setSymmetricDistance_km(boliden, campus, scale * 34);
		scenario.setSymmetricDistance_km(boliden, hamn, scale * 47);
		scenario.setSymmetricDistance_km(boliden, burea, scale * 55);
		scenario.setSymmetricDistance_km(boliden, burtrask, scale * 48);

		scenario.setSymmetricDistance_km(kage, centrum, scale * 13);
		scenario.setSymmetricDistance_km(kage, campus, scale * 13);
		scenario.setSymmetricDistance_km(kage, hamn, scale * 24);
		scenario.setSymmetricDistance_km(kage, burea, scale * 33);
		scenario.setSymmetricDistance_km(kage, burtrask, scale * 53);

		scenario.setSymmetricDistance_km(centrum, campus, scale * 5);
		scenario.setSymmetricDistance_km(centrum, hamn, scale * 13);
		scenario.setSymmetricDistance_km(centrum, burea, scale * 22);
		scenario.setSymmetricDistance_km(centrum, burtrask, scale * 42);

		scenario.setSymmetricDistance_km(campus, hamn, scale * 13);
		scenario.setSymmetricDistance_km(campus, burea, scale * 22);
		scenario.setSymmetricDistance_km(campus, burtrask, scale * 42);

		scenario.setSymmetricDistance_km(hamn, burea, scale * 11);
		scenario.setSymmetricDistance_km(hamn, burtrask, scale * 46);

		scenario.setSymmetricDistance_km(burea, burtrask, scale * 35);

//		scenario.setSymmetricDistance_km(boliden, kage, 25);
//		scenario.setSymmetricDistance_km(boliden, centrum, 25);
//		scenario.setSymmetricDistance_km(boliden, campus, 25);
//		scenario.setSymmetricDistance_km(boliden, hamn, 25);
//		scenario.setSymmetricDistance_km(boliden, burea, 25);
//		scenario.setSymmetricDistance_km(boliden, burtrask, 25);
//
//		scenario.setSymmetricDistance_km(kage, centrum, 25);
//		scenario.setSymmetricDistance_km(kage, campus, 25);
//		scenario.setSymmetricDistance_km(kage, hamn, 25);
//		scenario.setSymmetricDistance_km(kage, burea, 25);
//		scenario.setSymmetricDistance_km(kage, burtrask, 25);
//
//		scenario.setSymmetricDistance_km(centrum, campus, 25);
//		scenario.setSymmetricDistance_km(centrum, hamn, 25);
//		scenario.setSymmetricDistance_km(centrum, burea, 25);
//		scenario.setSymmetricDistance_km(centrum, burtrask, 25);
//
//		scenario.setSymmetricDistance_km(campus, hamn, 25);
//		scenario.setSymmetricDistance_km(campus, burea, 25);
//		scenario.setSymmetricDistance_km(campus, burtrask, 25);
//
//		scenario.setSymmetricDistance_km(hamn, burea, 25);
//		scenario.setSymmetricDistance_km(hamn, burtrask, 25);
//
//		scenario.setSymmetricDistance_km(burea, burtrask, 25);

		/*
		 * Create simulator.
		 */

		// Simulator has default parking/charging and driving logics.
		Simulator simulator = new Simulator(scenario);
		// Below an example of how alternative charging logics can be inserted.
		simulator.setParkingSimulator(new V2GParkingSimulator(scenario, campus));

		/*
		 * Define preferences for round trip sampling.
		 */

		Preferences consistencyPreferences = new Preferences();
		consistencyPreferences.addComponent(new StrategyRealizationConsistency(scenario), 1.0);
		consistencyPreferences.addComponent(new AllDayTimeConstraintPreference(), 1.0);
		consistencyPreferences.addComponent(new NonnegativeBatteryStatePreference(), 1.0);

		Preferences allPreferences = new Preferences();
		allPreferences.addPreferences(consistencyPreferences);
//		allPreferences.addComponent(new AtHomePreference(22.0, 6.0), 1.0);
//		allPreferences.addComponent(new NotHomePreference(campus), 10.0);
//		allPreferences.addComponent(new LocalChargingAmountPrefence(scenario, campus), 0.1);

		
//		allPreferences.addComponent(new AtLocationPreference(campus, 10.0, 14.0));
//		LocationAttractivityPreference locPref = new LocationAttractivityPreference();
//		locPref.setAttractivity(boliden, 1.566);
//		locPref.setAttractivity(kage,2.248);
//		locPref.setAttractivity(centrum, 74.702);
//		locPref.setAttractivity(campus, 10.000);
//		locPref.setAttractivity(hamn, 10.000);
//		locPref.setAttractivity(burea, 2.360);
//		locPref.setAttractivity(burtrask, 1.575);
//		allPreferences.addComponent(locPref);

		/*
		 * Run MH algorithm.
		 */

		int iterations = 20 * 1000 * 1000;
		MHAlgorithm<RoundTrip<Location>> algo = scenario.createMHAlgorithm(allPreferences, simulator);

		// StationaryStats is an example of how to extract statistics from an MH run.
//		MHStateProcessor<RoundTrip<Location>> stats = new StationarityStats(simulator, campus, iterations / 100);
//		algo.addStateProcessor(stats);

		algo.addStateProcessor(new LocationVisitAnalyzer(scenario, new Preferences(), iterations / 2, 10));

		algo.setMsgInterval(iterations / 100);
		algo.run(iterations);
	}
}