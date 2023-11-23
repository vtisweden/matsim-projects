/**
 * se.vti.tramodby
 * 
 * Copyright (C) 2023 Gunnar Flötteröd (VTI, LiU), Rasmus Ringdahl (LiU). 
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
package se.vti.tramodby.od;

import java.io.IOException;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.Id;

import floetteroed.utilities.TimeDiscretization;
import floetteroed.utilities.tabularfileparser.TabularFileHandler;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.tramodby.od.InterZonalMatrices.Matrix;
import se.vti.tramodby.od.ZonalSystem.Zone;

/**
 * This class contains utility methods for handling Origin-Destination (OD) flow data. 
 * 
 */
public class ODMatrixUtils {
	
	private ODMatrixUtils() {
		throw new IllegalStateException("Utility class");
	}

	/**
	 * This method loads OD demand for a specific time bin from a file.
	 * 
	 * @param odMatrix - the OD-matrix
	 * @param timeBin - the time bin
	 * @param matrixFile - the file name of the input data
	 * @param samplingRate - the scaling factor for modeling a sample of the population
	 * 
	 * @throws IOException
	 */
	public static void loadTimeSlice(InterZonalMatrices odMatrix, int timeBin, String matrixFile, double samplingRate) throws IOException {
		// Extracts the specific OD-matrix.
		final Matrix matrix = odMatrix.getMatrixListView().get(timeBin);
		
		// Creates a handler for parsing the OD demand file.
		final TabularFileHandler handler = new TabularFileHandler() {
			@Override
			public void startDocument() {
				// Do not handle start of the document.
			}

			@Override
			public void endDocument() {
				// Do not handle start of the document.s
			}

			@Override
			public String preprocess(String line) {
				return line;
			}

			@Override
			public void startRow(String[] row) {
				matrix.put(Id.create(row[0], Zone.class), Id.create(row[1], Zone.class), samplingRate * Double.parseDouble(row[2]));
			}
		};
		// Setting parser parameters.
		final TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterRegex("\\s");
		parser.setOmitEmptyColumns(false);
		
		// Parses the OD demand file.
		parser.parse(matrixFile, handler);
	}

	/**
	 * This method gets a list of event times between a origin zone and a destination zone
	 * that are interpolated from the event rate.
	 * 
	 * @param ods - the OD-matrix
	 * @param orig - the origin zone id
	 * @param dest - the destination zone id
	 * @param rnd - random generator
	 * @return list with event times
	 */
	public static List<Double> sampleDeparturesFromPiecewiseConstantInterpolation(InterZonalMatrices ods, Id<Zone> orig,
			Id<Zone> dest, Random rnd) {
		final TimeDiscretization timeDiscr = ods.getTimeDiscretization();
		final List<InterZonalMatrices.Matrix> matrices = ods.getMatrixListView();
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
