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
package se.vti.samgods.logistics.choicemodel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import se.vti.samgods.ConsolidationUnit;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.network.NetworkDataProvider;
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

	public LogisticChoiceDataProvider(Map<TransportMode, Double> mode2efficiency,
			Map<ConsolidationUnit, Double> consolidationUnit2efficiency, NetworkDataProvider networkData,
			FleetDataProvider fleetData) {
		this.mode2efficiency = new ConcurrentHashMap<>(mode2efficiency);
		this.consolidationUnit2efficiency = new ConcurrentHashMap<>(consolidationUnit2efficiency);
		this.networkDataProvider = networkData;
		this.fleetDataProvider = fleetData;
	}

	public LogisticChoiceData createLogisticChoiceData() {
		return new LogisticChoiceData(this, this.networkDataProvider.createNetworkData(),
				this.fleetDataProvider.createFleetData());
	}

	// -------------------- THREAD SAFE --------------------

	private final ConcurrentMap<TransportMode, Double> mode2efficiency;
	private final ConcurrentMap<ConsolidationUnit, Double> consolidationUnit2efficiency;

	double getEfficiency(ConsolidationUnit consolidationUnit) {
		return this.consolidationUnit2efficiency.getOrDefault(consolidationUnit,
				this.mode2efficiency.get(consolidationUnit.mode));
	}

// -------------------- THREAD SAFE, INTERNALLY CHACHED --------------------

	private final ConcurrentMap<TransportEpisode, DetailedTransportCost> episode2unitCost_1_ton = new ConcurrentHashMap<>();

	ConcurrentMap<TransportEpisode, DetailedTransportCost> getEpisode2unitCost_1_ton(TransportEpisode episode) {
		return this.episode2unitCost_1_ton;
	}
}
