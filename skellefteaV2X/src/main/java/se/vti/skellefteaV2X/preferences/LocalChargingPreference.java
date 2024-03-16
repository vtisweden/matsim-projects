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
package se.vti.skellefteaV2X.preferences;

import java.util.List;

import se.vti.roundtrips.model.Episode;
import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.single.Location;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;
import se.vti.skellefteaV2X.model.ElectrifiedLocation;
import se.vti.skellefteaV2X.model.ElectrifiedScenario;
import se.vti.skellefteaV2X.model.ElectrifiedVehicleState;

/**
 * 
 * @author GunnarF
 *
 */
public class LocalChargingPreference extends Preferences.Component<ElectrifiedRoundTrip, ElectrifiedLocation> {
	
	private final ElectrifiedScenario scenario;
	
	private final Location location;

	private final double minCharging_kWh;
	
	public LocalChargingPreference(ElectrifiedScenario scenario, Location location, double minCharging_kWh) {
		this.scenario = scenario;
		this.location = location;
		this.minCharging_kWh = minCharging_kWh;
		this.setLogWeightThreshold(-1e-8);
	}

	@Override
	public double logWeight(ElectrifiedRoundTrip roundTrip) {
		double amount_kWh = 0.0;
		for (Episode<ElectrifiedVehicleState> e : (List<Episode<ElectrifiedVehicleState>>) roundTrip.getEpisodes()) {
			if (e instanceof ParkingEpisode) {
				ParkingEpisode<?, ElectrifiedVehicleState> p = (ParkingEpisode<?, ElectrifiedVehicleState>) e;
				if (this.location.equals(p.getLocation())) {
//					amount_kWh += p.getChargeAtEnd_kWh() - p.getChargeAtStart_kWh();
					amount_kWh += p.getFinalState().getBatteryCharge_kWh() - p.getInitialState().getBatteryCharge_kWh();
				}
			}
		}
		return Math.min(0.0, amount_kWh - this.minCharging_kWh) / this.scenario.getMaxCharge_kWh();
	}

}
