/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.logistics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.transportation.consolidation.road.ConsolidationCostModel;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChainStats {

	private class Data {
		private Data() {
		}
	}

	private final Map<SamgodsConstants.Commodity, Map<List<List<SamgodsConstants.TransportMode>>, Data>> commodity2modes2data = new LinkedHashMap<>();

	private final ConsolidationCostModel costModel;

	public TransportChainStats(ConsolidationCostModel costModel) {
		this.costModel = costModel;
	}

	public void register(TransportChain chain, SamgodsConstants.Commodity commodity) {

		List<List<SamgodsConstants.TransportMode>> modeSequence = chain.getTransportModeSequence();

		Map<List<List<SamgodsConstants.TransportMode>>, Data> modes2data = this.commodity2modes2data
				.computeIfAbsent(commodity, c -> new LinkedHashMap<List<List<SamgodsConstants.TransportMode>>, Data>());

		Data data = modes2data.computeIfAbsent(chain.getTransportModeSequence(), seq -> new Data());

		// TODO incomplete
		
	}

}
