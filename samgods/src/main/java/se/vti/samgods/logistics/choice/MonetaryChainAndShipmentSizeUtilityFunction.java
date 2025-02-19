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

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.logistics.costs.NonTransportCost;
import se.vti.samgods.transportation.costs.DetailedTransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public class MonetaryChainAndShipmentSizeUtilityFunction implements ChainAndShipmentSizeUtilityFunction {

//	private final Map<TransportMode, Double> mode2meanASC;
	private final Map<Commodity, Double> commodity2railASC;
	private final Map<Commodity, Double> commodity2scale;

	public MonetaryChainAndShipmentSizeUtilityFunction(Map<Commodity, Double> commodity2scale,
//			Map<TransportMode, Double> mode2meanASC, 
			Map<Commodity, Double> commodity2railASC) {
		this.commodity2scale = commodity2scale;
//		this.mode2meanASC = mode2meanASC;
		this.commodity2railASC = commodity2railASC;
	}

	public double totalASC(TransportChain transportChain) {
//		if (this.mode2meanASC == null) {
//			return 0.0;
//		} else {
//			final Set<TransportMode> modes = new LinkedHashSet<>();
//			final Set<Commodity> railCommodities = new LinkedHashSet<>();
//			for (TransportEpisode episode : transportChain.getEpisodes()) {
//				modes.add(episode.getMode());
//				if (TransportMode.Rail.equals(episode.getMode())) {
//					railCommodities.add(episode.getCommodity());
//				}
//			}
//			return modes.stream().mapToDouble(m -> this.mode2meanASC.getOrDefault(m, 0.0)).sum()
//					+ railCommodities.stream().mapToDouble(c -> this.commodity2railASC.getOrDefault(c, 0.0)).sum();
//		}
		if (this.commodity2railASC == null) {
			return 0.0;
		} else {
			final Set<Commodity> railCommodities = new LinkedHashSet<>();
			for (TransportEpisode episode : transportChain.getEpisodes()) {
				if (TransportMode.Rail.equals(episode.getMode())) {
					railCommodities.add(episode.getCommodity());
				}
			}
			return railCommodities.stream().mapToDouble(c -> this.commodity2railASC.getOrDefault(c, 0.0)).sum();
		}
	}

	public double totalMonetaryCost(double amount_ton, DetailedTransportCost transportUnitCost,
			NonTransportCost totalNonTransportCost) {
		return transportUnitCost.monetaryCost * amount_ton + totalNonTransportCost.totalOrderCost
				+ totalNonTransportCost.totalEnRouteMonetaryLoss + totalNonTransportCost.totalInventoryCost;
	}

	@Override
	public double computeUtility(TransportChain transportChain, double amount_ton,
			DetailedTransportCost transportUnitCost_1_ton, NonTransportCost totalNonTransportCost) {

		return (-1.0) * this.commodity2scale.get(transportChain.getCommodity())
				* this.totalMonetaryCost(amount_ton, transportUnitCost_1_ton, totalNonTransportCost)
				+ this.totalASC(transportChain);
	}
}
