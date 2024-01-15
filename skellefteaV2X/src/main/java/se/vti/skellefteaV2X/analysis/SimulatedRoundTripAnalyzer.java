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
package se.vti.skellefteaV2X.analysis;

import java.util.ArrayList;
import java.util.List;

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.model.SimulatedRoundTrip;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class SimulatedRoundTripAnalyzer implements MHStateProcessor<RoundTrip<Location>> {

	protected final Scenario scenario;

	private final int burnInIterations;

	private final int samplingInterval;

	private long iteration = 0;

	public SimulatedRoundTripAnalyzer(Scenario scenario, int burnInIterations, int samplingInterval) {
		this.scenario = scenario;
		this.burnInIterations = burnInIterations;
		this.samplingInterval = samplingInterval;
	}

	@Override
	public void start() {
	}

	@Override
	public final void processState(RoundTrip<Location> state) {
		this.iteration++;
		if ((this.iteration > this.burnInIterations) && (this.iteration % this.samplingInterval == 0)) {
			this.processRelevantState((SimulatedRoundTrip) state);
		}
	}

	@Override
	public void end() {
	}

	public abstract void processRelevantState(SimulatedRoundTrip state);

}
