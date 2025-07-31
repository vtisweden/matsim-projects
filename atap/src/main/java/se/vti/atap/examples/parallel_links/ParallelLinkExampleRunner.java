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
package se.vti.atap.examples.parallel_links;

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

	public Scenario createMediumExample(String scenarioFolder, ATAPConfigGroup atapConfig) {
		double inflowDuration_s = 1800.0;

		ParallelLinkScenarioFactory factory = new ParallelLinkScenarioFactory();
		factory.setBottleneck(0, 500.0);
		factory.setBottleneck(1, 500.0);
		factory.setBottleneck(2, 500.0);
		factory.setBottleneck(3, 500.0);
		factory.setBottleneck(4, 500.0);
		factory.setOD(1000, inflowDuration_s, 0, 1, 2);
		factory.setOD(1000, inflowDuration_s, 1, 2, 3);
		factory.setOD(1000, inflowDuration_s, 2, 3, 4);

		Config config = factory.buildConfig();
		config.controller().setLastIteration(100); // TODO
		config.addModule(new EmulationConfigGroup());
		config.addModule(atapConfig);

		if (scenarioFolder != null) {
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

		Scenario scenario = factory.build(config);

		if (scenarioFolder != null) {
			NetworkUtils.writeNetwork(scenario.getNetwork(),
					Paths.get(scenarioFolder, config.network().getInputFile()).toString());
			PopulationUtils.writePopulation(scenario.getPopulation(),
					Paths.get(scenarioFolder, config.plans().getInputFile()).toString());
		}

		return scenario;
	}

	public Scenario createMediumExampleWithProposed(String scenarioFolder) {
		ATAPConfigGroup atapConfig = new ATAPConfigGroup();
		atapConfig.setReplannerIdentifier(ReplannerIdentifierType.UPPERBOUND_ATOMIC);
		atapConfig.setReplanningRateIterationExponent(-1.0);
		return this.createMediumExample(scenarioFolder, atapConfig);
	}

	public void runScenario(Scenario scenario) {
		ATAP atap = new ATAP();
		atap.meet(scenario.getConfig());
		Controler controler = new Controler(scenario);
		atap.meet(controler);
		controler.run();
	}

	public static void main(String[] args) {
		var example = new ParallelLinkExampleRunner();
		Scenario scenario = example.createMediumExampleWithProposed("./medium");
		example.runScenario(scenario);
	}

}
