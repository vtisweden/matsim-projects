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

import se.vti.roundtrips.model.DefaultSimulator.StaySimulator;
import se.vti.roundtrips.single.Location;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
public class DefaultStaySimulator<L extends Location> implements StaySimulator<L> {

	protected final Scenario<L> scenario;

	public DefaultStaySimulator(Scenario<L> scenario) {
		this.scenario = scenario;
	}

	public Object computeFinalState(RoundTrip<L> roundTrip, int roundTripIndex, StayEpisode<L> parking) {
		return null;
	}

	@Override
	public StayEpisode<L> newStayEpisode(RoundTrip<L> roundTrip, int roundTripIndex, double time_h,
			Object initialState) {
		final StayEpisode<L> stay = new StayEpisode<>(roundTrip.getLocation(roundTripIndex));
		stay.setInitialState(initialState);

		stay.setEndTime_h(Math.max(time_h, this.scenario.getBinSize_h() * roundTrip.getDeparture(roundTripIndex)));
		stay.setDuration_h(stay.getEndTime_h() - time_h);

		stay.setFinalState(this.computeFinalState(roundTrip, roundTripIndex, stay));

		return stay;
	}

}
