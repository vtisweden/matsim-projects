/**
 * se.vti.samgods.transportation.costs
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.transportation.costs;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class RealizedInVehicleCost {

	public RealizedInVehicleCost() {
	}

	public DetailedTransportCost compute(SamgodsVehicleAttributes vehicleAttrs, double payload_ton,
			ConsolidationUnit consolidationUnit, Map<Id<Link>, BasicTransportCost> link2unitCost,
			Set<Id<Link>> ferryLinks, Map<Id<Link>, Double> linkId2weight) throws InsufficientDataException {
		final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().setToAllZeros()
				.addAmount_ton(payload_ton);
		if (consolidationUnit.linkIds.size() > 0) {
			for (List<Id<Link>> linkIds : consolidationUnit.linkIds) {
				for (Id<Link> linkId : linkIds) {
					final double weight = linkId2weight == null ? 1.0
							: Math.max(1.0, linkId2weight.getOrDefault(linkId, 0.0));
					BasicTransportCost unitCost = link2unitCost.get(linkId);
					builder.addMoveDuration_h(weight * unitCost.duration_h);
					builder.addDistance_km(weight * unitCost.length_km);
					if (ferryLinks.contains(linkId)) {
						builder.addMoveCost(weight * unitCost.duration_h * vehicleAttrs.onFerryCost_1_h);
						builder.addMoveCost(weight * unitCost.length_km * vehicleAttrs.onFerryCost_1_km);
					} else {
						builder.addMoveCost(weight * unitCost.duration_h * vehicleAttrs.cost_1_h);
						builder.addMoveCost(weight * unitCost.length_km * vehicleAttrs.cost_1_km);
					}
				}
			}
		}
		return builder.build();
	}

	public DetailedTransportCost compute(SamgodsVehicleAttributes vehicleAttrs, double payload_ton,
			ConsolidationUnit consolidationUnit, Map<Id<Link>, BasicTransportCost> link2unitCost,
			Set<Id<Link>> ferryLinks) throws InsufficientDataException {
		return this.compute(vehicleAttrs, payload_ton, consolidationUnit, link2unitCost, ferryLinks, null);
	}
}
