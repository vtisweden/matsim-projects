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

import se.vti.roundtrips.logging.AbstractStateProcessor;
import se.vti.roundtrips.multiple.MultiRoundTrip;
import se.vti.roundtrips.simulator.Episode;
import se.vti.roundtrips.simulator.StayEpisode;

/**
 * 
 * @author GunnarF
 *
 */
class AgeStats extends AbstractStateProcessor<MultiRoundTrip<GridNodeWithActivity>> {

	private final List<Person> syntheticPopulation;

	private long[] workMoreThanOneHourAges;
	private long[] educationMoreThanOneHourAges;
	private long[] otherMoreThanOneHourAges;

	AgeStats(long burnInIterations, long samplingInterval, List<Person> syntheticPopulation) {
		super(burnInIterations, samplingInterval);
		this.syntheticPopulation = syntheticPopulation;
		this.workMoreThanOneHourAges = new long[10];
		this.educationMoreThanOneHourAges = new long[10];
		this.otherMoreThanOneHourAges = new long[10];
	}

	@Override
	public void processStateHook(MultiRoundTrip<GridNodeWithActivity> multiRoundTrip) {
		for (int roundTripIndex = 0; roundTripIndex < multiRoundTrip.size(); roundTripIndex++) {
			var roundTrip = multiRoundTrip.getRoundTrip(roundTripIndex);
			double workDur_h = 0.0;
			double eduDur_h = 0.0;
			double otherDur_h = 0.0;
			List<Episode> episodes = roundTrip.getEpisodes();
			for (int i = 0; i < episodes.size(); i += 2) {
				var stay = (StayEpisode<GridNodeWithActivity>) episodes.get(i);
				if (Activity.WORK.equals(stay.getLocation().getActivity())) {
					workDur_h += stay.getDuration_h();
				} else if (Activity.EDUCATION.equals(stay.getLocation().getActivity())) {
					eduDur_h += stay.getDuration_h();
				} else if (Activity.OTHER.equals(stay.getLocation().getActivity())) {
					otherDur_h += stay.getDuration_h();
				}
			}
			if (roundTrip.getIndex() != roundTripIndex) {
				throw new RuntimeException("Indexing error!");
			}
			int age = this.syntheticPopulation.get(roundTrip.getIndex()).age;
			if (workDur_h >= 1.0) {
				this.workMoreThanOneHourAges[age / 10]++;
			}
			if (eduDur_h >= 1.0) {
				this.educationMoreThanOneHourAges[age / 10]++;
			}
			if (otherDur_h >= 1.0) {
				this.otherMoreThanOneHourAges[age / 10]++;
			}
		}
	}

	@Override
	public void end() {
		super.end();

		double workSum = Arrays.stream(this.workMoreThanOneHourAges).sum();
		double eduSum = Arrays.stream(this.educationMoreThanOneHourAges).sum();
		double otherSum = Arrays.stream(this.otherMoreThanOneHourAges).sum();

		System.out.println("----------------------------------------");
		System.out.println("age\twork\teducation\tother");
		for (int ageBin = 0; ageBin < 10; ageBin++) {
			System.out.println(
					10 * ageBin + " to " + 10 * (ageBin + 1) + "\t" + this.workMoreThanOneHourAges[ageBin] / workSum
							+ "\t" + this.educationMoreThanOneHourAges[ageBin] / eduSum + "\t"
							+ otherMoreThanOneHourAges[ageBin] / otherSum);
		}
		System.out.println("----------------------------------------");
	}
}
