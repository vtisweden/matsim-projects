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

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import se.vti.tramodby.od.ZonalSystem.Zone;

class ZonalSystemTest {
	// Test utility variables.
	private static final Logger log = Logger.getLogger( ZonalSystemTest.class );
	
	// Setup variables.
	private static List<Id<Zone>> zones;
	private static List<Id<Link>> links;
	private static List<String> zoneNames;
	private ZonalSystem system;

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		log.info(String.format("Preparing tests for class: %s",ZonalSystem.class.getName()));
		Random rand = new Random();
		
		zones = IntStream.rangeClosed(0, rand.nextInt(23)+3)
						 .mapToObj(i -> Id.create(String.valueOf((char)(i + 65)),Zone.class))
						 .collect(Collectors.toList());
		
		links = IntStream.rangeClosed(0, zones.size()-1)
						 .mapToObj(i -> Id.createLinkId(i))
						 .collect(Collectors.toList());
		
		zoneNames = zones.stream()
						 .map(zone -> String.format("Zone %s",zone.toString()))
						 .collect(Collectors.toList());
		
		log.info(String.format("Generated test data size: %d (%d zones and %d links)", 
				zones.size(), zones.size(), links.size()));
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		log.info("Test complete");
	}

	@BeforeEach
	void setUp() throws Exception {
		system = new ZonalSystem();
		
	}

	private void addValues() {
		try {
			for(int test = 0 ; test < zones.size() ; test++) {
				system.add(links.get(test), zones.get(test),  zoneNames.get(test));
				
			}
		}
		catch(IllegalArgumentException e) {
			fail("Test failed to add unique zone names");
			e.printStackTrace();
		}
		
	}

	/**
	 *  Test method for {@link od.ZonalSystem#add(Id, Id, String)}.
	 */
	@Test
	@DisplayName(value="Test add")
	void testAdd() {
		
		// Adding one zone.
		system.add(links.get(0), zones.get(0),  zoneNames.get(0));
		
		/* Testing IllegalArgumentException */
		// Expected: exception
		Exception actual = Assertions.assertThrows(Exception.class, () -> {
			system.add(links.get(0), zones.get(0),  zoneNames.get(1));
		});
		
		assertEquals(IllegalArgumentException.class, actual.getClass());
	}

	/**
	 *  Test method for {@link od.ZonalSystem#getAllZoneIds()}.
	 */
	@Test
	@DisplayName(value="Test allZoneIdsView")
	void testAllZoneIdsView() {
		
		/* Testing empty zones. */
		// Expected: empty size
		int expectedSize = 0;
		int actualSize = system.getAllZones().keySet().size();
		assertEquals(expectedSize, actualSize);
		
		// Adding data.
		addValues();
		
		/* Testing with added zonal data. */
		// Expected: size of test set
		expectedSize = zones.size();
		actualSize = system.getAllZones().keySet().size();
		assertEquals(expectedSize, actualSize);
		
		/* Testing adding one extra link to a zone. */
		// Expected: size of test set
		system.add(links.get(0), zones.get(1), zoneNames.get(1));
		actualSize = system.getAllZones().keySet().size();
		assertEquals(expectedSize, actualSize);
		
		/* Testing the values */
		assertTrue(zones.containsAll(system.getAllZones().keySet()));
	}

	/**
	 *  Test method for {@link od.ZonalSystem#getAllZones()}.
	 */
	@Test
	@DisplayName(value="Test allZonesView")
	void testAllZonesView() {
		/* Testing empty zones. */
		// Expected: empty size
		int expectedSize = 0;
		int actualSize = system.getAllZones().size();
		assertEquals(expectedSize, actualSize);
		
		// Adding data.
		addValues();
		
		/* Testing with added zonal data. */
		// Expected: size of test set
		expectedSize = zones.size();
		actualSize = system.getAllZones().size();
		assertEquals(expectedSize, actualSize);
		
		/* Testing adding one extra link to a zone. */
		// Expected: size of test set
		system.add(links.get(0), zones.get(1), zoneNames.get(1));
		actualSize = system.getAllZones().size();
		assertEquals(expectedSize, actualSize);
		
		/* Testing the values */
		List<Id<Zone>> actualIds = system.getAllZones().values().stream()
														.map(Zone::getId)
														.collect(Collectors.toList());
		Set<Id<Link>> actualLinks = system.getAllZones().values().stream()
														 .map(Zone::getLinkIds)
														 .flatMap(Collection::stream)
														 .collect(Collectors.toSet());
		assertTrue(zones.containsAll(actualIds), "All zones ids");
		assertTrue(links.containsAll(actualLinks), "All link ids");

	}

	/**
	 *  Test method for {@link od.ZonalSystem#getZone(Id)}.
	 */
	@Test
	@DisplayName(value="Test getZone")
	void testGetZone() {
		// Adding data.
		addValues();
		
		for(int i = 0 ; i < zones.size() ; i++) {
			Id<Zone> expectedZoneId = zones.get(i);
			Set<Id<Link>> expectedLinkId = new HashSet<>();
			expectedLinkId.add(links.get(i));
			
			Zone zone = system.getAllZones().get(expectedZoneId);
			Id<Zone> actualZoneId = zone.getId();
			Set<Id<Link>> actualLinkId = zone.getLinkIds();
			
			assertEquals(expectedZoneId, actualZoneId);
			assertTrue(expectedLinkId.containsAll(actualLinkId));
		}
	}

	/**
	 *  Test method for {@link od.ZonalSystem#zoneCnt()}.
	 */
	@Test
	@DisplayName(value="Test zoneCnt")
	void testZoneCnt() {
		/* Testing empty zones. */
		// Expected: empty size
		int expectedSize = 0;
		int actualSize = system.zoneCnt();
		assertEquals(expectedSize, actualSize);
		
		// Adding data.
		addValues();
		
		/* Testing with added zonal data. */
		// Expected: size of test set
		expectedSize = zones.size();
		actualSize = system.zoneCnt();
		assertEquals(expectedSize, actualSize);
		
		/* Testing adding one extra link to a zone. */
		// Expected: size of test set
		system.add(links.get(0), zones.get(1), zoneNames.get(1));
		actualSize = system.zoneCnt();
		assertEquals(expectedSize, actualSize);
	}

	/**
	 *  Test method for {@link od.ZonalSystem#linkCnt()}.
	 */
	@Test
	@DisplayName(value="Test linkCnt")
	void testLinkCnt() {
		/* Testing empty zones. */
		// Expected: empty size
		int expectedSize = 0;
		int actualSize = system.linkCnt();
		assertEquals(expectedSize, actualSize);
		
		// Adding data.
		addValues();
		
		/* Testing with added zonal data. */
		// Expected: size of test set
		expectedSize = zones.size();
		actualSize = system.linkCnt();
		assertEquals(expectedSize, actualSize);
		
		/* Testing adding one extra link to a zone. */
		// Expected: size of test set
		system.add(links.get(0), zones.get(1), zoneNames.get(1));
		actualSize = system.linkCnt();
		assertEquals(expectedSize, actualSize);
		
		/* Testing adding one extra link to a zone. */
		// Expected: size of test set + 1
		system.add(Id.createLinkId(links.size()), zones.get(1), zoneNames.get(1));
		expectedSize = zones.size()+1;
		actualSize = system.linkCnt();
		assertEquals(expectedSize, actualSize);
	}

	/**
	 *  Test method for {@link od.ZonalSystem#consistent()}.
	 */
	@Test
	@DisplayName(value="Test consistent")
	void testConsistent() {
		assertTrue(system.consistent(), "Empty set");
		addValues();
		assertTrue(system.consistent(), "populated set");		
	}

}
