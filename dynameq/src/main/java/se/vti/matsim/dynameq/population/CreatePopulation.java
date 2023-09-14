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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationWriter;

import se.vti.matsim.dynameq.network.CreateNetwork;
import se.vti.matsim.dynameq.network.Utils;
import se.vti.matsim.dynameq.population.CentroidSystem.Centroid;
import se.vti.matsim.dynameq.population.ODMatrices.Matrix;
import se.vti.matsim.dynameq.utils.TimeDiscretization;
import se.vti.matsim.dynameq.utils.Units;

/**
 *
 * Directory and File Configuration Instructions:
 * ----------------------------------------------
 * 1. Adjust 'baseDataFolder' to correspond to your directory setup. It should align with the folder used in CreateNetwork.
 * 2. The 'dynameqMatrixFolder' should contain all matrix files you want to process. 
 *    Ensure matrix filenames in the lists match your matrix data.
 * 3. Modify 'modifiedDynaFolder', 'producedMatsimFilesFolder', and 'networkFile' to suit your directory structure and naming conventions.
 * 	  They should correspond the naming used in CreateNetwork.
 * 4. Confirm and adjust VOT and vehicle type definitions to match labels in your matrix data.
 * 5. Confirm that 'startHour_FM', 'startHour_EM', and 'binCnt' correspond to the time frames in your matrix data.
 * 6. The final translated population will be saved to 'produced_matsim_files/dynameq_population.xml'.
 * 
 * @author FilipK
 *
 */
public class CreatePopulation {

	private static final Logger log = Logger.getLogger(CreateNetwork.class);

	public static void main(String[] args) throws IOException {
		
		Path baseDataFolder = Paths.get("data");
		Path dynameqMatrixFolder = baseDataFolder.resolve("nuläge matriser");
		
		Path modifiedDynaFolder = baseDataFolder.resolve("modified_dynameq_files");
		Path producedMatsimFilesFolder = baseDataFolder.resolve("produced_matsim_files");
		Path networkFile = producedMatsimFilesFolder.resolve("dynameq_network.xml");
		
		// Split the matrix files based on FM and EM types.
		List<Path> matrixFilesFM = Arrays.asList(
			dynameqMatrixFolder.resolve("klass1-3_nulage_fm_matx_matx.dqt"),
			dynameqMatrixFolder.resolve("klass4-5_10-11_nulage_fm_matx_matx.dqt"),
			dynameqMatrixFolder.resolve("klass6-7_lbu_nulage_fm_matx_matx.dqt"),
			dynameqMatrixFolder.resolve("klass8-9_lbs_nulage_fm_matx_matx.dqt")
		);

		List<Path> matrixFilesEM = Arrays.asList(
			dynameqMatrixFolder.resolve("klass1-3_nulage_em_matx_matx.dqt"),
			dynameqMatrixFolder.resolve("klass4-5_10-11_nulage_em_matx_matx.dqt"),
		    dynameqMatrixFolder.resolve("klass6-7_lbu_nulage_em_matx_matx.dqt"),
		    dynameqMatrixFolder.resolve("klass8-9_lbs_nulage_em_matx_matx.dqt")
		);
		
		// Found in the matrix files. Value of time or vehicle type
		String vot_veh_type_1_3 = "Tidsvarde1till3";
		String vot_veh_type_4_5 = "Tidsvarde4till5";
		String vot_veh_type_lbu = "Lbu";
		String vot_veh_type_lbs = "Lbs";

		// Parameters related to matrix file.
		int startHour_FM = 6; // First hour in matrix-file
		int startHour_EM = 14;
		int binCnt = 4; // Number of hours in matrix-file
		double samplingRate = 1.0; // The sampling rate is multiplied with the OD value
		
		Path outputPopulationFile = producedMatsimFilesFolder.resolve("dynameq_population.xml");

		// TODO: Having to "fix" every file could be avoided by implementing a
		// customized TabularFileParser
		
		List<Path> fixedMatrixFilesFM = matrixFilesFM.stream()
		    .map(file -> modifiedDynaFolder.resolve(com.google.common.io.Files.getNameWithoutExtension(file.getFileName().toString()) + "_FIXED.txt"))
		    .collect(Collectors.toList());

		List<Path> fixedMatrixFilesEM = matrixFilesEM.stream()
		    .map(file -> modifiedDynaFolder.resolve(com.google.common.io.Files.getNameWithoutExtension(file.getFileName().toString()) + "_FIXED.txt"))
		    .collect(Collectors.toList());
		
		MatrixFileCleaner matrixFileCleaner = new MatrixFileCleaner();
		
		for (int i = 0; i < matrixFilesFM.size(); i++) {
		    matrixFileCleaner.removeRowsUntilAndIncluding(matrixFilesFM.get(i).toString(), fixedMatrixFilesFM.get(i).toString(), "SLICE");
		}

		for (int i = 0; i < matrixFilesEM.size(); i++) {
		    matrixFileCleaner.removeRowsUntilAndIncluding(matrixFilesEM.get(i).toString(), fixedMatrixFilesEM.get(i).toString(), "SLICE");
		}

		Network network = NetworkUtils.readNetwork(networkFile.toString());
		Population population = PopulationUtils.createPopulation(ConfigUtils.createConfig(), network);

		Set<Id<Node>> centroidNodes = new HashSet<>();
		for (Node node : network.getNodes().values()) {
			if (Utils.NodeTypeConstants.SUPER_CENTROID
					.equals(node.getAttributes().getAttribute(Utils.NODE_LINK_TYPE_ATTRIBUTE_KEY))) {
				centroidNodes.add(node.getId());
			}
		}
		CentroidSystem centroidSystem = new CentroidSystem(centroidNodes, network);

		log.info("Number of centroids: " + centroidSystem.centoridCnt());

		List<String> votVehTypes = Arrays.asList(vot_veh_type_1_3, vot_veh_type_4_5, vot_veh_type_lbu, vot_veh_type_lbs);

		// Loading FM data
		for (int i = 0; i < fixedMatrixFilesFM.size(); i++) {
		    loadMatricesAndFillPopulation(population, centroidSystem, startHour_FM, binCnt, samplingRate, votVehTypes.get(i), fixedMatrixFilesFM.get(i).toString());
		}

		// Loading EM data
		for (int i = 0; i < fixedMatrixFilesEM.size(); i++) {
		    loadMatricesAndFillPopulation(population, centroidSystem, startHour_EM, binCnt, samplingRate, votVehTypes.get(i), fixedMatrixFilesEM.get(i).toString());
		}

		new PopulationWriter(population).write(outputPopulationFile.toString());
	}

