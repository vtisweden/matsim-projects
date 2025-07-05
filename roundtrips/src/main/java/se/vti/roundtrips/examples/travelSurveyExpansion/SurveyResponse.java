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

import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.MoveEpisode;
import se.vti.roundtrips.simulator.StayEpisode;
import se.vti.roundtrips.single.RoundTrip;

/**
 * 
 * @author GunnarF
 *
 */
class SurveyResponse {

	private final Person respondent;

	private final double reportedWorkDuration_h;
	private final double reportedEducationDuration_h;
	private final double reportedOtherDuration_h;
	private final double reportedTravelDuration_h;

	SurveyResponse(Person respondent, double reportedWorkDuration_h, double reportedEducationDuration_h,
			double reportedOtherDuration_h, double reportedTravelDuration_h) {
		this.respondent = respondent;
		this.reportedWorkDuration_h = reportedWorkDuration_h;
		this.reportedEducationDuration_h = reportedEducationDuration_h;
		this.reportedOtherDuration_h = reportedOtherDuration_h;
		this.reportedTravelDuration_h = reportedTravelDuration_h;
	}

	public double personSimilarity(Person syntheticPerson) {
		if (syntheticPerson.inStudentAge() && this.respondent.inStudentAge()
				|| syntheticPerson.inMidlifeAge() && this.respondent.inMidlifeAge()
				|| syntheticPerson.inRetirementAge() && this.respondent.inRetirementAge()) {
			return 1.0;
		} else {
			return 0.01;
		}
	}

	public double travelSimilarity(RoundTrip<GridNodeWithActivity> simulatedRoundTrip) {

		List<Episode> episodes = simulatedRoundTrip.getEpisodes();

		double simulatedWorkDuration_h = 0.0;
		double simulatedEducationDuration_h = 0.0;
		double simulatedOtherDuration_h = 0.0;
		for (int stayEpisodeIndex = 0; stayEpisodeIndex < episodes.size(); stayEpisodeIndex += 2) {
			var stay = (StayEpisode<GridNodeWithActivity>) episodes.get(stayEpisodeIndex);
			if (Activity.WORK.equals(stay.getLocation().getActivity())) {
				simulatedEducationDuration_h += stay.getDuration_h();
			} else if (Activity.EDUCATION.equals(stay.getLocation().getActivity())) {
				simulatedEducationDuration_h += stay.getDuration_h();
			} else if (Activity.OTHER.equals(stay.getLocation().getActivity())) {
				simulatedOtherDuration_h += stay.getDuration_h();
			}
		}

		double simulatedTravelDuration_h = 0.0;
		for (int travelEpisodeIndex = 1; travelEpisodeIndex < episodes.size(); travelEpisodeIndex += 2) {
			var move = (MoveEpisode<GridNodeWithActivity>) episodes.get(travelEpisodeIndex);
			simulatedTravelDuration_h += move.getDuration_h();
		}

		return Math.exp(-0.5 * (Math.pow(simulatedWorkDuration_h - this.reportedWorkDuration_h, 2.0)
				+ Math.pow(simulatedEducationDuration_h - this.reportedEducationDuration_h, 2.0)
				+ Math.pow(simulatedOtherDuration_h - this.reportedOtherDuration_h, 2.0)
				+ Math.pow(simulatedTravelDuration_h - this.reportedTravelDuration_h, 2.0)));
	}

}
