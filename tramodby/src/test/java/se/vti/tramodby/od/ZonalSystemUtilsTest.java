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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.config.Configurator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import se.vti.tramodby.module.TramodByConfigGroup;
import se.vti.tramodby.od.ZonalSystem.Zone;

class ZonalSystemUtilsTest {
	// Test utility variables.
	private static final Logger log = LogManager.getLogger( ZonalSystemUtilsTest.class );
	private static boolean skip = false;
	
	private static final String[] classList = {
			"org.matsim.core.network.NetworkImpl"
	};
	private static Level[] defaultLevels;
	
	// Setup variables.
	private static Network network;
	private static TramodByConfigGroup configGroup;
	private static List<Id<Zone>> zones;
	private static List<String> zoneNames;
	private static List<Id<Link>> links;
	
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		log.info(String.format("Preparing tests for class: %s",ZonalSystemUtils.class.getName()));
		// Disable logging.
		defaultLevels = new Level[classList.length];
		for(int i = 0 ; i < classList.length ; i++) {
			Logger currentLogger = LogManager.getLogger(classList[i]);
			defaultLevels[i] = currentLogger.getLevel();
			
			Configurator.setLevel(classList[i], Level.OFF);
		}
				
		Random rand = new Random();
		
		zones = IntStream.rangeClosed(0, rand.nextInt(23)+3)
				 .mapToObj(i -> Id.create(String.valueOf((char)(i + 65)),Zone.class))
				 .collect(Collectors.toList());
		
		zoneNames = zones.stream()
				 .map(zone -> String.format("Zone %s",zone.toString()))
				 .collect(Collectors.toList());
		
		links = new ArrayList<>();
		
		// Creating the file.
		File zoneFile = null;
		PrintWriter writer = null;
		Path zonePath;
		
		try {
			zoneFile = File.createTempFile("zones",".txt");
			zoneFile.deleteOnExit();
			zonePath = Path.of(zoneFile.toURI());
			writer = new PrintWriter(zoneFile);
			writer.write("linkID,bsu,bsuname\n");
			for(int zone = 0 ; zone < zones.size() ; zone++) {
				String currentZone = zones.get(zone).toString();
				List<Id<Link>> currentLinks = IntStream.rangeClosed(0, rand.nextInt(10)+1)
						 .mapToObj(i -> Id.createLinkId(String.format("%s_%d",currentZone, i)))
						 .collect(Collectors.toList());
				links.addAll(currentLinks);
				
				for(int link = 0 ; link < currentLinks.size() ; link++) {
					writer.write(String.format("%s,%s,%s",
							currentLinks.get(link),
							zones.get(zone).toString(),
							zoneNames.get(zone)));
					
					if(link < currentLinks.size()-1 || zone < zones.size()-1) {
						writer.write("\n");
					}
				}
				
			}
			
			configGroup = new TramodByConfigGroup();
			configGroup.setZoneDefinitionFile(zonePath.toString());
			
			buildNetwork();
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
		
		
		
		log.info(String.format("Generated test data size: %d (%d zones and %d links)", 
				zones.size()*links.size(), zones.size(), links.size()));
	}

	private static void buildNetwork() {
		
		  network = NetworkUtils.createNetwork();
		  HashMap<Integer,Node> nodes = new HashMap<>();
		  for (int i = 0; i <= links.size(); i++) {
			  nodes.put(i, NetworkUtils.createAndAddNode(network, Id.create(i, Node.class), new Coord(i,i)));
		  }
		  
		  for(int i = 0 ; i < links.size() ; i++) {
			  NetworkUtils.createAndAddLink(network,links.get(i), nodes.get(i), nodes.get(i+1), 1000.0, 10.0, 3600.0, (double) 1 );
		  }
	}
	
	/**
	 * Skip checker method.
	 */
	private boolean skip() {
		return skip;
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		if(skip()) {return;}
	}
	
	@AfterAll
	static void tearDownAfterClass() throws Exception {
		// Enabling logging.
		for(int i = 0 ; i < classList.length ; i++) {
			Configurator.setLevel(classList[i], defaultLevels[i]);
		}
		
		log.info("Test complete");
	}

	/**
	 *  Test method for {@link od.ODMatrixUtils#loadTimeSlice(od.InterZonalMatrices, int, java.lang.String, double)}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value="Test createZonalSystemFromFile")
	void testCreateZonalSystemFromFile() {
		
		try {
			ZonalSystem zonalSystem = ZonalSystemUtils.createZonalSystemFromFile(configGroup, network);
	
			assertTrue(zonalSystem.getAllZones().keySet().containsAll(zones), "Testing that all zones are loaded.");
			
			int expected = links.size();
			int actual = zonalSystem.linkCnt();
			assertEquals(expected, actual, "Testing that all links are loaded.");
		}
		catch(IOException e) {
			fail("Test failed to load ZonalSystem");
			e.printStackTrace();
		}
	}

	@Test
	@Disabled
	void testCreateLinkFromZoneSampler() {
		fail("Not yet implemented");
	}

	@Test
	@Disabled
	void testDrawLowVariance() {
		fail("Not yet implemented");
	}

}
