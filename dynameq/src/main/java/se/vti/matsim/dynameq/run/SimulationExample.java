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
package se.vti.matsim.dynameq.run;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class SimulationExample {

	public static void main(String[] args) {

		final double populationDownsample = 0.01;

		Config config = ConfigUtils.loadConfig("dynameq_config.xml");
		config.qsim().setFlowCapFactor(populationDownsample);
		config.qsim().setStorageCapFactor(populationDownsample);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		PopulationUtils.sampleDown(scenario.getPopulation(), populationDownsample);

		Controler controler = new Controler(scenario);
		controler.run();
	}

}
