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

import java.util.List;

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class TargetWeights implements MHWeight<RoundTrip<Location>> {

	private final RoundTripSimulator simulator;

	private final Location campus;
	private final double targetTime_h;
	private final double scale;
	private final double homeStart_h;
	private final double homeEnd_h;

	public TargetWeights(RoundTripSimulator simulator, Location campus, double targetTime_h, double scale,
			double homeStart_h, double homeEnd_h) {
		this.simulator = simulator;
		this.campus = campus;
		this.targetTime_h = targetTime_h;
		this.scale = scale;
		this.homeStart_h = homeStart_h;
		this.homeEnd_h = homeEnd_h;
	}

	double atHomeOffCampusLogWeight(List<Episode> episodes) {
		if (episodes == null) {
			return 0.0;
		}
		ParkingEpisode home = (ParkingEpisode) episodes.get(0);
		double discrepancy_h = 0.0;
		if (this.campus.equals(home.getLocation())) {
			discrepancy_h = this.homeEnd_h - this.homeStart_h;
		} else {
			discrepancy_h += Math.max(0, home.getStartTime_h() - this.homeStart_h);
			discrepancy_h += Math.max(0, this.homeEnd_h - home.getEndTime_h());

		}
		return -discrepancy_h;
	}

	double presenceLogWeight(List<Episode> episodes) {
		double minDist_h = 24.0;
		if (episodes != null) {
			for (Episode e : episodes) {
				if (e instanceof ParkingEpisode) {
					ParkingEpisode p = (ParkingEpisode) e;
					if (this.campus.equals(p.getLocation())) {
						final double candDist_h;
						if (this.targetTime_h < p.getStartTime_h()) {
							candDist_h = p.getStartTime_h() - this.targetTime_h;
						} else if (this.targetTime_h < p.getEndTime_h()) {
							candDist_h = 0.0;
						} else {
							candDist_h = this.targetTime_h - p.getEndTime_h();
						}
						minDist_h = Math.min(minDist_h, candDist_h);
					}
				}
			}
		}
		return -minDist_h;
	}

	double timeWrapAroundConsistencyLogWeight(List<Episode> episodes) {
		if (episodes == null) {
			return 0.0;
		}
		ParkingEpisode home = (ParkingEpisode) episodes.get(0);
		double timeDiscrepancy_h = Math.max(0.0, home.getStartTime_h() - home.getEndTime_h());
		return -timeDiscrepancy_h;
	}

	double chargingWrapAroundConsistencyLogWeight(List<Episode> episodes) {
		if (episodes == null) {
			return 0.0;
		}
		ParkingEpisode home = (ParkingEpisode) episodes.get(0);
		double chargingDiscrepancy_kWh = Math.abs(home.getChargeAtStart_kWh() - home.getChargeAtEnd_kWh());
		return -chargingDiscrepancy_kWh;
	}

	double chargingBoundConsistencyLogWeight(List<Episode> episodes) {
		if (episodes == null) {
			return 0.0;
		}
		double minCharging_kWh = Double.POSITIVE_INFINITY;
		double maxCharging_kWh = Double.NEGATIVE_INFINITY;
		double minDist_h = 24.0;
		for (Episode e : episodes) {
			if (e instanceof ParkingEpisode) {
				ParkingEpisode p = (ParkingEpisode) e;
				minCharging_kWh = Math.min(minCharging_kWh, Math.min(p.getChargeAtStart_kWh(), p.getChargeAtEnd_kWh()));
				maxCharging_kWh = Math.max(maxCharging_kWh, Math.max(p.getChargeAtStart_kWh(), p.getChargeAtEnd_kWh()));
				if (this.campus.equals(p.getLocation())) {
					final double candDist_h;
					if (this.targetTime_h < p.getStartTime_h()) {
						candDist_h = p.getStartTime_h() - this.targetTime_h;
					} else if (this.targetTime_h < p.getEndTime_h()) {
						candDist_h = 0.0;
					} else {
						candDist_h = this.targetTime_h - p.getEndTime_h();
					}
					minDist_h = Math.min(minDist_h, candDist_h);
				}
			}
		}
		final double excess_kWh = Math.max(0.0,
				(maxCharging_kWh - minCharging_kWh) - this.simulator.getScenario().getMaxCharge_kWh());
		return -excess_kWh;
	}

	@Override
	public double logWeight(RoundTrip<Location> roundTrip) {
		List<Episode> episodes = this.simulator.simulate(roundTrip);
		double result = 0.0;
		result += this.timeWrapAroundConsistencyLogWeight(episodes);
		result += this.chargingWrapAroundConsistencyLogWeight(episodes);
		result += this.chargingBoundConsistencyLogWeight(episodes);
		result += this.atHomeOffCampusLogWeight(episodes);
		result += this.presenceLogWeight(episodes);

		return this.scale * result;

	}

}
