/**
 * se.vti.atap.examples.parallel_links
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.atap.examples.matsim.parallel_links;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.atap.ATAP;
import org.matsim.contrib.atap.ATAPConfigGroup;
import org.matsim.contrib.atap.ATAPConfigGroup.ReplannerIdentifierType;
import org.matsim.contrib.emulation.EmulationConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class ParallelLinkExampleRunner {

	public ParallelLinkExampleRunner() {
	}

	private void writeConfig(Config config, String scenarioFolder) {
		File folder = new File(scenarioFolder);
		if (folder.exists()) {
			try {
				FileUtils.cleanDirectory(folder);
			} catch (IOException e) {
				throw new RuntimeException();
			}
		} else {
			folder.mkdirs();
		}
		config.network().setInputFile("network.xml");
		config.plans().setInputFile("population.xml");
		ConfigUtils.writeMinimalConfig(config, Paths.get(scenarioFolder, "config.xml").toString());
	}

	private void writeScenario(Scenario scenario, String scenarioFolder) {
		NetworkUtils.writeNetwork(scenario.getNetwork(),
				Paths.get(scenarioFolder, scenario.getConfig().network().getInputFile()).toString());
		PopulationUtils.writePopulation(scenario.getPopulation(),
				Paths.get(scenarioFolder, scenario.getConfig().plans().getInputFile()).toString());
	}

	public Scenario createSmallExample(String scenarioFolder, ATAPConfigGroup atapConfig) {
		double sizeFactor = 3.0;
		double inflowDuration_s = 900.0;

		ParallelLinkScenarioFactory factory = new ParallelLinkScenarioFactory(inflowDuration_s, sizeFactor);
		factory.setBottleneck(0, 500.0);
		factory.setBottleneck(1, 500.0);
		factory.setOD(2000, 0, 1);

		Config config = factory.buildConfig();
		config.controller().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controller().setLastIteration(100);
		config.travelTimeCalculator().setTraveltimeBinSize(60);
		config.qsim().setStuckTime(Double.POSITIVE_INFINITY);
		// default: config.qsim().setInsertingWaitingVehiclesBeforeDrivingVehicles(true);
		config.addModule(new EmulationConfigGroup());
		config.addModule(atapConfig);
		if (scenarioFolder != null) {
			this.writeConfig(config, scenarioFolder);
		}

		Scenario scenario = factory.build(config);
		if (scenarioFolder != null) {
			this.writeScenario(scenario, scenarioFolder);
		}

		return scenario;
	}

	public Scenario createSmallExampleWithUniform(String scenarioFolder) {
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.IID);
		atapConfig.setReplanningRateIterationExponent(-1.0);
		return this.createSmallExample(scenarioFolder, atapConfig);
	}

	public Scenario createSmallExampleWithSorting(String scenarioFolder) {
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.SBAYTI2007);
		atapConfig.setReplanningRateIterationExponent(-0.5);
		atapConfig.setMaxMemory(4);
		atapConfig.setKernelHalftime_s(300);
		atapConfig.setShuffleBeforeReplannerSelection(true);
		return this.createSmallExample(scenarioFolder, atapConfig);
	}

	public Scenario createSmallExampleWithProposed(String scenarioFolder) {
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.UPPERBOUND_ATOMIC);
		atapConfig.setReplanningRateIterationExponent(-0.5);
		atapConfig.setMaxMemory(4);
		atapConfig.setKernelHalftime_s(60);
		atapConfig.setShuffleBeforeReplannerSelection(true);
		atapConfig.setUseQuadraticDistance(true);
		return this.createSmallExample(scenarioFolder, atapConfig);
	}

	public void runScenario(Scenario scenario) {
		var atap = new ATAP();
		atap.meet(scenario.getConfig());
		var controler = new Controler(scenario);
		var flowListener = new CumulativeFlowListener();
		controler.addControlerListener(flowListener);
		controler.getEvents().addHandler(flowListener);
		atap.meet(controler);
		controler.run();
	}

	public static void main(String[] args) {
		var example = new ParallelLinkExampleRunner();
		Scenario scenario = example.createSmallExampleWithProposed("./small");
		example.runScenario(scenario);
	}

}
