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
import se.vti.skellefteaV2X.model.Scenario;
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

		final int timeBinCnt = 24;
		final double binSize_h = 24.0 / timeBinCnt;

		// TODO CONTINUE HERE
		final double chargingRate_kW = 11.0;
		final double maxCharge_kWh = 60.0;
		final double consumptionRate_kWh_km = 0.2;
		final double speed_km_h = 60.0;

		Scenario scenario = new Scenario(chargingRate_kW, maxCharge_kWh, consumptionRate_kWh_km, speed_km_h, binSize_h);

		Location boliden = scenario.createAndAddLocation("Boliden", false);
		Location kage = scenario.createAndAddLocation("Kåge", false);
		Location centrum = scenario.createAndAddLocation("Centrum", false);
		Location campus = scenario.createAndAddLocation("Campus", false);
		Location hamn = scenario.createAndAddLocation("Hamn", false);
		Location burea = scenario.createAndAddLocation("Bureå", false);
		Location burtrask = scenario.createAndAddLocation("Burträsk", false);

		scenario.setDistance_km(boliden, kage, 38, true);
		scenario.setDistance_km(boliden, centrum, 34, true);
		scenario.setDistance_km(boliden, campus, 34, true);
		scenario.setDistance_km(boliden, hamn, 47, true);
		scenario.setDistance_km(boliden, burea, 55, true);
		scenario.setDistance_km(boliden, burtrask, 48, true);

		scenario.setDistance_km(kage, centrum, 13, true);
		scenario.setDistance_km(kage, campus, 13, true);
		scenario.setDistance_km(kage, hamn, 24, true);
		scenario.setDistance_km(kage, burea, 33, true);
		scenario.setDistance_km(kage, burtrask, 53, true);

		scenario.setDistance_km(centrum, campus, 5, true);
		scenario.setDistance_km(centrum, hamn, 13, true);
		scenario.setDistance_km(centrum, burea, 22, true);
		scenario.setDistance_km(centrum, burtrask, 42, true);

		scenario.setDistance_km(campus, hamn, 13, true);
		scenario.setDistance_km(campus, burea, 22, true);
		scenario.setDistance_km(campus, burtrask, 42, true);

		scenario.setDistance_km(hamn, burea, 11, true);
		scenario.setDistance_km(hamn, burtrask, 46, true);

		scenario.setDistance_km(burea, burtrask, 35, true);

		// BUILD ROUNDTRIPS SAMPLER

		int maxLocations = 4;
		double locationProposalWeight = 0.1;
		double departureProposalWeight = 0.45;
		double chargingProposalWeight = 0.45;

		final RoundTripConfiguration<Location> roundTrips = new RoundTripConfiguration<>(maxLocations, timeBinCnt,
				locationProposalWeight, departureProposalWeight, chargingProposalWeight);
		roundTrips.addLocation(boliden);
		roundTrips.addLocation(kage);
		roundTrips.addLocation(centrum);
		roundTrips.addLocation(campus);
		roundTrips.addLocation(hamn);
		roundTrips.addLocation(burea);
		roundTrips.addLocation(burtrask);

		// BUILD MH MACHINERY

		int iterations = 100 * 1000 * 1000;

		RoundTripSimulator simulator = new RoundTripSimulator(scenario);
		TargetWeights targetWeights = new TargetWeights(simulator);

		RoundTripProposal<Location> proposal = new RoundTripProposal<>(roundTrips);
		RoundTrip<Location> initialState = new RoundTrip<>(Arrays.asList(centrum), Arrays.asList(8),
				Arrays.asList(true));

		MHAlgorithm<RoundTrip<Location>> algo = new MHAlgorithm<>(proposal, targetWeights, new Random());
//		algo.addStateProcessor(prn);
		algo.setMsgInterval(iterations / 100);
		algo.setInitialState(initialState);
		algo.run(iterations);

		System.out.println("... DONE");
	}

}
