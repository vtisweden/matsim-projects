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
package se.vti.skellefteaV2X.instances.hEART2024;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import se.vti.skellefteaV2X.analysis.LocationVisitAnalyzer;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Preferences;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.Simulator;
import se.vti.skellefteaV2X.preferences.HomeLocationShare;
import se.vti.skellefteaV2X.preferences.LocalChargingPreference;
import se.vti.skellefteaV2X.preferences.OffHomeLocationShare;
import se.vti.skellefteaV2X.preferences.consistency.AllDayBatteryConstraintPreference;
import se.vti.skellefteaV2X.preferences.consistency.AllDayTimeConstraintPreference;
import se.vti.skellefteaV2X.preferences.consistency.NonnegativeBatteryStatePreference;
import se.vti.skellefteaV2X.preferences.consistency.StrategyRealizationConsistency;
import se.vti.skellefteaV2X.preferences.consistency.UniformOverLocationCount;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.skellefteaV2X.simulators.V2GParkingSimulator;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;

/**
 * 
 * @author GunnarF
 *
 */
public class Runner {

	static Runnable createMHAlgorithmRunnable(long iterations, String outputFileName) {
		/*
		 * Define study region.
		 * 
		 */

		final double distanceScale = 1.0;

		// Scenario has setters for non-default scenario parameters.
		Scenario scenario = new Scenario();
		scenario.setMaxParkingEpisodes(4);

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
		scenario.setSymmetricDistance_km(boliden, kage, distanceScale * 38);
		scenario.setSymmetricDistance_km(boliden, centrum, distanceScale * 34);
		scenario.setSymmetricDistance_km(boliden, campus, distanceScale * 34);
		scenario.setSymmetricDistance_km(boliden, hamn, distanceScale * 47);
		scenario.setSymmetricDistance_km(boliden, burea, distanceScale * 55);
		scenario.setSymmetricDistance_km(boliden, burtrask, distanceScale * 48);

		scenario.setSymmetricDistance_km(kage, centrum, distanceScale * 13);
		scenario.setSymmetricDistance_km(kage, campus, distanceScale * 13);
		scenario.setSymmetricDistance_km(kage, hamn, distanceScale * 24);
		scenario.setSymmetricDistance_km(kage, burea, distanceScale * 33);
		scenario.setSymmetricDistance_km(kage, burtrask, distanceScale * 53);

		scenario.setSymmetricDistance_km(centrum, campus, distanceScale * 5);
		scenario.setSymmetricDistance_km(centrum, hamn, distanceScale * 13);
		scenario.setSymmetricDistance_km(centrum, burea, distanceScale * 22);
		scenario.setSymmetricDistance_km(centrum, burtrask, distanceScale * 42);

		scenario.setSymmetricDistance_km(campus, hamn, distanceScale * 13);
		scenario.setSymmetricDistance_km(campus, burea, distanceScale * 22);
		scenario.setSymmetricDistance_km(campus, burtrask, distanceScale * 42);

		scenario.setSymmetricDistance_km(hamn, burea, distanceScale * 11);
		scenario.setSymmetricDistance_km(hamn, burtrask, distanceScale * 46);

		scenario.setSymmetricDistance_km(burea, burtrask, distanceScale * 35);

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

		// CONSISTENCY PREFERENCES

		final Preferences consistencyPreferences = new Preferences();
		consistencyPreferences.addComponent(new UniformOverLocationCount(scenario), 1.0 /* must be one */);
		consistencyPreferences.addComponent(new StrategyRealizationConsistency(scenario), 1);
		consistencyPreferences.addComponent(new AllDayTimeConstraintPreference(), 1);
		consistencyPreferences.addComponent(new AllDayBatteryConstraintPreference(scenario), 4);
		consistencyPreferences.addComponent(new NonnegativeBatteryStatePreference(scenario), 1);

		// MODELING PREFERENCES

		final Preferences modelingPreferences = new Preferences();

		final HomeLocationShare homeShare = new HomeLocationShare(8.0, 12.0, 6.0, 10.0);
		homeShare.setShare(boliden, 1.0);
		homeShare.setShare(kage, 1.0);
		homeShare.setShare(centrum, 5.0);
		homeShare.setShare(campus, 0.01);
		homeShare.setShare(hamn, 0.01);
		homeShare.setShare(burea, 1.0);
		homeShare.setShare(burtrask, 1.0);
		modelingPreferences.addComponent(homeShare, 1.0 /* must be one */);

		final OffHomeLocationShare offHomeShare = new OffHomeLocationShare(8.0, 12.0, 18.0, 10.0);
		offHomeShare.setShare(boliden, 1.0);
		offHomeShare.setShare(kage, 1.0);
		offHomeShare.setShare(centrum, 5.0);
		offHomeShare.setShare(campus, 2.0);
		offHomeShare.setShare(hamn, 2.0);
		offHomeShare.setShare(burea, 1.0);
		offHomeShare.setShare(burtrask, 1.0);
		modelingPreferences.addComponent(offHomeShare, 1.0 /* must be one */);

		// ANALYSIS PREFERENCES

		final Preferences importanceSamplingPreferences = new Preferences();

//		LocalChargingPreference localChargingPreference = new LocalChargingPreference(scenario, campus, 10.0);
//		importanceSamplingPreferences.addComponent(localChargingPreference, 1.0);

		/*
		 * Run MH algorithm.
		 */

		Preferences allPreferences = new Preferences(consistencyPreferences, modelingPreferences,
				importanceSamplingPreferences);
		MHAlgorithm<RoundTrip<Location>> algo = scenario.createMHAlgorithm(allPreferences, simulator);

		final long targetSamples = 1000 * 1000;
		final long burnInIterations = (iterations / 4);
		final long samplingInterval = (iterations - burnInIterations) / targetSamples;
		algo.addStateProcessor(new LocationVisitAnalyzer(scenario, iterations / 2, samplingInterval, outputFileName,
				importanceSamplingPreferences));

		algo.setMsgInterval(samplingInterval);

		return new Runnable() {
			@Override
			public void run() {
				algo.run(iterations);
			}
		};
	}

	public static void main(String[] args) {

//		createMHAlgorithmRunnable(10 * 1000 * 1000, "10-000-000_skelleftea.log").run();
//		System.exit(0);

		final ExecutorService threadPool = Executors.newFixedThreadPool(4);
		threadPool.execute(createMHAlgorithmRunnable(10 * 1000 * 1000, "10-000-000_skelleftea.log"));
		threadPool.execute(createMHAlgorithmRunnable(20 * 1000 * 1000, "20-000-000_skelleftea.log"));
		threadPool.execute(createMHAlgorithmRunnable(40 * 1000 * 1000, "40-000-000_skelleftea.log"));
		threadPool.execute(createMHAlgorithmRunnable(80 * 1000 * 1000, "80-000-000_skelleftea.log"));
		threadPool.shutdown();
		try {
			threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return;
		}
	}
}