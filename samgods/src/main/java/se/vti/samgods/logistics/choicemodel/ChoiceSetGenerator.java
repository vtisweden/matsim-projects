/**
 * se.vti.samgods
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import se.vti.samgods.DetailedTransportCost;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.ShipmentSizeClass;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.transportation.consolidation.EpisodeCostModel;

/**
 * 
 * @author GunnarF
 *
 */
public class ChoiceSetGenerator {

	// -------------------- MEMBERS --------------------

//	private final EpisodeCostModel empiricalEpisodeCostModel;
//	private final EpisodeCostModel fallbackCostModel;

	private final AnnualShipmentUtilityFunction utilityFunction;

	// -------------------- CONSTRUCTION --------------------

	public ChoiceSetGenerator(
			EpisodeCostModel empiricalEpisodeCostModel, EpisodeCostModel fallbackCostModel,
			final AnnualShipmentUtilityFunction utilityFunction) {
//		this.empiricalEpisodeCostModel = empiricalEpisodeCostModel;
//		this.fallbackCostModel = fallbackCostModel;
		this.utilityFunction = utilityFunction;
	}

	// -------------------- INTERNALS --------------------

//	private DetailedTransportCost computeCost_1_ton(TransportChain transportChain) {
//		DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(1.0);
//		for (TransportEpisode episode : transportChain.getEpisodes()) {
//			DetailedTransportCost episodeCost = null;
//			if (this.empiricalEpisodeCostModel != null) {
//				episodeCost = this.empiricalEpisodeCostModel.computeCost_1_ton(episode);
//			}
//			if (episodeCost == null) {
//				episodeCost = this.fallbackCostModel.computeCost_1_ton(episode);
//			}
//			builder.addLoadingCost(episodeCost.loadingCost).addLoadingDuration_h(episodeCost.loadingDuration_h)
//					.addMoveCost(episodeCost.moveCost).addMoveDuration_h(episodeCost.moveDuration_h)
//					.addTransferCost(episodeCost.transferCost).addTransferDuration_h(episodeCost.transferDuration_h)
//					.addUnloadingCost(episodeCost.unloadingCost)
//					.addUnloadingDuration_h(episodeCost.unloadingDuration_h);
//		}
//		return builder.build();
//	}

//	private List<Alternative> combineWithSizeClass(Commodity commodity, OD od, AnnualShipments.AnnualShipment shipment,
//			final SamgodsConstants.ShipmentSizeClass sizeClass, List<TransportChain> transportChains) {
//		final ArrayList<Alternative> result = new ArrayList<>(transportChains.size());
//		for (TransportChain transportChain : transportChains) {
//			// TODO Compute upstream and store?
//			final DetailedTransportCost transportUnitCost_1_ton = this.computeCost_1_ton(transportChain);
//			// TODO Add storage cost!
//			result.add(new Alternative(sizeClass, transportChain,
//					this.utilityFunction.computeUtility(commodity, transportUnitCost_1_ton)));
//		}
//		return result;
//	}

	// -------------------- IMPLEMENTATION --------------------

	public List<Alternative> createChoiceSet(final Commodity commodity, TransportDemand.AnnualShipment annualShipment,
			final Map<TransportChain, DetailedTransportCost> transportChain2transportUnitCost_1_ton) {
		final ArrayList<Alternative> result = new ArrayList<>(
				transportChain2transportUnitCost_1_ton.size() * SamgodsConstants.ShipmentSizeClass.values().length);
		for (Map.Entry<TransportChain, DetailedTransportCost> chain2transpCost : transportChain2transportUnitCost_1_ton
				.entrySet()) {
			final ArrayList<Alternative> resultPerChain = new ArrayList<>(
					SamgodsConstants.ShipmentSizeClass.values().length);
			for (ShipmentSizeClass sizeClass : SamgodsConstants.ShipmentSizeClass.values()) {
				if (annualShipment.getSingleInstanceAmount() >= sizeClass.getUpperValue_ton()) {
					resultPerChain.add(new Alternative(sizeClass, chain2transpCost.getKey(), this.utilityFunction
							.computeUtility(commodity, annualShipment, chain2transpCost.getValue())));
				}
			}
			if (resultPerChain.size() == 0) {
				resultPerChain.add(new Alternative(SamgodsConstants.ShipmentSizeClass.getSmallestClass_ton(),
						chain2transpCost.getKey(),
						this.utilityFunction.computeUtility(commodity, annualShipment, chain2transpCost.getValue())));
			}
			result.addAll(resultPerChain);
		}
		return result;
	}
}
