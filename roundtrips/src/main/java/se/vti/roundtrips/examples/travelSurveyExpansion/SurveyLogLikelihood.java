/**
 * se.vti.roundtrips.examples.travelSurveyExpansion
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.examples.travelSurveyExpansion;

import java.util.Arrays;
import java.util.List;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.SamplingWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class SurveyLogLikelihood implements SamplingWeight<MultiRoundTrip<GridNodeWithActivity>> {

	private final List<SurveyResponse> responses;

	private final List<Person> syntheticPopulation;

	private final double[][] personLikelihood;

	SurveyLogLikelihood(List<SurveyResponse> responses, List<Person> syntheticPopulation) {
		this.responses = responses;
		this.syntheticPopulation = syntheticPopulation;

		this.personLikelihood = new double[responses.size()][syntheticPopulation.size()];
		for (int responseIndex = 0; responseIndex < responses.size(); responseIndex++) {
			SurveyResponse response = this.responses.get(responseIndex);
			for (int personIndex = 0; personIndex < syntheticPopulation.size(); personIndex++) {
				this.personLikelihood[responseIndex][personIndex] = response
						.personLikelihood(syntheticPopulation.get(personIndex));
			}
//			double sum = Arrays.stream(this.personWeights[responseIndex]).sum();
//			for (int personIndex = 0; personIndex < syntheticPopulation.size(); personIndex++) {
//				this.personWeights[responseIndex][personIndex] /= sum;
//			}
		}
	}

	@Override
	public double logWeight(MultiRoundTrip<GridNodeWithActivity> multiRoundTrip) {
		double result = 0.0;
		// This double look is slow. The inner loop could be parallelized.
		for (int responseIndex = 0; responseIndex < this.responses.size(); responseIndex++) {
			SurveyResponse response = this.responses.get(responseIndex);
			double[] weights = this.personLikelihood[responseIndex];
			double responseLikelihood = 0.0;
			for (int personIndex = 0; personIndex < this.syntheticPopulation.size(); personIndex++) {
				responseLikelihood += weights[personIndex]
						* response.travelLikelihood(multiRoundTrip.getRoundTrip(personIndex));
			}
			responseLikelihood /= multiRoundTrip.size();
			result += Math.log(Math.max(1e-8, responseLikelihood));
		}
		return result;
	}
}
