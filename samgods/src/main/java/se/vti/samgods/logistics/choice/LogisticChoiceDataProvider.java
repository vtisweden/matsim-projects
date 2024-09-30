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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
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
			ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2realizedDomesticCost,
			FleetDataProvider fleetDataProvider) {
		this.consolidationUnit2realizedCost = consolidationUnit2realizedCost;
		this.consolidationUnit2realizedDomesticCost = consolidationUnit2realizedDomesticCost;
		this.fleetDataProvider = fleetDataProvider;
	}

	public LogisticChoiceData createLogisticChoiceData() {
		return new LogisticChoiceData(this, this.fleetDataProvider.createFleetData());
	}

	// -------------------- INTERNALS --------------------

	private ConcurrentMap<Commodity, ConcurrentMap<TransportMode, Double>> concurrentDeepCopy(
			Map<Commodity, Map<TransportMode, Double>> commodity2mode2double) {
		ConcurrentHashMap<Commodity, ConcurrentMap<TransportMode, Double>> copy = new ConcurrentHashMap<>(
				commodity2mode2double.size());
		for (Map.Entry<Commodity, Map<TransportMode, Double>> c2m2dEntry : commodity2mode2double.entrySet()) {
			copy.put(c2m2dEntry.getKey(), new ConcurrentHashMap<>(c2m2dEntry.getValue()));
		}
		return copy;
	}

	// ---------- THREAD SAFE CONSOLIDATION UNIT UNIT COST ACCESS ----------

	private final ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2realizedCost;
	private final ConcurrentMap<ConsolidationUnit, DetailedTransportCost> consolidationUnit2realizedDomesticCost;

	DetailedTransportCost getRealizedCost(ConsolidationUnit consolidationUnit) {
		return this.consolidationUnit2realizedCost.get(consolidationUnit);
	}

	DetailedTransportCost getRealizedDomesticCost(ConsolidationUnit consolidationUnit) {
		return this.consolidationUnit2realizedDomesticCost.get(consolidationUnit);
	}

	// --------------- THREAD SAFE EPISODE UNIT COST ACCESS ---------------

	private final ConcurrentMap<TransportEpisode, DetailedTransportCost> episode2unitCost_1_ton = new ConcurrentHashMap<>();

	ConcurrentMap<TransportEpisode, DetailedTransportCost> getEpisode2unitCost_1_ton() {
		return this.episode2unitCost_1_ton;
	}

	// --------------- THREAD SAFE BACKGROUND TRANSPORT CALIBRATION ---------------

//	private ConcurrentMap<Commodity, ConcurrentMap<TransportMode, Double>> commodity2mode2freightFactor = new ConcurrentHashMap<>();
//
//	public void setCommodity2mode2freightFactor(
//			Map<Commodity, Map<TransportMode, Double>> commodity2mode2freightFactor) {
//		this.commodity2mode2freightFactor = this.concurrentDeepCopy(commodity2mode2freightFactor);
//	}
//
//	ConcurrentMap<Commodity, ConcurrentMap<TransportMode, Double>> getCommodity2mode2freightFactor() {
//		return this.commodity2mode2freightFactor;
//	}

//	public ConcurrentMap<Commodity, ConcurrentMap<TransportMode, Double>> commodity2mode2avgTotalDemand_ton = new ConcurrentHashMap<>();
//
//	public void setCommodity2mode2avgTotalDemand_ton(
//			Map<Commodity, Map<TransportMode, Double>> commodity2mode2avgTotalDemand_ton) {
//		this.commodity2mode2avgTotalDemand_ton = this.concurrentDeepCopy(commodity2mode2avgTotalDemand_ton);
//	}
//
//	ConcurrentMap<Commodity, ConcurrentMap<TransportMode, Double>> getCommodity2mode2avgTotalDemand_ton() {
//		return this.commodity2mode2avgTotalDemand_ton;
//	}

}
