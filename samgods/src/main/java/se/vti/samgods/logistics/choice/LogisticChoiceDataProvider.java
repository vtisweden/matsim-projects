/**
 * se.vti.samgods.logistics.choicemodel
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
package se.vti.samgods.logistics.choice;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.matsim.vehicles.VehicleType;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkData;
import se.vti.samgods.network.NetworkDataProvider;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.consolidation.HalfLoopConsolidationJobProcessor.FleetAssignment;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.RealizedInVehicleCost;
import se.vti.samgods.transportation.fleet.FleetData;
import se.vti.samgods.transportation.fleet.FleetDataProvider;
import se.vti.samgods.transportation.fleet.SamgodsVehicleAttributes;

/**
 * 
 * @author GunnarF
 *
 */
public class LogisticChoiceDataProvider {

	// -------------------- CONSTANTS --------------------

	private final double initialTransportEfficiency = 0.7;

	private final RealizedInVehicleCost realizedInVehicleCost = new RealizedInVehicleCost();

	private final NetworkData internalNetworkData;

	private final FleetDataProvider fleetDataProvider;
	private final FleetData internalFleetData;

	// -------------------- CONSTRUCTION --------------------

	public LogisticChoiceDataProvider(NetworkDataProvider networkDataProvider, FleetDataProvider fleetDataProvider) {
		this.internalNetworkData = networkDataProvider.createNetworkData();
		this.fleetDataProvider = fleetDataProvider;
		this.internalFleetData = fleetDataProvider.createFleetData();
	}

	public LogisticChoiceData createLogisticChoiceData() {
		return new LogisticChoiceData(this, this.fleetDataProvider.createFleetData());
	}

	// -------------------- THREAD SAFE CONSOLIDATION COSTS --------------------

	// TODO Might not need to be concurrent.
	private ConcurrentMap<ConsolidationUnit, FleetAssignment> consolidationUnit2fleetAssignment = null;

	public void update(ConcurrentMap<ConsolidationUnit, FleetAssignment> consolidationUnit2fleetAssignment) {
		this.consolidationUnit2fleetAssignment = consolidationUnit2fleetAssignment;
	}

	private final ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2inVehicleTransportCost = new ConcurrentHashMap<>();

	private synchronized DetailedTransportCost createInVehicleTransportCost(ConsolidationUnit consolidationUnit) {
		if (this.consolidationUnit2fleetAssignment != null) {
			final FleetAssignment assignment = this.consolidationUnit2fleetAssignment.get(consolidationUnit);
			if (assignment != null && assignment.payload_ton >= 1e-3) { // WHY?
				try {
					final SamgodsVehicleAttributes vehicleAttributes = this.internalFleetData
							.getVehicleType2attributes().get(assignment.vehicleType);
					return this.realizedInVehicleCost.compute(vehicleAttributes, assignment.payload_ton,
							consolidationUnit, this.internalNetworkData.getLinkId2unitCost(assignment.vehicleType),
							this.internalNetworkData.getFerryLinkIds());
				} catch (InsufficientDataException e1) {
					throw new RuntimeException(e1);
				}
			}
		}
		final VehicleType vehicleType = this.internalFleetData.getRepresentativeVehicleType(consolidationUnit.commodity,
				consolidationUnit.samgodsMode, consolidationUnit.isContainer, consolidationUnit.containsFerry);
		if (vehicleType != null) {
			final SamgodsVehicleAttributes vehicleAttributes = this.internalFleetData.getVehicleType2attributes()
					.get(vehicleType);
			try {
				return this.realizedInVehicleCost.compute(vehicleAttributes,
						this.initialTransportEfficiency * vehicleAttributes.capacity_ton, consolidationUnit,
						this.internalNetworkData.getLinkId2unitCost(vehicleType),
						this.internalNetworkData.getFerryLinkIds());
			} catch (InsufficientDataException e) {
				throw new RuntimeException(
						"could not initialize unit cost for consolidation unit " + consolidationUnit);
			}
		} else {
			throw new RuntimeException("could not initialize unit cost for consolidation unit " + consolidationUnit);
		}
	}

	public DetailedTransportCost getInVehicleTransportCost(ConsolidationUnit consolidationUnit) {
		return this.consolidationUnit2inVehicleTransportCost.computeIfAbsent(consolidationUnit,
				cu -> this.createInVehicleTransportCost(cu));
	}

	// --------------- THREAD SAFE EPISODE UNIT COST ACCESS ---------------

	private final ConcurrentMap<TransportEpisode, DetailedTransportCost> episode2unitCost_1_ton = new ConcurrentHashMap<>();

	ConcurrentMap<TransportEpisode, DetailedTransportCost> getEpisode2unitCost_1_ton() {
		return this.episode2unitCost_1_ton;
	}
}
