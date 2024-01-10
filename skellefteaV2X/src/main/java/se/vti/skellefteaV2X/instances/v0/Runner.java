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
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.roundtrips.RoundTripConfiguration;

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

		Scenario scenario = new Scenario(binSize_h);

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

		final int maxLocations = 4;
		final double locationProposalWeight = 0.1;
		final double departureProposalWeight = 0.7;
		final double chargingProposalWeight = 0.2;

		RoundTripConfiguration<Location> roundTrips = new RoundTripConfiguration<>(maxLocations, timeBinCnt,
				locationProposalWeight, departureProposalWeight, chargingProposalWeight);
		roundTrips.addLocation(boliden);
		roundTrips.addLocation(kage);
		roundTrips.addLocation(centrum);
		roundTrips.addLocation(campus);
		roundTrips.addLocation(hamn);
		roundTrips.addLocation(burea);
		roundTrips.addLocation(burtrask);

		System.out.println("... DONE");
	}

}
