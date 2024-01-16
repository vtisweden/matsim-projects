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

import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.Preferences;
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

	private final long burnInIterations;

	private final long samplingInterval;

	private final Preferences importanceSamplingPreferences;

	protected long iteration = 0;
	
	private double sampleWeightSum = 0.0;

	private double acceptedSampleWeightSum = 0.0;
	
	public SimulatedRoundTripAnalyzer(Scenario scenario, long burnInIterations, long samplingInterval,
			Preferences importanceSamplingPreferences) {
		this.scenario = scenario;
		this.burnInIterations = burnInIterations;
		this.samplingInterval = samplingInterval;
		this.importanceSamplingPreferences = importanceSamplingPreferences;
	}

//	public SimulatedRoundTripAnalyzer(Scenario scenario, long burnInIterations, long samplingInterval) {
//		this.scenario = scenario;
//		this.burnInIterations = burnInIterations;
//		this.samplingInterval = samplingInterval;
//		this.importanceSamplingPreferences = new Preferences();
//	}

	@Override
	public void start() {
	}

	@Override
	public final void processState(RoundTrip<Location> state) {
		this.iteration++;
		if ((this.iteration > this.burnInIterations) && (this.iteration % this.samplingInterval == 0)) {
			final SimulatedRoundTrip simulatedRoundTrip = (SimulatedRoundTrip) state;
			final double sampleWeight = 1.0
					/ Math.exp(this.importanceSamplingPreferences.logWeight(simulatedRoundTrip));
			this.sampleWeightSum += sampleWeight;
			if (this.importanceSamplingPreferences.thresholdPassed(simulatedRoundTrip)) {
				this.processRelevantState(simulatedRoundTrip, sampleWeight);
				this.acceptedSampleWeightSum += sampleWeight;
			}
		}
	}

	@Override
	public void end() {
	}

	public abstract void processRelevantState(SimulatedRoundTrip state, double sampleWeight);

	protected double sampleWeightSum() {
		return this.sampleWeightSum;
	}

	protected double acceptedSampleWeightSum() {
		return this.acceptedSampleWeightSum;
	}

	protected double acceptanceRate() {
		return this.acceptedSampleWeightSum / this.sampleWeightSum;
	}
	
}
