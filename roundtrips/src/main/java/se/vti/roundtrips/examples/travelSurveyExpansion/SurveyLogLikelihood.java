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

import java.util.List;

import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.samplingweights.SamplingWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class SurveyLogLikelihood implements SamplingWeight<MultiRoundTrip<GridNodeWithActivity>> {

	private final List<SurveyResponse> respondents;

	private final List<Person> syntheticPopulation;

	SurveyLogLikelihood(List<SurveyResponse> respondents, List<Person> syntheticPopulation) {
		this.respondents = respondents;
		this.syntheticPopulation = syntheticPopulation;
	}

	@Override
	public double logWeight(MultiRoundTrip<GridNodeWithActivity> multiRoundTrip) {
		double similaritySum = 0.0;
		for (int syntheticPersonIndex = 0; syntheticPersonIndex < multiRoundTrip.size(); syntheticPersonIndex++) {
			var syntheticPerson = this.syntheticPopulation.get(syntheticPersonIndex);
			var simulatedTravel = multiRoundTrip.getRoundTrip(syntheticPersonIndex);
			for (var response : this.respondents) {
				similaritySum += response.personSimilarity(syntheticPerson)
						* response.travelSimilarity(simulatedTravel);
			}
		}
		return Math.log(Math.max(1e-8, similaritySum));
	}
}
