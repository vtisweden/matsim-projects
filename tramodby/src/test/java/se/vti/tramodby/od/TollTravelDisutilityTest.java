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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;

class TollTravelDisutilityTest {
	
	// Test utility variables.
	private static final Logger log = LogManager.getLogger( TollTravelDisutility.class );
	private static final String[] classList = {
			"org.matsim.core.config.Config",
			"org.matsim.core.network.NetworkImpl"
	};
	
	private static Level[] defaultLevels;
	
	// Setup variables.
	private static RoadPricingSchemeImpl scheme;
	private static Network network;
	private static ArrayList<Id<Link>> testLinks;
	private static ArrayList<Double> testTimes;
	
	// Expected values
	private static ArrayList<Double> expectedValues;

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
		
		// Creating network and RoadPricingScheme.
		Config config = ConfigUtils.createConfig();
		network = buildNetwork();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		scheme = (RoadPricingSchemeImpl) RoadPricingUtils.addOrGetRoadPricingScheme(scenario);
		
		// Creating test data.
		Random rand = new Random();
		testLinks = new ArrayList<>();
		testTimes = new ArrayList<>();
		expectedValues = new ArrayList<>();
		
		int testSize = rand.nextInt(10)+1;
		for(int i = 0 ; i < testSize ; i++) {
			// Randomizing link, cost and time.
			Id<Link> testLink = Id.createLinkId(rand.nextInt(network.getLinks().values().size())+1);
			double cost = rand.nextDouble()*10;
			int testTime =  (600*i) + rand.nextInt(600);
			
			// Adding cost data.
			RoadPricingUtils.addLinkSpecificCost(scheme, testLink, (double)600*i, (double)600*(i+1), cost);
			
			testLinks.add(testLink);
			testTimes.add(Double.valueOf(testTime));
			expectedValues.add(cost);
		}
		
	}

	private static Network buildNetwork() {
		/*
			    B           E
			    |	        ^
		       [2]         [6]
			    |	        |
			    V           |
		A-[1]-> C <-[4/5]-> F
			    |	        ^
		       [3]         [7]
		        |           |
		        V           |
			    D           G
		*/
		
		  Network network = NetworkUtils.createNetwork();
		  HashMap<String,Node> nodes = new HashMap<>();
		  for (int i = 0; i <= 6; i++) {
			  String id = String.valueOf((char)(i + 65));
			  nodes.put(id, NetworkUtils.createAndAddNode(network, Id.create(id, Node.class), new Coord(i,i)));
		  }
		  
		  
		  NetworkUtils.createAndAddLink(network,Id.create(1, Link.class), nodes.get("A"), nodes.get("C"), 1000.0, 10.0, 3600.0, (double) 1 );
		  NetworkUtils.createAndAddLink(network,Id.create(2, Link.class), nodes.get("B"), nodes.get("C"), 1000.0, 10.0, 3600.0, (double) 1 );
		  NetworkUtils.createAndAddLink(network,Id.create(3, Link.class), nodes.get("C"), nodes.get("D"), 1000.0, 10.0, 3600.0, (double) 1 );
		  NetworkUtils.createAndAddLink(network,Id.create(4, Link.class), nodes.get("C"), nodes.get("F"), 1000.0, 10.0, 3600.0, (double) 1 );
		  NetworkUtils.createAndAddLink(network,Id.create(5, Link.class), nodes.get("F"), nodes.get("C"), 1000.0, 10.0, 3600.0, (double) 1 );
		  NetworkUtils.createAndAddLink(network,Id.create(6, Link.class), nodes.get("F"), nodes.get("E"), 1000.0, 10.0, 3600.0, (double) 1 );
		  NetworkUtils.createAndAddLink(network,Id.create(7, Link.class), nodes.get("G"), nodes.get("F"), 1000.0, 10.0, 3600.0, (double) 1 );
		  
		  
		  return network;
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
		// Enabling logging.
		for(int i = 0 ; i < classList.length ; i++) {
			Configurator.setLevel(classList[i], defaultLevels[i]);
		}
				
		log.info("Test complete");
	}

	@BeforeEach
	void setUp() throws Exception {
	}

	/**
	 *  Test method for {@link od.TollTravelDisutility#TollTravelDisutility(org.matsim.contrib.roadpricing.RoadPricingScheme)}.
	 */
	@Test
	@DisplayName(value="Test TollTravelDisutility")
	void testTollTravelDisutility() {
		
		TollTravelDisutility tollTravelDisutility = new TollTravelDisutility(scheme);
		
		assertNotNull(tollTravelDisutility);
	}

	/**
	 *  Test method for {@link od.TollTravelDisutility#getLinkTravelDisutility(Link, double, org.matsim.api.core.v01.population.Person, org.matsim.vehicles.Vehicle)}.
	 */
	@Test
	@DisplayName(value="Test getLinkTravelDisutility")
	void testGetLinkTravelDisutility() {
		TollTravelDisutility tollTravelDisutility = new TollTravelDisutility(scheme);
		
		// First round.
		for(int test = 0 ; test < expectedValues.size() ; test++) {
			double expected = expectedValues.get(test);
			double actual = tollTravelDisutility.getLinkTravelDisutility(network.getLinks()
																				.get(testLinks.get(test)), 
																				testTimes.get(test), 
																				null, 
																				null);
			assertEquals(expected, actual, 1E-6);
		}
		
		// Second round.
		double expected = 0;
		double actual = tollTravelDisutility.getLinkTravelDisutility(network.getLinks()
				.get(testLinks.get(testLinks.size()-1)), 
				testTimes.get(testTimes.size()-1)+600, 
				null, 
				null);
		
		assertEquals(expected, actual, 1E-6);
	}

	/**
	 *  Test method for {@link od.TollTravelDisutility#getLinkMinimumTravelDisutility(Link)}.
	 */
	@Test
	@DisplayName(value="Test getLinkMinimumTravelDisutility")
	void testGetLinkMinimumTravelDisutility() {
		TollTravelDisutility tollTravelDisutility = new TollTravelDisutility(scheme);
		
		double expected = 0;
		double actual = tollTravelDisutility.getLinkMinimumTravelDisutility(network.getLinks()
				.get(testLinks.get(0)));
		
		assertEquals(expected, actual, 1E-6);
	}

}
