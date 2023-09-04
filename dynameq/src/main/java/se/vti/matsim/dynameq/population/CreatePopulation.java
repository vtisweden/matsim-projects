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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

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
 * @author FilipK
 *
 */
public class CreatePopulation {

	private static final Logger log = Logger.getLogger(CreateNetwork.class);

	public static void main(String[] args) throws IOException {

		// TODO: Having to "fix" every file could be avoided by implementing a
		// customized TabularFileParser
		final String MATRIX_FILE_1_3_EM = "data/dynameq_files/nuläge matriser/klass1-3_nulage_em_matx_matx.dqt";
		final String MATRIX_FILE_1_3_FM = "data/dynameq_files/nuläge matriser/klass1-3_nulage_fm_matx_matx.dqt";
		final String MATRIX_FILE_4_5_EM = "data/dynameq_files/nuläge matriser/klass4-5_10-11_nulage_em_matx_matx.dqt";
		final String MATRIX_FILE_4_5_FM = "data/dynameq_files/nuläge matriser/klass4-5_10-11_nulage_fm_matx_matx.dqt";
		final String MATRIX_FILE_6_7_LBU_EM = "data/dynameq_files/nuläge matriser/klass6-7_lbu_nulage_em_matx_matx.dqt";
		final String MATRIX_FILE_6_7_LBU_FM = "data/dynameq_files/nuläge matriser/klass6-7_lbu_nulage_fm_matx_matx.dqt";
		final String MATRIX_FILE_8_9_LBS_EM = "data/dynameq_files/nuläge matriser/klass8-9_lbs_nulage_em_matx_matx.dqt";
		final String MATRIX_FILE_8_9_LBS_FM = "data/dynameq_files/nuläge matriser/klass8-9_lbs_nulage_fm_matx_matx.dqt";

		final String MATRIX_FILE_1_3_EM_FIXED = "data/modified_dynameq_files/klass1-3_nulage_em_matx_matx_FIXED.txt";
		final String MATRIX_FILE_1_3_FM_FIXED = "data/modified_dynameq_files/klass1-3_nulage_fm_matx_matx_FIXED.txt";
		final String MATRIX_FILE_4_5_EM_FIXED = "data/modified_dynameq_files/klass4-5_10-11_nulage_em_matx_matx_FIXED.txt";
		final String MATRIX_FILE_4_5_FM_FIXED = "data/modified_dynameq_files/klass4-5_10-11_nulage_fm_matx_matx_FIXED.txt";
		final String MATRIX_FILE_6_7_LBU_EM_FIXED = "data/modified_dynameq_files/klass6-7_lbu_nulage_em_matx_matx_FIXED.txt";
		final String MATRIX_FILE_6_7_LBU_FM_FIXED = "data/modified_dynameq_files/klass6-7_lbu_nulage_fm_matx_matx_FIXED.txt";
		final String MATRIX_FILE_8_9_LBS_EM_FIXED = "data/modified_dynameq_files/klass8-9_lbs_nulage_em_matx_matx_FIXED.txt";
		final String MATRIX_FILE_8_9_LBS_FM_FIXED = "data/modified_dynameq_files/klass8-9_lbs_nulage_fm_matx_matx_FIXED.txt";

		final String NETWORK_FILE = "data/produced_matsim_files/dynameq_network.xml";

		MatrixFileCleaner matrixFileCleaner = new MatrixFileCleaner();
		matrixFileCleaner.removeRowsUntilAndIncluding(MATRIX_FILE_1_3_EM, MATRIX_FILE_1_3_EM_FIXED, "SLICE");
		matrixFileCleaner.removeRowsUntilAndIncluding(MATRIX_FILE_1_3_FM, MATRIX_FILE_1_3_FM_FIXED, "SLICE");
		matrixFileCleaner.removeRowsUntilAndIncluding(MATRIX_FILE_4_5_EM, MATRIX_FILE_4_5_EM_FIXED, "SLICE");
		matrixFileCleaner.removeRowsUntilAndIncluding(MATRIX_FILE_4_5_FM, MATRIX_FILE_4_5_FM_FIXED, "SLICE");
		matrixFileCleaner.removeRowsUntilAndIncluding(MATRIX_FILE_6_7_LBU_EM, MATRIX_FILE_6_7_LBU_EM_FIXED, "SLICE");
		matrixFileCleaner.removeRowsUntilAndIncluding(MATRIX_FILE_6_7_LBU_FM, MATRIX_FILE_6_7_LBU_FM_FIXED, "SLICE");
		matrixFileCleaner.removeRowsUntilAndIncluding(MATRIX_FILE_8_9_LBS_EM, MATRIX_FILE_8_9_LBS_EM_FIXED, "SLICE");
		matrixFileCleaner.removeRowsUntilAndIncluding(MATRIX_FILE_8_9_LBS_FM, MATRIX_FILE_8_9_LBS_FM_FIXED, "SLICE");

		Network network = NetworkUtils.readNetwork(NETWORK_FILE);
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

		// Loading FM data
		loadMatricesAndFillPopulation(population, centroidSystem, startHour_FM, binCnt, samplingRate, vot_veh_type_1_3,
				MATRIX_FILE_1_3_FM_FIXED);
		loadMatricesAndFillPopulation(population, centroidSystem, startHour_FM, binCnt, samplingRate, vot_veh_type_4_5,
				MATRIX_FILE_4_5_FM_FIXED);
		loadMatricesAndFillPopulation(population, centroidSystem, startHour_FM, binCnt, samplingRate, vot_veh_type_lbu,
				MATRIX_FILE_6_7_LBU_FM_FIXED);
		loadMatricesAndFillPopulation(population, centroidSystem, startHour_FM, binCnt, samplingRate, vot_veh_type_lbs,
				MATRIX_FILE_8_9_LBS_FM_FIXED);
		
		// Loading EM data
		loadMatricesAndFillPopulation(population, centroidSystem, startHour_EM, binCnt, samplingRate, vot_veh_type_1_3,
				MATRIX_FILE_1_3_EM_FIXED);
		loadMatricesAndFillPopulation(population, centroidSystem, startHour_EM, binCnt, samplingRate, vot_veh_type_4_5,
				MATRIX_FILE_4_5_EM_FIXED);
		loadMatricesAndFillPopulation(population, centroidSystem, startHour_EM, binCnt, samplingRate, vot_veh_type_lbu,
				MATRIX_FILE_6_7_LBU_EM_FIXED);
		loadMatricesAndFillPopulation(population, centroidSystem, startHour_EM, binCnt, samplingRate, vot_veh_type_lbs,
				MATRIX_FILE_8_9_LBS_EM_FIXED);

		String outputPopulationFile = "data/produced_matsim_files/dynameq_population_FM_EM.xml";
		new PopulationWriter(population).write(outputPopulationFile);
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
