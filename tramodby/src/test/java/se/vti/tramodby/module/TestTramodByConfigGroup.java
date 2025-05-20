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
package se.vti.tramodby.module;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.DisplayName;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;

/**
 * @author rasri17
 *
 */
class TestTramodByConfigGroup {
	// Test utility variables
	private static final Logger log = LogManager.getLogger( TestTramodByConfigGroup.class );
	private static boolean skip = false;
	
	private static final Class<?>[] classList = {
			Config.class,
			org.matsim.core.config.ConfigReader.class,
			org.matsim.core.utils.io.MatsimXmlParser.class,
			org.hibernate.validator.internal.util.Version.class,
			org.matsim.core.utils.io.IOUtils.class
	};
	private static Level[] defaultLevels;
	
	// Setup variables
	private static Path configFile = null;
	private TramodByConfigGroup configGroup;
	
	// Expected values
	private static int binSize;
	private static int binCount;
	private static int startIndex;
	private static int startTime;
	private static double sampleRate;
	private static int sampleLinks;
	
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeAll
	static void setUpBeforeClass() throws Exception {
		log.info(String.format("Preparing tests for class: %s",TramodByConfigGroup.class.getName()));
		
		// Disable logging.
		defaultLevels = new Level[classList.length];
		for(int i = 0 ; i < classList.length ; i++) {
			Logger currentLogger = LogManager.getLogger(classList[i]);
			defaultLevels[i] = currentLogger.getLevel();
			
			Configurator.setLevel(classList[i], Level.OFF);
		}
		
		/* Creating utility objects */
		Random rand = new Random();
		DecimalFormatSymbols otherSymbols = new DecimalFormatSymbols(Locale.getDefault());
		otherSymbols.setDecimalSeparator('.');
		otherSymbols.setGroupingSeparator(' '); 
		DecimalFormat df = new DecimalFormat("#.#####", otherSymbols);
		
		/* Generating test data */
		// Randomizing a bin size of 15, 30 or 60 minutes.
		binSize = (int) (60*15*Math.pow(2,rand.nextInt(3)));
		// Randomizing a bin count.
		int maxBinCount = (24*60*60) / binSize;
		binCount = rand.nextInt(maxBinCount+1);
		
		// Randomizing a start index and generate a start time.
		startIndex = rand.nextInt(maxBinCount-binCount);
		startTime = startIndex*binSize;
		
		sampleRate = Math.round(rand.nextDouble()*10)/10.0;
		sampleLinks = rand.nextInt(10)+1;
		
		
		/* Creating temporary files */
		File tempFile = null;
		PrintWriter writer = null;
		try {
			tempFile = File.createTempFile("test_config_group", ".xml");
			tempFile.deleteOnExit();
			
			/* Creating XML input file */
			String xmlString =String.format( 
			"<?xml version=\"1.0\" ?>\r\n"
			+ "<!DOCTYPE config SYSTEM \"http://www.matsim.org/files/dtd/config_v2.dtd\">\r\n"
			+ "<config>\r\n"
			+ "	<module name=\"tramod_by\">\r\n"
			+ "		<param name=\"zoneDefinitionFile\" value=\"./zones.csv\" />\r\n"
			+ "		<param name=\"binCount\" value=\"%d\" />\r\n"
			+ "		<param name=\"startTime_s\" value=\"%d\" />\r\n"
			+ "		<param name=\"binSize_s\" value=\"%d\" />\r\n"
			+ "		<param name=\"odFileStartIndex\" value=\"%d\" />\r\n"
			+ "		<param name=\"odFilePrefix\" value=\"./od_time_\" />\r\n"
			+ "		<param name=\"costFilePrefix\" value=\"./cost_\" />\r\n"
			+ "		<param name=\"samplingRate\" value=\"%s\" />\r\n"
			+ "		<param name=\"sampledLinksPerZone\" value=\"%d\" />\r\n"
			+ "	</module>\r\n"
			+ "</config>",
			binCount,startTime, binSize, startIndex, df.format(sampleRate), sampleLinks);
			
			// Printing to file.
			writer = new PrintWriter(tempFile);
			writer.write(xmlString);
		} 
		catch (IOException e) {
			log.fatal(String.format("Failed to create test file: %s", e.getLocalizedMessage()));
			skip = true;
			return;
		}
		finally {
			if(writer != null) {
				writer.close();
			}
		}
		
		configFile = Paths.get(tempFile.toURI());
	
	}

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeEach
	void setUp() throws Exception {
		// Loads Tramod By config group for each test.
		if(skip()) {
			configGroup = new TramodByConfigGroup();
			return;
		}
		
		Config config = ConfigUtils.loadConfig(configFile.toString());

		configGroup = ConfigUtils.addOrGetModule(config, TramodByConfigGroup.class);
	}
	
	
	/**
	 * Skip checker method.
	 */
	private boolean skip() {
		return skip;
	}
	
