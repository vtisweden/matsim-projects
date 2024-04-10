/**
 * od2roundtrips.model
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.od2roundtrips.model;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public class SimpleStatsLogger implements MHStateProcessor<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> {

	final String errorsFile = "errors.log";
	final String locationsFile = "locationCounts.log";
	final String departuresFile = "departures.log";

	private final Scenario<?> scenario;

	private final int interval;

	private long iteration;

	public SimpleStatsLogger(Scenario<?> scenario, int interval) {
		this.scenario = scenario;
		this.interval = interval;
	}

	private void append(String line, String fileName) {
		try {
			FileUtils.writeStringToFile(new File(fileName), line + "\n", true);
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}
	}

	@Override
	public void start() {
		this.iteration = 0;
		new File(this.errorsFile).delete();
		new File(this.locationsFile).delete();
		new File(this.departuresFile).delete();
	}

	@Override
	public void processState(MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> state) {

		if (this.iteration++ % this.interval == 0) {

			//
			this.append("" + state.getODReproductionError(), this.errorsFile);

			//
			final int[] lengths = new int[scenario.getMaxParkingEpisodes()];
			for (RoundTrip<TAZ> r : state) {
				lengths[r.locationCnt() - 1]++;
			}
			this.append(Arrays.stream(lengths).mapToObj(l -> "" + l).collect(Collectors.joining("\t")),
					this.locationsFile);

			//
			final int[] departures = new int[scenario.getBinCnt()];
			for (RoundTrip<TAZ> r : state) {
				if (r.locationCnt() > 1) {
					for (int i = 0; i < r.locationCnt(); i++) {
						departures[r.getDeparture(i)]++;
					}
				}
			}
			this.append(Arrays.stream(departures).mapToObj(l -> "" + l).collect(Collectors.joining("\t")),
					this.departuresFile);


		}
	}

	@Override
	public void end() {
	}

}
