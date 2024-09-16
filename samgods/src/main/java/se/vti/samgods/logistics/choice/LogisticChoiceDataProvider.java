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

import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkDataProvider;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.fleet.FleetDataProvider;

/**
 * 
 * @author GunnarF
 *
 */
public class LogisticChoiceDataProvider {

	// -------------------- CONSTANTS --------------------

	private final NetworkDataProvider networkDataProvider;
	private final FleetDataProvider fleetDataProvider;

	// -------------------- CONSTRUCTION --------------------

	public LogisticChoiceDataProvider(// Map<TransportMode, Double> mode2efficiency,
			ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2realizedCost,
			NetworkDataProvider networkDataProvider, FleetDataProvider fleetDataProvider) {
//		this.mode2efficiency = new ConcurrentHashMap<>(mode2efficiency);
		this.consolidationUnit2realizedCost = consolidationUnit2realizedCost;
		this.networkDataProvider = networkDataProvider;
		this.fleetDataProvider = fleetDataProvider;
	}

	public LogisticChoiceData createLogisticChoiceData() {
		return new LogisticChoiceData(this, this.networkDataProvider.createNetworkData(),
				this.fleetDataProvider.createFleetData());
	}

	// -------------------- THREAD SAFE EFFICIENCY ACCESS --------------------

//	private final ConcurrentMap<TransportMode, Double> mode2efficiency;
	private final ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2realizedCost;

	DetailedTransportCost getRealizedCost(ConsolidationUnit consolidationUnit) {
		return this.consolidationUnit2realizedCost.get(consolidationUnit);
	}

// -------------------- THREAD SAFE UNIT COST ACCESS --------------------

	private final ConcurrentMap<TransportEpisode, DetailedTransportCost> episode2unitCost_1_ton = new ConcurrentHashMap<>();

	ConcurrentMap<TransportEpisode, DetailedTransportCost> getEpisode2unitCost_1_ton() {
		return this.episode2unitCost_1_ton;
	}
}
