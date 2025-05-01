/**
 * se.vti.skellefteaV2X
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
package se.vti.roundtrips.preferences;

import se.vti.roundtrips.model.StayEpisode;
import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class StrategyRealizationPreference<R extends RoundTrip<L>, L extends Location> extends PreferenceComponent<R> {

	private final Scenario<?> scenario;

	public StrategyRealizationPreference(Scenario<?> scenario) {
		this.scenario = scenario;
	}

	public double discrepancy_h(R simulatedRoundTrip) {
		if (simulatedRoundTrip.locationCnt() == 1) {
			return 0.0;
		} else {
			double discrepancy_h = 0.0;
			for (int i = 0; i < simulatedRoundTrip.locationCnt(); i++) {
				StayEpisode<?> parking = (StayEpisode<?>) simulatedRoundTrip.getEpisodes().get(2 * i);
				assert (parking.getEndTime_h() >= 0.0);
				discrepancy_h += Math.abs(
						this.scenario.getBinSize_h() * simulatedRoundTrip.getDeparture(i) - parking.getEndTime_h());
			}
			return discrepancy_h;
		}
	}

	@Override
	public double logWeight(R simulatedRoundTrip) {
		return -this.discrepancy_h(simulatedRoundTrip) / this.scenario.getPeriodLength_h();
	}

}
