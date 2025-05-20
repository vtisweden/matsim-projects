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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.matsim.api.core.v01.Id;

import se.vti.tramodby.od.ZonalSystem.Zone;
import se.vti.utils.misc.dynamicdata.TimeDiscretization;

/**
 * @author rasri17
 *
 */
class ODMatrixUtilsTest {
	// Test utility variables.
	private static final Logger log = LogManager.getLogger( ODMatrixUtilsTest.class );
	private static boolean skip = false;
	
	// Setup variables.
	private static Path[] odFiles;
	private static TimeDiscretization timeDiscretization;
	private InterZonalMatrices matrices;
	
	// Expected values.
	private static List<String> ODPairs; 
	private static List<List<Double>> expectedValues;
	private static double sampleRate;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		log.info(String.format("Preparing tests for class: %s",ODMatrixUtils.class.getName()));
		
		/* Creating utility objects */
		Random rand = new Random();
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(' '); 
		DecimalFormat df = new DecimalFormat("#.#####", otherSymbols);
		
		/* Setting scaling factor */
		sampleRate = Math.round(rand.nextDouble()*10)/10.0;
		
		/* Creating zones and OD pairs. */
		// The number of zones are randomized between 4 and 7 named A - H. 
		List<String> zones = IntStream.rangeClosed(0, rand.nextInt(23)+3)
				  .mapToObj(i -> String.valueOf((char)(i + 65)))
				  .collect(Collectors.toList());

		ODPairs = zones.stream()
					   .flatMap(str1 -> zones.stream()
					   .map(str2 -> str1 + "\t" + str2))
					   .collect(Collectors.toList());
		
		
		/* Generating test data temporary files */
		// The number of bins are randomized between 2 and 5.
		int binSize = 2 + rand.nextInt(22);
		odFiles = new Path[binSize];
		expectedValues = new ArrayList<>();
		for(int bin = 0 ; bin < binSize ; bin++) {
			
			// Creating the file.
			File odFile = null;
			PrintWriter writer = null;
			try {
				odFile = File.createTempFile(String.format("test_od_%d_",bin), ".txt");
				odFile.deleteOnExit();
			
				odFiles[bin] = Paths.get(odFile.toURI());
				
				// Generating flow
				List<Double> values = rand.doubles(ODPairs.size())
						  .parallel()
						  .map(value -> Math.round(value * 100))
						  .boxed()
						  .collect(Collectors.toList());
				
				// Storing expected flow.
				expectedValues.add(values.stream().map(value -> value * sampleRate).collect(Collectors.toList()));
				
				writer = new PrintWriter(odFile);
				for(int od = 0 ; od < ODPairs.size() ; od++) {
					writer.write(String.format("%s\t%s", ODPairs.get(od), df.format(values.get(od))));
					if(od < ODPairs.size()-1) {
						writer.write("\n");
					}
				}
			} 
			catch (IOException e) {
				log.fatal(String.format("Failed to create test files: %s", e.getLocalizedMessage()));
				skip = true;
				return;
			}
			finally {
				if(writer != null) {
					writer.close();
				}
			}
		}
		
		timeDiscretization = new TimeDiscretization(0, 3600, binSize);
		log.info(String.format("Generated test data size: %d (%d OD-pairs and %d time bins)", 
				ODPairs.size()*binSize, ODPairs.size(), binSize));
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void tearDownAfterClass() throws Exception {
		log.info("Test complete");
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		if(skip()) {return;}
		
		matrices = new InterZonalMatrices(timeDiscretization);
	}

	/**
	 * Skip checker method.
	 */
	private boolean skip() {
		return skip;
	}
	
	/**
	 * Test method for {@link od.ODMatrixUtils#loadTimeSlice(od.InterZonalMatrices, int, java.lang.String, double)}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test loadTimeSlice")
	void testLoadTimeSlice() {
		
		// Loading data.
		try {
			for(int i = 0 ; i < odFiles.length ; i++) {
			
				ODMatrixUtils.loadTimeSlice(matrices, i, odFiles[i].toString(), sampleRate);
				}
		} catch (IOException e) {
			fail(e.toString());
		}
		
		// Testing output.
		for(int bin = 0 ; bin < timeDiscretization.getBinCnt() ; bin++ ) {
			for(int od = 0 ; od < ODPairs.size() ; od++) {
				double expected = expectedValues.get(bin).get(od);
				String[] ODPair = ODPairs.get(od).split("\t");
				double actual = matrices.getMatrixListView()
										.get(bin)
										.get(Id.create(ODPair[0],Zone.class), 
											 Id.create(ODPair[1],Zone.class));
				
				assertEquals(expected, actual, 1E-4, "Testing loaded value");
			}
		}
	}

	/**
	 * Test method for {@link od.ODMatrixUtils#sampleDeparturesFromPiecewiseConstantInterpolation(od.InterZonalMatrices, org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, java.util.Random)}.
	 */
	@Test
	@Disabled
	void testSampleDeparturesFromPiecewiseConstantInterpolation() {
		
		fail("Not yet implementd.");
	}

}
