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
package se.vti.skellefteaV2X.preferences;

import java.util.Arrays;

import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class HomeLocationShare extends LocationShare {

	public HomeLocationShare(double targetDuration_h, double duration_h, double endTime_h, double overlapStrictness) {
		super(targetDuration_h, duration_h, endTime_h, overlapStrictness);
	}

	@Override
	public double logWeight(ElectrifiedRoundTrip simulatedRoundTrip) {
		return this.logWeight(Arrays.asList((ParkingEpisode<?, ?>) simulatedRoundTrip.getEpisodes().get(0)));
	}

}
