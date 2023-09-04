/**
 * se.vti.matsim.dynameq
 * 
 * Copyright (C) 2023 by Filip Kristofferson (VTI) and Gunnar Flötteröd (VTI, LiU).
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
package se.vti.matsim.dynameq.population;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParser;
import org.matsim.core.utils.io.tabularFileParser.TabularFileParserConfig;

import se.vti.matsim.dynameq.population.CentroidSystem.Centroid;
import se.vti.matsim.dynameq.population.ODMatrices.Matrix;
import se.vti.matsim.dynameq.utils.Time;
import se.vti.matsim.dynameq.utils.TimeDiscretization;
import se.vti.matsim.dynameq.utils.Units;

/**
 * This class contains utility methods for handling Origin-Destination (OD) flow
 * data.
 *
 * @author FilipK based on
 * @author Gunnar Flötteröd
 */

public class ODMatrixUtils {

	/**
	 * This method loads OD demand for a specific time bin from a file
	 * 
	 * @param odMatrix     - the OD-matrix
	 * @param timeBin      - the time bin
	 * @param matrixFile   - the file name of the input data
	 * @param samplingRate - the scaling factor for modeling a sample of the
	 *                     population
	 * 
	 * @throws IOException
	 */
	public static void loadTimeSlice(ODMatrices odMatrix, int timeBin, String matrixFile, double samplingRate)
			throws IOException {
		// Extracts the specific OD-matrix.
		final Matrix matrix = odMatrix.getMatrixListView().get(timeBin);

		TabularFileParserConfig tabularFileParserConfig = new TabularFileParserConfig();
		tabularFileParserConfig.setFileName(matrixFile);
		tabularFileParserConfig.setDelimiterRegex("\\s");

		// A time bin is defined by the hour it ends in the matrix file. This is why
		// hour + 1 is used
		int hour = (int) (Units.H_PER_S * odMatrix.getTimeDiscretization().getBinStartTime_s(timeBin));
		tabularFileParserConfig.setStartTag(Time.intToHourString(hour + 1));

		tabularFileParserConfig.setEndTag("SLICE");

		new TabularFileParser().parse(tabularFileParserConfig, row -> {
			Id<Centroid> fromCentroid = Id.create(row[0], Centroid.class);
			Id<Centroid> toCentroid = Id.create(row[1], Centroid.class);
			double value = Double.parseDouble(row[2]) * samplingRate;

			matrix.add(fromCentroid, toCentroid, value);
		});
	}

	/**
	 * This method gets a list of event times between a origin centroid and a
	 * destination centroid that are interpolated from the event rate
	 * 
	 * @param ods  - the OD-matrix
	 * @param orig - the origin centroid id
	 * @param dest - the destination centroid id
	 * @param rnd  - random generator
	 * @return list with event times
	 */
	public static List<Double> sampleDeparturesFromPiecewiseConstantInterpolation(ODMatrices ods, Id<Centroid> orig,
			Id<Centroid> dest, Random rnd) {
		final TimeDiscretization timeDiscr = ods.getTimeDiscretization();
		final List<ODMatrices.Matrix> matrices = ods.getMatrixListView();
		final ProcessSampler sampler = new ProcessSampler(rnd);

		// Adds the sample rate for each time bin.
		sampler.add(timeDiscr.getStartTime_s(), matrices.get(0).get(orig, dest) / timeDiscr.getBinSize_s());
		for (int bin = 0; bin < timeDiscr.getBinCnt() - 1; bin++) {
			sampler.add(timeDiscr.getBinEndTime_s(bin) - 1.0,
					matrices.get(bin).get(orig, dest) / timeDiscr.getBinSize_s());
			sampler.add(timeDiscr.getBinStartTime_s(bin + 1) + 1.0,
					matrices.get(bin + 1).get(orig, dest) / timeDiscr.getBinSize_s());
		}
		sampler.add(ods.getTimeDiscretization().getEndTime_s(),
				matrices.get(timeDiscr.getBinCnt() - 1).get(orig, dest) / timeDiscr.getBinSize_s());

		// Extracts draws a list of event times.
		return sampler.sample();
	}

}
