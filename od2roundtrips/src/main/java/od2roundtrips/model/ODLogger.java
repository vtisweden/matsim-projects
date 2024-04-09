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
package od2roundtrips.model;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import floetteroed.utilities.Tuple;
import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public class ODLogger implements MHStateProcessor<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> {

	final String errorsFile = "errors.txt";
	final String odFile = "ods.txt";
	final String locationsFile = "locationCounts.txt";
	final String departuresFile = "departures.txt";

	private final Scenario<?> scenario;

	private final Map<Tuple<TAZ, TAZ>, Double> targetODMatrix;

	private final int interval;

	private long iteration;

	public ODLogger(Scenario<?> scenario, Map<Tuple<TAZ, TAZ>, Double> targetODMatrix, int interval) {
		this.scenario = scenario;
		this.targetODMatrix = targetODMatrix;
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
		new File(this.odFile).delete();
		new File(this.locationsFile).delete();
		new File(this.departuresFile).delete();
	}

	@Override
	public void processState(MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> state) {

		if (this.iteration++ % this.interval == 0) {

			//
			this.append("" + state.getODReproductionError(), this.errorsFile);

			//
			final Map<Tuple<TAZ, TAZ>, Integer> realizedOdMatrix = state.getODView();
			new File(this.odFile).delete();
			try {
				PrintWriter writer = new PrintWriter(this.odFile);
				for (Map.Entry<Tuple<TAZ, TAZ>, Double> target : this.targetODMatrix.entrySet()) {
					writer.println(target.getKey().getA() + "\t" + target.getKey().getB() + "\t" + target.getValue()
							+ "\t" + realizedOdMatrix.getOrDefault(target.getKey(), 0));
				}
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}

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
