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

	private final FleetDataProvider fleetDataProvider;

	// -------------------- CONSTRUCTION --------------------

	public LogisticChoiceDataProvider(
			ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2realizedCost,
			FleetDataProvider fleetDataProvider) {
		this.consolidationUnit2realizedCost = consolidationUnit2realizedCost;
		this.fleetDataProvider = fleetDataProvider;
	}

	public LogisticChoiceData createLogisticChoiceData() {
		return new LogisticChoiceData(this, this.fleetDataProvider.createFleetData());
	}

	// ---------- THREAD SAFE CONSOLIDATION UNIT UNIT COST ACCESS ----------

	private final ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2realizedCost;

	DetailedTransportCost getRealizedCost(ConsolidationUnit consolidationUnit) {
		return this.consolidationUnit2realizedCost.get(consolidationUnit);
	}

// -------------------- THREAD SAFE EPISODE UNIT COST ACCESS --------------------

	private final ConcurrentMap<TransportEpisode, DetailedTransportCost> episode2unitCost_1_ton = new ConcurrentHashMap<>();

	ConcurrentMap<TransportEpisode, DetailedTransportCost> getEpisode2unitCost_1_ton() {
		return this.episode2unitCost_1_ton;
	}
}
