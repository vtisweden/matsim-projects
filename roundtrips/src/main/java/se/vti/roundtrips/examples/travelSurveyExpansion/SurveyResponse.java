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

	private final double reportedWorkDur_h;
	private final double reportedEduDur_h;
	private final double reportedOtherDur_h;

	SurveyResponse(Person respondent, double reportedWorkDuration_h, double reportedEducationDuration_h,
			double reportedOtherDuration_h) {
		this.respondent = respondent;
		this.reportedWorkDur_h = reportedWorkDuration_h;
		this.reportedEduDur_h = reportedEducationDuration_h;
		this.reportedOtherDur_h = reportedOtherDuration_h;
	}

	public double personLikelihood(Person syntheticPerson) {
		if (syntheticPerson.inStudentAge() && this.respondent.inStudentAge()
				|| syntheticPerson.inMidlifeAge() && this.respondent.inMidlifeAge()
				|| syntheticPerson.inRetirementAge() && this.respondent.inRetirementAge()) {
			return 1.0 - 0.02;
		} else {
			return 0.01;
		}
	}

	private double timeSimilarity(double simulated_h, double reported_h) {
		double dev_h = simulated_h - reported_h;
		double stddev_h = Math.max(0.4 * reported_h, 1e-2);
		return 1.0 / Math.sqrt(2.0 * Math.PI) / stddev_h * Math.exp(-dev_h * dev_h / 2.0 / stddev_h / stddev_h);
	}

	public double travelLikelihood(RoundTrip<GridNodeWithActivity> simulatedRoundTrip) {
		List<Episode> episodes = simulatedRoundTrip.getEpisodes();
		double simWorkDur_h = 0.0;
		double simEduDur_h = 0.0;
		double simOtherDur_h = 0.0;
		for (int i = 0; i < episodes.size(); i += 2) {
			var stay = (StayEpisode<GridNodeWithActivity>) episodes.get(i);
			var activity = stay.getLocation().getActivity();
			var duration_h = stay.getDuration_h();
			if (Activity.WORK.equals(activity)) {
				simWorkDur_h += duration_h;
			} else if (Activity.EDUCATION.equals(activity)) {
				simEduDur_h += duration_h;
			} else if (Activity.OTHER.equals(activity)) {
				simOtherDur_h += duration_h;
			}
		}
		return timeSimilarity(simWorkDur_h, this.reportedWorkDur_h) * timeSimilarity(simEduDur_h, this.reportedEduDur_h)
				* timeSimilarity(simOtherDur_h, this.reportedOtherDur_h);
	}
}
