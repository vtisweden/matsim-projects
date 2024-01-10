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
import se.vti.skellefteaV2X.roundtrips.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class RoundTripSimulator {

	private final Scenario scenario;

	private final Location campus;

	private final int startV2G;

	private final int endV2G;

	private final double chargingRate_kW;

	private final double consumption_kWh_km;

	private final double speed_km_h;

	public RoundTripSimulator(Scenario scenario, Location campus, int v2gStartTime, int v2gEndTime,
			double chargingRate_kW, double consumption_kWh_km, double speed_km_h) {
		this.scenario = scenario;
		this.campus = campus;
		this.startV2G = v2gStartTime;
		this.endV2G = v2gEndTime;
		this.chargingRate_kW = chargingRate_kW;
		this.consumption_kWh_km = consumption_kWh_km;
		this.speed_km_h = speed_km_h;
	}

	public SimulationStats simulate(RoundTrip<Location> roundTrip) {

		if (roundTrip.size() == 1) {
			return null;
		}
		
		SimulationStats stats = new SimulationStats();

		double time_h = roundTrip.getDeparture(0) * this.scenario.getBinSize_h();
		stats.setStartTime_h(time_h);
		
		double charge_kWh = 0.0;
		stats.addCharge_kWh(0);

		for (int i = 0; i <= roundTrip.size(); i++) {
			Location location = roundTrip.getLocation(i);
			Location nextLocation = roundTrip.getSuccessorLocation(i);

			final double distance_km = this.scenario.getDistance_km(location, nextLocation);
			final double consumption_kWh = distance_km * this.consumption_kWh_km;
			time_h += distance_km / this.speed_km_h;
			charge_kWh -= consumption_kWh;
			stats.addCharge_kWh(charge_kWh);

			final double nextDeparture_h = Math.max(roundTrip.getNextDeparture(i), time_h);
			if (roundTrip.getCharging(i) && location.getAllowsCharging()) {
				final double parkingDuration_h = Math.max(0.0, nextDeparture_h - time_h);
				charge_kWh += parkingDuration_h * this.chargingRate_kW;
			}
			stats.addCharge_kWh(charge_kWh);
			time_h = nextDeparture_h;
		}
		stats.setEndTime_h(time_h);
		
		
		return stats;
	}

}
