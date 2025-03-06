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
package se.vti.tramodby.runner;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.contrib.roadpricing.RoadPricingUtils;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;

import se.vti.utils.misc.dynamicdata.Time;

public class InputDataAnalyzer {

	public static void main(String[] args) {

		System.out.println("STARTED ...");

		Config config = ConfigUtils
				.loadConfig("C:\\Users\\GunnarF\\NoBackup\\data-workspace\\tramod-by_data-check\\config2.xml");

//		Config config = ConfigUtils.createConfig();
//		config.network().setInputFile("C:\\Users\\GunnarF\\Desktop\\2022-08-29\\output_network.xml.gz");
//		ConfigUtils.addOrGetModule(config, RoadPricingConfigGroup.class).setTollLinksFile("C:\\Users\\GunnarF\\Desktop\\2022-08-29\\output_toll.xml.gz");
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		RoadPricingSchemeImpl roadPricingScheme = RoadPricingUtils
				.loadRoadPricingSchemeAccordingToRoadPricingConfig(scenario);

		Set<Id<Link>> tolledLinksNotInNetwork = new LinkedHashSet<>();
		double earliestTollTimeInNetwork = Double.POSITIVE_INFINITY;
		double latestTollTimeInNetwork = Double.NEGATIVE_INFINITY;

		double maxTollAt9am = Double.NEGATIVE_INFINITY;
		double maxTollAt3pm = Double.NEGATIVE_INFINITY;
		
		Map<Id<Link>, List<RoadPricingSchemeImpl.Cost>> link2costs = roadPricingScheme.getTypicalCostsForLink();

		for (Map.Entry<Id<Link>, List<RoadPricingSchemeImpl.Cost>> entry : link2costs.entrySet()) {
			Id<Link> linkId = entry.getKey();
			RoadPricingSchemeImpl.Cost cost9am = roadPricingScheme.getLinkCostInfo(linkId, 9 * 3600, null, null);
			if (cost9am != null) {
				maxTollAt9am = Math.max(maxTollAt9am, cost9am.amount);
			}
			RoadPricingSchemeImpl.Cost cost3pm = roadPricingScheme.getLinkCostInfo(linkId, 15 * 3600, null, null);
			if (cost3pm != null) {
				maxTollAt3pm = Math.max(maxTollAt3pm, cost3pm.amount);
			}
			
			if (scenario.getNetwork().getLinks().containsKey(linkId)) {
				for (RoadPricingSchemeImpl.Cost cost : entry.getValue()) {
					earliestTollTimeInNetwork = Math.min(earliestTollTimeInNetwork, cost.startTime);
					latestTollTimeInNetwork = Math.max(latestTollTimeInNetwork, cost.endTime);
				}
			} else {
				tolledLinksNotInNetwork.add(linkId);
			}
		}
		System.out
				.println("Number of tolled links in network: " + (link2costs.size() - tolledLinksNotInNetwork.size()));
		System.out.println("Earliest toll time: " + Time.strFromSec((int) earliestTollTimeInNetwork));
		System.out.println("Latest toll time: " + Time.strFromSec((int) latestTollTimeInNetwork));
		System.out.println("Max. toll at 9am: " + maxTollAt9am);
		System.out.println("Max. toll at 3pm: " + maxTollAt3pm);
		System.out.println("Number of tolled links NOT in network: " + tolledLinksNotInNetwork.size());
		System.out.println("IDs of tolled links not in network: " + tolledLinksNotInNetwork);
		
		System.out.println("... DONE");
	}

}
