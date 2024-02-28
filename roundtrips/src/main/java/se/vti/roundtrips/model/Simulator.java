/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class Simulator {

	public interface ParkingSimulator {

		public ParkingEpisode newParkingEpisode(Location location, Integer departure, double initialTime_h,
				VehicleState initialState);

	}

	public interface DrivingSimulator {

		public DrivingEpisode newDrivingEpisode(Location origin, Location destination, double initialTime_h,
				VehicleState initialState);

	}

	private final Scenario scenario;

	private DrivingSimulator drivingSimulator;
	private ParkingSimulator parkingSimulator;

	public Simulator(Scenario scenario) {
		this.scenario = scenario;
		this.drivingSimulator = new DefaultDrivingSimulator(scenario);
		this.parkingSimulator = new DefaultParkingSimulator(scenario);
	}

	public Scenario getScenario() {
		return this.scenario;
	}

	public void setDrivingSimulator(DrivingSimulator drivingSimulator) {
		this.drivingSimulator = drivingSimulator;
	}

	public void setParkingSimulator(ParkingSimulator parkingSimulator) {
		this.parkingSimulator = parkingSimulator;
	}

	public List<Episode> simulate(RoundTrip<Location> roundTrip) {

		if (roundTrip.locationCnt() == 1) {
			ParkingEpisode home = new ParkingEpisode(roundTrip.getLocation(0));
			home.setDuration_h(24.0);
			home.setEndTime_h(24.0 - 1e-8); // wraparound
//			double charge_kWh = (this.scenario.isAllowHomeCharging() && roundTrip.getCharging(0)
//					? this.scenario.getMaxCharge_kWh()
//					: 0.0);
//			home.setChargeAtStart_kWh(charge_kWh);
//			home.setChargeAtEnd_kWh(charge_kWh);
			home.setInitialState(new VehicleState());
			home.setFinalState(new VehicleState());

			return Collections.singletonList(home);
		}

		final double initialTime_h = this.scenario.getBinSize_h() * roundTrip.getDeparture(0);
//		double initialCharge_kWh = this.scenario.getMaxCharge_kWh(); // initial guess
		VehicleState initialState = new VehicleState();
		List<Episode> episodes;
		do {

			episodes = new ArrayList<>(2 * roundTrip.locationCnt() - 1);

			episodes.add(null); // placeholder for home episode

			double time_h = initialTime_h;
//			double charge_kWh = initialCharge_kWh;
			VehicleState currentState = initialState;

			for (int index = 0; index < roundTrip.locationCnt() - 1; index++) {

				final DrivingEpisode driving = this.drivingSimulator.newDrivingEpisode(roundTrip.getLocation(index),
						roundTrip.getLocation(index + 1), time_h, initialState);
				episodes.add(driving);
				time_h = driving.getEndTime_h();
//				charge_kWh = driving.getChargeAtEnd_kWh();
				currentState = driving.getFinalState();

				final ParkingEpisode parking = this.parkingSimulator.newParkingEpisode(roundTrip.getLocation(index + 1),
						roundTrip.getDeparture(index + 1), 
//						roundTrip.getCharging(index + 1), 
						time_h, currentState);
				episodes.add(parking);
				time_h = parking.getEndTime_h();
//				charge_kWh = parking.getChargeAtEnd_kWh();
				currentState = parking.getFinalState();
			}

			final DrivingEpisode driving = this.drivingSimulator.newDrivingEpisode(
					roundTrip.getLocation(roundTrip.locationCnt() - 1), roundTrip.getLocation(0), time_h, currentState);
			episodes.add(driving);
			time_h = driving.getEndTime_h();
//			charge_kWh = driving.getChargeAtEnd_kWh();
			currentState = driving.getFinalState();

			final ParkingEpisode home = this.parkingSimulator.newParkingEpisode(roundTrip.getLocation(0),
					roundTrip.getDeparture(0), 
//					this.scenario.isAllowHomeCharging() && roundTrip.getCharging(0),
					time_h - 24.0, currentState);
			episodes.set(0, home);

			// TODO postprocessing for wrap-around of battery level
//			final double newInitialCharge_kWh = home.getChargeAtEnd_kWh();
//			if ((newInitialCharge_kWh >= 0.0) && Math.abs(newInitialCharge_kWh - initialCharge_kWh) > 1e-3) {
//				episodes = null;
//				initialCharge_kWh = newInitialCharge_kWh;
//			}

		} while (episodes == null);

		return episodes;
	}

}
