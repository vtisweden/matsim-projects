/**
 * se.vti.samgods.consolidation.road
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
package se.vti.samgods.transportation.consolidation;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

import se.vti.samgods.ConsolidationUnit;
import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.fleet.FreightVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationCostModel {

	// -------------------- CONSTANTS --------------------

	private static final double minTransferredAmount_ton = 1.0;

	// -------------------- CONSTRUCTION --------------------

	public ConsolidationCostModel() {
	}

	// -------------------- IMPLEMENTATION --------------------

	public DetailedTransportCost computeSignatureCost(FreightVehicleAttributes vehicleAttrs, double payload_ton,
			ConsolidationUnit consolidationUnit, boolean loadAtStart, boolean unloadAtEnd,
			Map<Id<Link>, BasicTransportCost> link2unitCost, Set<Id<Link>> ferryLinks) throws InsufficientDataException {

		final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(payload_ton)
				.addLoadingDuration_h(0.0).addTransferDuration_h(0.0).addUnloadingDuration_h(0.0).addMoveDuration_h(0.0)
				.addLoadingCost(0.0).addTransferCost(0.0).addUnloadingCost(0.0).addMoveCost(0.0).addDistance_km(0.0);

		if (consolidationUnit.linkIds.size() > 0) {

			final List<Id<Link>> firstLinkIds = consolidationUnit.linkIds.get(0);
			final List<Id<Link>> lastLinkIds = consolidationUnit.linkIds.get(consolidationUnit.linkIds.size() - 1);

			for (List<Id<Link>> linkIds : consolidationUnit.linkIds) {

				if (linkIds.size() > 0) {

					if (loadAtStart && (linkIds == firstLinkIds)) {
						builder.addLoadingDuration_h(vehicleAttrs.loadTime_h.get(consolidationUnit.commodity));
						builder.addLoadingCost(vehicleAttrs.loadCost_1_ton.get(consolidationUnit.commodity)
								* Math.max(minTransferredAmount_ton, payload_ton));
					} else {
						builder.addTransferDuration_h(
								0.5 * vehicleAttrs.transferTime_h.get(consolidationUnit.commodity));
						builder.addTransferCost(0.5 * vehicleAttrs.transferCost_1_ton.get(consolidationUnit.commodity)
								* Math.max(minTransferredAmount_ton, payload_ton));
					}

					if (unloadAtEnd && (linkIds == lastLinkIds)) {
						builder.addUnloadingDuration_h(vehicleAttrs.loadTime_h.get(consolidationUnit.commodity));
						builder.addUnloadingCost(vehicleAttrs.loadCost_1_ton.get(consolidationUnit.commodity)
								* Math.max(minTransferredAmount_ton, payload_ton));
					} else {
						builder.addTransferDuration_h(
								0.5 * vehicleAttrs.transferTime_h.get(consolidationUnit.commodity));
						builder.addTransferCost(0.5 * vehicleAttrs.transferCost_1_ton.get(consolidationUnit.commodity)
								* Math.max(minTransferredAmount_ton, payload_ton));
					}

					for (Id<Link> linkId : linkIds) {
						BasicTransportCost unitCost = link2unitCost.get(linkId);
						builder.addMoveDuration_h(unitCost.duration_h);
						builder.addDistance_km(unitCost.length_km);
						if (ferryLinks.contains(linkId)) {
							builder.addMoveCost(unitCost.duration_h * vehicleAttrs.onFerryCost_1_h);
							builder.addMoveCost(unitCost.length_km * vehicleAttrs.onFerryCost_1_km);
						} else {
							builder.addMoveCost(unitCost.duration_h * vehicleAttrs.cost_1_h);
							builder.addMoveCost(unitCost.length_km * vehicleAttrs.cost_1_km);
						}
					}
				}
			}
		}

		return builder.build();
	}

}
