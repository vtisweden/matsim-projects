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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;

import se.vti.tramodby.module.TramodByConfigGroup;
import se.vti.tramodby.od.InterZonalMatrices.Matrix;
import se.vti.tramodby.od.ZonalSystem.Zone;
import se.vti.utils.misc.dynamicdata.TimeDiscretization;

/**
 * @author rasri17
 *
 */
class TestInterZonalMatrices {
	// Setup variables
	private static TimeDiscretization timeDiscretization;
	private InterZonalMatrices matrices;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		timeDiscretization = new TimeDiscretization(0, 3600, 24);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		matrices = new InterZonalMatrices(timeDiscretization);
	}

	/**
	 * Test method for {@link od.InterZonalMatrices#InterZonalMatrices(floetteroed.utilities.TimeDiscretization)}.
	 */
	@Test
	@DisplayName(value="Test ZonalMatricesTimeDiscretization")
	void testInterZonalMatricesTimeDiscretization() {
		TimeDiscretization timeDiscretization = new TimeDiscretization(0, 3600, 24);
		
		InterZonalMatrices matrices = new InterZonalMatrices(timeDiscretization);
		
		int expected = 24;
		int actual = matrices.getMatrixListView().size();
		
		assertEquals(expected, actual, "Test correct number of time slices");
		
	}

	/**
	 * Test method for {@link od.InterZonalMatrices#InterZonalMatrices(module.TramodByConfigGroup)}.
	 */
	@Test
	@DisplayName(value="Test ZonalMatricesTimeDiscretization")
	void testInterZonalMatricesTramodByConfigGroup() {
		TramodByConfigGroup configGroup = new TramodByConfigGroup();
		configGroup.setStartTime(0);
		configGroup.setBinSize(3600);
		configGroup.setBinCount(3);
		
		InterZonalMatrices matrices = new InterZonalMatrices(configGroup);
		
		int expected = 3;
		int actual = matrices.getMatrixListView().size();
		
		assertEquals(expected, actual, "Test correct number of time slizes");
	}

	/**
	 * Test method for {@link od.InterZonalMatrices#getTimeDiscretization()}.
	 */
	@Test
	@DisplayName(value="Test getTimeDiscretization")
	void testGetTimeDiscretization() {
		
		assertEquals(timeDiscretization, 
					 matrices.getTimeDiscretization(),
					 "Test correct TimeDiscretization object");
	}

	/**
	 * Test method for {@link od.InterZonalMatrices#getMatrixListView()}.
	 */
	@Test
	@DisplayName(value="Test getMatrixListView")
	void testGetMatrixListView() {		
		
		int expectedLength = 24;
		
		List<Matrix> internalMatrices = matrices.getMatrixListView(); 
		
		assertEquals(expectedLength,internalMatrices.size(),
					 "Test the length on MatrixListView.");
		for(int i = 0 ; i < internalMatrices.size() ; i++) {
			assertNotNull(internalMatrices.get(i),
						  "Test that Matrix object is added");
		}
	}

	/**
	 * Test method for {@link od.InterZonalMatrices#addSynchronized(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id, int, double)}.
	 */
	@Test
	@DisplayName(value="Test addSynchronized")
	void testAddSynchronized() {
		Id<Zone> origing = Id.create("origin", Zone.class);
		Id<Zone> destination = Id.create("destination", Zone.class);
		
		// First test round.
		double expected = 0;
		double actual = matrices.getMatrixListView().get(0).get(origing, destination);
		assertEquals(expected, actual, 1E-6, "Testing OD with zero demand.");
		
		// Second test round.
		int[] bin = {0,1,2};
		double[] values = {5.0, 10.0, 15.0};
		double[] expectedValues = {5.0, 10.0, 15.0};
		
		arrayAddTesting(origing, destination, bin, values, expectedValues);
		
		
		// Third test round.
		values = new double[] {-5.0, 0.0, 5.0};
		expectedValues = new double[] {0, 10.0, 20.0};
		arrayAddTesting(origing, destination, bin, values, expectedValues);
		
	}
	
	void arrayAddTesting(Id<Zone> origing, Id<Zone> destination,
						 int[] bin, double[] valuesToAdd, double[] expectedValues) {
		
		IntStream.range(0, bin.length).parallel().forEach(i ->{
			matrices.addSynchronized(origing, destination, bin[i], valuesToAdd[i]);
		});
		
		for(int i = 0; i < bin.length ; i++) {
			double expected = expectedValues[i];
			
			double actual = matrices.getMatrixListView().get(bin[i]).get(origing, destination);
			assertEquals(expected, actual, 1E-6, "Testing adding demand.");
		}
	}

}
