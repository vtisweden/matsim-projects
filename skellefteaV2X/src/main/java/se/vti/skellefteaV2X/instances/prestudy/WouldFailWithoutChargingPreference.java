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
package se.vti.skellefteaV2X.instances.prestudy;

import java.util.List;

import se.vti.roundtrips.model.DrivingEpisode;
import se.vti.roundtrips.model.Episode;
import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;
import se.vti.skellefteaV2X.model.ElectrifiedDrivingSimulator;
import se.vti.skellefteaV2X.model.ElectrifiedLocation;
import se.vti.skellefteaV2X.model.ElectrifiedScenario;
import se.vti.skellefteaV2X.model.ElectrifiedSimulator;
import se.vti.skellefteaV2X.model.ElectrifiedVehicleState;
import se.vti.skellefteaV2X.model.ElectrifiedVehicleStateFactory;
import se.vti.skellefteaV2X.model.V2GParkingSimulator;

/**
 * 
 * @author GunnarF
 *
 */
public class WouldFailWithoutChargingPreference extends PreferenceComponent<ElectrifiedRoundTrip> {

	private final ElectrifiedScenario scenario;

	private final ElectrifiedLocation location;

	private final ElectrifiedSimulator simulator;
	
	private final double minDiscrepancy_kWh;

	public WouldFailWithoutChargingPreference(ElectrifiedScenario scenario, ElectrifiedLocation location, double minDiscrepancy_kWh) {
		this.scenario = scenario;
		this.location = location;
//		this.setLogWeightThreshold(-1e-8);
		this.minDiscrepancy_kWh = minDiscrepancy_kWh;

		this.simulator = new ElectrifiedSimulator(scenario, new ElectrifiedVehicleStateFactory());
		simulator.setDrivingSimulator(new ElectrifiedDrivingSimulator(scenario, new ElectrifiedVehicleStateFactory()));
		simulator.setParkingSimulator(new V2GParkingSimulator(location, scenario));
	}
	
	private double discrepancy_kWh(ElectrifiedRoundTrip roundTrip) {
		
		ElectrifiedRoundTrip noChargingRoundTrip = roundTrip.clone();
		for (int i = 0; i < noChargingRoundTrip.locationCnt(); i++) {
			if (this.location.equals(noChargingRoundTrip.getLocation(i))) {
				noChargingRoundTrip.setCharging(i, false);
			}
		}
		List<Episode<ElectrifiedVehicleState>> noChargingEpisodes = this.simulator.simulate(noChargingRoundTrip);

		double minSOC_kWh = Double.POSITIVE_INFINITY;
		for (Episode<ElectrifiedVehicleState> e : noChargingEpisodes) {
			if (e instanceof DrivingEpisode) {
				DrivingEpisode<?, ElectrifiedVehicleState> drive = (DrivingEpisode<?, ElectrifiedVehicleState>) e;
				minSOC_kWh = Math.min(minSOC_kWh, drive.getFinalState().getBatteryCharge_kWh());
			}
		}

		return Math.max(0, -minSOC_kWh);
	}

	@Override
	public boolean accept(ElectrifiedRoundTrip roundTrip) {
		return this.discrepancy_kWh(roundTrip) >= this.minDiscrepancy_kWh;
	}
	
	@Override
	public double logWeight(ElectrifiedRoundTrip roundTrip) {
		return this.discrepancy_kWh(roundTrip) / this.scenario.getMaxCharge_kWh() - 1.0;
	}

}
