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

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.Simulator;
import se.vti.skellefteaV2X.preferences.AllDayTimeConstraintPreference;
import se.vti.skellefteaV2X.preferences.AtHomeOffCampusPreference;
import se.vti.skellefteaV2X.preferences.NonnegativeBatteryStatePreference;
import se.vti.skellefteaV2X.preferences.OnCampusPreference;
import se.vti.skellefteaV2X.preferences.StrategyRealizationConsistency;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.skellefteaV2X.simulators.V2GParkingSimulator;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

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

		Preferences preferences = new Preferences();
		preferences.addComponent(new StrategyRealizationConsistency(scenario));
		preferences.addComponent(new AllDayTimeConstraintPreference());
		preferences.addComponent(new NonnegativeBatteryStatePreference());
		preferences.addComponent(new AtHomeOffCampusPreference(campus, -2.0, +6.0));
		preferences.addComponent(new OnCampusPreference(campus, 12.0));
		// Add as many preferences as desired.

		/*
		 * Run MH algorithm.
		 */

		int iterations = 10 * 1000 * 1000;
		MHAlgorithm<RoundTrip<Location>> algo = scenario.createMHAlgorithm(preferences, simulator);

		// StationaryStats is an example of how to extract statistics from an MH run.
		MHStateProcessor<RoundTrip<Location>> stats = new StationarityStats(simulator, campus, iterations / 100);
		algo.addStateProcessor(stats);

		algo.setMsgInterval(iterations / 100);
		algo.run(iterations);
	}
}
