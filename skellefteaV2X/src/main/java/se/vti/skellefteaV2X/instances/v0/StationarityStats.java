/**
 * se.vti.skellefeaV2X
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
package se.vti.skellefteaV2X.instances.v0;

import java.util.ArrayList;
import java.util.List;

import floetteroed.utilities.math.BasicStatistics;
import se.vti.skellefteaV2X.model.Episode;
import se.vti.skellefteaV2X.model.Location;
import se.vti.skellefteaV2X.model.ParkingEpisode;
import se.vti.skellefteaV2X.model.RoundTripSimulator;
import se.vti.skellefteaV2X.model.Scenario;
import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public class StationarityStats implements MHStateProcessor<RoundTrip<Location>> {

	private final Scenario scenario;

	private final RoundTripSimulator simulator;

	private final Location campus;

	private final List<List<BasicStatistics>> sequenceOfStatsPerBin;

	private final int iterationsPerBlock;

	private List<BasicStatistics> statsPerBin;

	long cnt;

	public StationarityStats(Scenario scenario, Location campus, int iterationsPerBlock) {
		this.scenario = scenario;
		this.simulator = new RoundTripSimulator(scenario);
		this.campus = campus;
		this.sequenceOfStatsPerBin = new ArrayList<>();
		this.iterationsPerBlock = iterationsPerBlock;

		this.statsPerBin = newStatsPerTimeBin();
		this.cnt = 0;
	}

	private List<BasicStatistics> newStatsPerTimeBin() {
		List<BasicStatistics> result = new ArrayList<>(this.scenario.getBinCnt());
		for (int i = 0; i < this.scenario.getBinCnt(); i++) {
			result.add(new BasicStatistics());
		}
		return result;
	}

	@Override
	public void start() {
	}

	@Override
	public void processState(RoundTrip<Location> state) {
		if (this.cnt++ == this.iterationsPerBlock) {
			this.sequenceOfStatsPerBin.add(this.statsPerBin);
			this.statsPerBin = newStatsPerTimeBin();
			this.cnt = 0;
		}

		List<Episode> episodes = this.simulator.simulate(state);
		if (episodes == null) {

			if (this.campus.equals(state.getLocation(0))) {
				for (int bin = 0; bin < this.scenario.getBinCnt(); bin++) {
					this.statsPerBin.get(bin).add(1.0);
				}
			}

		} else {

			for (Episode e : episodes) {
				if (e instanceof ParkingEpisode) {
					ParkingEpisode p = (ParkingEpisode) e;
					if (this.campus.equals(p.getLocation())) {
						int startBin = (int) (p.getStartTime_h() / this.simulator.getScenario().getBinSize_h());
						int endBin = (int) (p.getEndTime_h() / this.simulator.getScenario().getBinSize_h());
						if (startBin >= 0) {
							for (int bin = startBin; bin <= Math.min(endBin, this.scenario.getBinCnt() - 1); bin++) {
								this.statsPerBin.get(bin).add(1.0);
							}
						} else {
							// wrap-around activity
							assert (state.getLocation(0).equals(p.getLocation()));
							for (int bin = startBin + this.scenario.getBinCnt(); bin < this.scenario
									.getBinCnt(); bin++) {
								this.statsPerBin.get(bin).add(1.0);
							}
							for (int bin = 0; bin <= Math.min(endBin, this.scenario.getBinCnt() - 1); bin++) {
								this.statsPerBin.get(bin).add(1.0);
							}
						}
					}
				}
			}

		}
	}

	@Override
	public void end() {
		System.out.println();
		for (List<BasicStatistics> statsPerDeparture : this.sequenceOfStatsPerBin) {
			for (BasicStatistics stats : statsPerDeparture) {
				System.out.print(stats.size());
				System.out.print("\t");
			}
			System.out.println();
		}
	}

}