	// Note: A new ODMatrices object is created for every matrix file.
	private static void loadMatricesAndFillPopulation(Population population, CentroidSystem centroidSystem,
			int startHour, int binCnt, double samplingRate, String vot_veh_type, String matrixFile) throws IOException {
		TimeDiscretization timeDiscretization = new TimeDiscretization((int) (startHour * Units.S_PER_H),
				(int) Units.S_PER_H, binCnt);
		ODMatrices ods = new ODMatrices(timeDiscretization, matrixFile);

		for (int timeBin = 0; timeBin < binCnt; timeBin++) {
			ODMatrixUtils.loadTimeSlice(ods, timeBin, matrixFile, samplingRate);
		}
		fillPopulation(population, centroidSystem, ods, vot_veh_type);
	}

	private static void fillPopulation(Population population, CentroidSystem centroidSystem, ODMatrices ods,
			String vot_veh_type) {
		HashMap<Integer, Integer> departureCntPerHour = new HashMap<Integer, Integer>();
		HashMap<Integer, Double> matrixDemandPerHour = new HashMap<Integer, Double>();
		final Random rnd = new Random();
		PopulationFactory popFact = population.getFactory();
		for (Id<Centroid> fromCentroidId : centroidSystem.getAllCentroids().keySet()) {
			for (Id<Centroid> toCentroidId : centroidSystem.getAllCentroids().keySet()) {
				List<Double> dptTimes = ODMatrixUtils.sampleDeparturesFromPiecewiseConstantInterpolation(ods,
						fromCentroidId, toCentroidId, rnd);

				// Creating a plan and generating a person for each departure time.
				for (double dptTime : dptTimes) {
					Plan plan = popFact.createPlan();
					Id<Link> fromLink = centroidSystem.getAllCentroids().get(fromCentroidId).getFromLink();
					Activity originAct = popFact.createActivityFromLinkId("origin", fromLink);
					originAct.setEndTime(dptTime);
					plan.addActivity(originAct);
					plan.addLeg(popFact.createLeg("car"));
					Id<Link> toLink = centroidSystem.getAllCentroids().get(toCentroidId).getToLink();
					plan.addActivity(popFact.createActivityFromLinkId("destination", toLink));

					Id<Person> personId = Id.createPersonId(population.getPersons().size());
					Person person = popFact.createPerson(personId);
					person.addPlan(plan);
					population.addPerson(person);

					person.getAttributes().putAttribute("dynameqSourceMatrix", ods.getMatrixFile());
					person.getAttributes().putAttribute("value_of_time_veh_type", vot_veh_type);
					person.getAttributes().putAttribute("originLinkId", fromLink.toString());
					person.getAttributes().putAttribute("destinationLinkId", toLink.toString());

					// Keeping track of number of departures per hour.
					int depInHour = departureCntPerHour.getOrDefault(getHourFromSeconds(dptTime), 0);
					departureCntPerHour.put(getHourFromSeconds(dptTime), depInHour + 1);
				}

				// Calculating expected flow (OD-demand) per hour.
				int bin = 0;
				for (Matrix matrix : ods.getMatrixListView()) {
					int demandHour = getHourFromSeconds(ods.getTimeDiscretization().getBinStartTime_s(bin));
					Double val = matrix.get(fromCentroidId, toCentroidId);
					Double demandInHour = matrixDemandPerHour.getOrDefault(demandHour, 0.0);
					matrixDemandPerHour.put(demandHour, demandInHour + val);
					bin++;
				}
			}
		}

		log.info("Number of departures per hour in fillPopulation iteration" + departureCntPerHour.toString());
		log.info("Expected flow per hour in fillPopulation iteration: " + matrixDemandPerHour.toString());
		log.info("Population size: " + population.getPersons().size());
	}

	public static int getHourFromSeconds(double secondsAfterMidnight) {
		int hours = (int) (secondsAfterMidnight / 3600);
		return hours % 24;
	}
}