	/**
	 * Test method for {@link module.TramodByConfigGroup#TramodByConfigGroup()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test module name")
	void testTramodByConfigGroup() {
		String expected = "tramod_by";
		
		TramodByConfigGroup configGroup = new TramodByConfigGroup();
		
		assertEquals(expected,configGroup.getName()); 
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getZoneDefinitionFile()}.
	 */
	
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getZoneDefinitionFile")
	void testGetZoneDefinitionFile() {
		String expected = "./zones.csv";
		
		String actual = configGroup.getZoneDefinitionFile();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setZoneDefinitionFile(java.lang.String)}.
	 */
	@Test
	@DisplayName(value = "Test setZoneDefinitionFile")
	void testSetZoneDefinitionFile() {
		String expected = "./new_zones.csv";
		
		configGroup.setZoneDefinitionFile(expected);
		String actual = configGroup.getZoneDefinitionFile();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getStartTime()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getStartTime")
	void testGetStartTime() {
		int expected = startTime;
		
		int actual = configGroup.getStartTime();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setStartTime(int)}.
	 */
	@Test
	@DisplayName(value = "Test setStartTime")
	void testSetStartTime() {
		int expected = 25200;
		
		configGroup.setStartTime(25200);
		int actual = configGroup.getStartTime();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getBinSize()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getBinSize")
	void testGetBinSize() {
		int expected = binSize;
		
		int actual = configGroup.getBinSize();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setBinSize(int)}.
	 */
	@Test
	@DisplayName(value = "Test setBinSize")
	void testSetBinSize() {
		int expected = 1800;
		
		configGroup.setBinSize(expected);
		int actual = configGroup.getBinSize();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getBinCount()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getBinCount")
	void testGetBinCount() {
		int expected = binCount;
		
		int actual = configGroup.getBinCount();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setBinCount(int)}.
	 */
	@Test
	@DisplayName(value = "Test setBinCount")
	void testSetBinCount() {
		int expected = 24;
		
		configGroup.setBinCount(expected);
		int actual = configGroup.getBinCount();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getOdFilePrefix()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getOdFilePrefix")
	void testGetOdFilePrefix() {
		String expected = "./od_time_";
		
		String actual = configGroup.getOdFilePrefix();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setOdFilePrefix(java.lang.String)}.
	 */
	@Test
	@DisplayName(value = "Test setOdFilePrefix")
	void testSetOdFilePrefix() {
		String expected = "./new_od_time_";
		
		configGroup.setOdFilePrefix(expected);
		String actual = configGroup.getOdFilePrefix();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getOdFileStartIndex()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getOdFileStartIndex")
	void testGetOdFileStartIndex() {
		int expected = startIndex;
		
		int actual = configGroup.getOdFileStartIndex();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setOdFileStartIndex(int)}.
	 */
	@Test
	@DisplayName(value = "Test setOdFileStartIndex")
	void testSetOdFileStartIndex() {
		int expected = 7;
		
		configGroup.setOdFileStartIndex(expected);
		int actual = configGroup.getOdFileStartIndex();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getOdFileName(int)}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getOdFileName")
	void testGetOdFileName() {
		String[] expected = new String[binSize];
		for(int bin = 0 ; bin < binSize ; bin++)
		{
			expected[bin] = String.format("./od_time_%d.txt",startIndex+bin);
		}
		
		// Checking multiple bins.
		for(int i = 0 ; i < expected.length ; i++) {
			String actual = configGroup.getOdFileName(i);
	
			assertEquals(expected[i], actual, String.format("Asserting iteration %d",i));
		}
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getCostFilePrefix()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getCostFilePrefix")
	void testGetCostFilePrefix() {
		String expected = "./cost_";
		
		String actual = configGroup.getCostFilePrefix();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setCostFilePrefix(java.lang.String)}.
	 */
	@Test
	@DisplayName(value = "Test setCostFilePrefix")
	void testSetCostFilePrefix() {
		String expected = "./new_cost_";
		
		configGroup.setCostFilePrefix(expected);
		String actual = configGroup.getCostFilePrefix();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getCostFileName(int)}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getCostFileName")
	void testGetCostFileName() {
		String[] expected = new String[binSize];
		for(int bin = 0 ; bin < binSize ; bin++)
		{
			expected[bin] = String.format("./cost_%d.txt",startIndex+bin);
		}
		
		// Checking multiple bins.
		for(int i = 0 ; i < expected.length ; i++) {
			String actual = configGroup.getCostFileName(i);
		
			assertEquals(expected[i], actual,String.format("Asserting iteration %d",i));
		}
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getSamplingRate()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getSamplingRate")
	void testGetSamplingRate() {
		double expected = sampleRate;
		
		double actual = configGroup.getSamplingRate();
		
		assertEquals(expected, actual, 1E-6);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setSamplingRate(double)}.
	 */
	@Test
	@DisplayName(value = "Test setSamplingRate")
	void testSetSamplingRate() {
		double expected = 0.5;
		
		configGroup.setSamplingRate(expected);
		double actual = configGroup.getSamplingRate();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#getSampledLinksPerZone()}.
	 */
	@Test
	@DisabledIf("skip")
	@DisplayName(value = "Test getSampledLinksPerZone")
	void testGetSampledLinksPerZone() {
		int expected = sampleLinks;
		
		int actual = configGroup.getSampledLinksPerZone();
		
		assertEquals(expected, actual);
	}

	/**
	 * Test method for {@link module.TramodByConfigGroup#setSampledLinksPerZone(int)}.
	 */
	@Test
	@DisplayName(value = "Test setSampledLinksPerZone")
	void testSetSampledLinksPerZone() {
		int expected = 2;
		
		configGroup.setSampledLinksPerZone(expected);
		int actual = configGroup.getSampledLinksPerZone();
		
		assertEquals(expected, actual);
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@AfterAll
	static void teardownAfterClass() throws Exception {
		// Enabling logging.
		for(int i = 0 ; i < classList.length ; i++) {
			Configurator.setLevel(classList[i], defaultLevels[i]);
		}
		
		log.info("Test complete");
	}
}
