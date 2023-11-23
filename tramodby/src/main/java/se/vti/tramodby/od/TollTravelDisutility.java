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

import org.jboss.logging.Logger;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.vehicles.Vehicle;

public class TollTravelDisutility implements TravelDisutility {

	private final boolean logTravelDisutilities = false;
	
	private RoadPricingScheme roadPricingScheme;
	
	public TollTravelDisutility(RoadPricingScheme roadPricingScheme) {
		this.roadPricingScheme = roadPricingScheme;
		
	}
	
	@Override
	public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
		final RoadPricingSchemeImpl.Cost cost = roadPricingScheme.getLinkCostInfo(link.getId(), time, 
				person != null ? person.getId() : null,
				vehicle != null ? vehicle.getId() : null);
		if (this.logTravelDisutilities) {
			if (this.roadPricingScheme.getTolledLinkIds().contains(link.getId())) {
				Logger.getLogger(this.getClass()).info("Requesting toll disutility for link " + link.getId() 
				+ " at time " + time + ". Result: " + (cost == null ? "null" : cost.toString()));				
			}			
		}		
		return (cost != null ? cost.amount : 0.0);
	}
	
	@Override
	public double getLinkMinimumTravelDisutility(Link link) {
		return 0.0;
	}

}
