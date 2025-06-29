/**
 * se.vti.roundtrips
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
package se.vti.roundtrips.model;

import se.vti.roundtrips.single.Node;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.roundtrips.weights.SamplingWeights;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public abstract class RoundTripAnalyzer<R extends RoundTrip<L>, L extends Node> implements MHStateProcessor<R> {

	private final long burnInIterations;

	private final long samplingInterval;

	private final SamplingWeights<R> importanceSamplingPreferences;

	protected long iteration = 0;

	private double sampleWeightSum = 0.0;

	private double acceptedSampleWeightSum = 0.0;

	public RoundTripAnalyzer(long burnInIterations, long samplingInterval,
			SamplingWeights<R> importanceSamplingPreferences) {
		this.burnInIterations = burnInIterations;
		this.samplingInterval = samplingInterval;
		this.importanceSamplingPreferences = importanceSamplingPreferences;
	}

	@Override
	public void start() {
	}

	@Override
	public final void processState(R roundTrip) {
		this.iteration++;
		if ((this.iteration > this.burnInIterations) && (this.iteration % this.samplingInterval == 0)) {
			final double sampleWeight = 1.0 / Math.exp(this.importanceSamplingPreferences.logWeight(roundTrip));
			this.sampleWeightSum += sampleWeight;
//			if (this.importanceSamplingPreferences.thresholdPassed(roundTrip)) {
			if (this.importanceSamplingPreferences.accept(roundTrip)) {
				this.processRelevantRoundTrip(roundTrip, sampleWeight);
				this.acceptedSampleWeightSum += sampleWeight;
			}
		}
	}

	@Override
	public void end() {
	}

	protected double sampleWeightSum() {
		return this.sampleWeightSum;
	}

	protected double acceptedSampleWeightSum() {
		return this.acceptedSampleWeightSum;
	}

	protected double acceptanceRate() {
		return this.acceptedSampleWeightSum / this.sampleWeightSum;
	}

	public abstract void processRelevantRoundTrip(R roundTrip, double sampleWeight);

}
