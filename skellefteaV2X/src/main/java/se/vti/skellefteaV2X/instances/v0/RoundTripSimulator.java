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

import java.util.ArrayList;
import java.util.List;

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripSimulator {

	private final Scenario scenario;

	public RoundTripSimulator(Scenario scenario) {
		this.scenario = scenario;
	}

	private DrivingEpisode newDrivingEpisode(Location origin, Location destination, double time_h, double charge_kWh) {
		final DrivingEpisode driving = new DrivingEpisode(origin, destination);
		driving.setStartTime_h(time_h);
		driving.setChargeAtStart_kWh(charge_kWh);
		final double dist_km = this.scenario.getDistance_km(origin, destination);
		driving.setEndTime_h(time_h + dist_km / this.scenario.getSpeed_km_h());
		driving.setChargeAtEnd_kWh(charge_kWh - dist_km * this.scenario.getConsumptionRate_kWh_km());
		return driving;
	}

	private ParkingEpisode newParkingEpisode(Location location, Integer departure, Boolean charging, double time_h,
			double charge_kWh) {
		final ParkingEpisode parking = new ParkingEpisode(location);
		parking.setStartTime_h(time_h);
		parking.setChargeAtStart_kWh(charge_kWh);
		parking.setEndTime_h(Math.max(time_h, this.scenario.getBinSize_h() * departure));
		if (location.getAllowsCharging() && charging) {
			charge_kWh += this.scenario.getChargingRate_kW() * (parking.getEndTime_h() - parking.getStartTime_h());
		}
		parking.setChargeAtEnd_kWh(charge_kWh);
		return parking;
	}

	public List<Episode> simulate(RoundTrip<Location> roundTrip) {

		if (roundTrip.size() == 1) {
			return new ArrayList<>(0);
		}

		final List<Episode> episodes = new ArrayList<>(2 * roundTrip.size() - 1);

		ParkingEpisode home = new ParkingEpisode(roundTrip.getLocation(0));
		episodes.add(home);

		double time_h = this.scenario.getBinSize_h() * roundTrip.getDeparture(0);
		double charge_kWh = this.scenario.getMaxCharge_kWh(); // initial guess
		home.setEndTime_h(time_h);
		home.setChargeAtEnd_kWh(charge_kWh);

		for (int index = 0; index < roundTrip.size(); index++) {
			final int nextIndex = roundTrip.successorIndex(index);

			final DrivingEpisode driving = this.newDrivingEpisode(roundTrip.getLocation(index),
					roundTrip.getLocation(nextIndex), time_h, charge_kWh);
			episodes.add(driving);
			time_h = driving.getEndTime_h();
			charge_kWh = driving.getChargeAtEnd_kWh();

			final ParkingEpisode parking = this.newParkingEpisode(roundTrip.getLocation(nextIndex),
					roundTrip.getDeparture(nextIndex), roundTrip.getCharging(nextIndex), time_h, charge_kWh);
			episodes.add(parking);
			time_h = parking.getEndTime_h();
			charge_kWh = parking.getChargeAtEnd_kWh();
		}

		final DrivingEpisode driving = this.newDrivingEpisode(roundTrip.getLocation(roundTrip.size() - 1),
				roundTrip.getLocation(0), time_h, charge_kWh);
		episodes.add(driving);
		time_h = driving.getEndTime_h();
		charge_kWh = driving.getChargeAtEnd_kWh();

		home.setStartTime_h(time_h - 24.0); // wrap-around
		home.setChargeAtStart_kWh(charge_kWh); // possible charge inconsistency at the home location

		return episodes;
	}

}
