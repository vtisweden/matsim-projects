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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.ShipmentSize;
import se.vti.samgods.logistics.StorageCost;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.logistics.TransportDemand.AnnualShipment;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.models.TestSamgods;
import se.vti.samgods.transportation.DetailedTransportCost;
import se.vti.samgods.transportation.EpisodeCostModel;
import se.vti.samgods.transportation.InsufficientDataException;
import se.vti.samgods.utils.ChoiceModelUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class ChainAndShipmentSizeChoiceModel {

	// -------------------- MEMBERS --------------------

	private final ChoiceModelUtils choiceModel = new ChoiceModelUtils();

	private final AnnualShipmentUtilityFunction utilityFunction;

	private final EpisodeCostModel episodeCostModel;

	// -------------------- CONSTRUCTION --------------------

	public ChainAndShipmentSizeChoiceModel(final AnnualShipmentUtilityFunction utilityFunction,
			EpisodeCostModel episodeCostModel) {
		this.utilityFunction = utilityFunction;
		this.episodeCostModel = episodeCostModel;
	}

	// -------------------- IMPLEMENTATION --------------------

	public List<ChainAndShipmentSize> choose(SamgodsConstants.Commodity commodity, OD od,
			Set<TransportChain> transportChains, List<TransportDemand.AnnualShipment> annualShipments) {

		final Map<TransportChain, DetailedTransportCost> chain2transportUnitCost = new LinkedHashMap<>(
				transportChains.size());
		for (TransportChain transportChain : transportChains) {
			try {
				final DetailedTransportCost.Builder builder = new DetailedTransportCost.Builder().addAmount_ton(1.0);
				for (TransportEpisode episode : transportChain.getEpisodes()) {
					final DetailedTransportCost episodeUnitCost = this.episodeCostModel.computeUnitCost(episode);
					builder.addLoadingCost(episodeUnitCost.loadingCost)
							.addLoadingDuration_h(episodeUnitCost.loadingDuration_h)
							.addMoveCost(episodeUnitCost.moveCost).addMoveDuration_h(episodeUnitCost.moveDuration_h)
							.addTransferCost(episodeUnitCost.transferCost)
							.addTransferDuration_h(episodeUnitCost.transferDuration_h)
							.addUnloadingCost(episodeUnitCost.unloadingCost)
							.addUnloadingDuration_h(episodeUnitCost.unloadingDuration_h);
				}
				chain2transportUnitCost.put(transportChain, builder.build());
			} catch (InsufficientDataException e0) {
				e0.log(this.getClass(), "No transport cost data for at least one episode in this transport chain.",
						transportChain);
			}
		}

		if (chain2transportUnitCost.size() == 0) {
			new InsufficientDataException(this.getClass(), "No transport chains with transport cost available.",
					commodity, od, null, null, null);
			return new ArrayList<>(0);
		} else {
			
			final List<ChainAndShipmentSize> choices = new ArrayList<>(annualShipments.size());
			for (AnnualShipment annualShipment : annualShipments) {

				final List<ChainAndShipmentSize> alternatives = new ArrayList<>();
				for (Map.Entry<TransportChain, DetailedTransportCost> e : chain2transportUnitCost.entrySet()) {
					final TransportChain transportChain = e.getKey();
					final DetailedTransportCost transportUnitCost = e.getValue();
					for (ShipmentSize size : SamgodsConstants.ShipmentSize.values()) {
						if ((annualShipment.getSingleInstanceAmount_ton() >= size.getUpperValue_ton())
								|| SamgodsConstants.ShipmentSize.getSmallestClass_ton().equals(size)) {
							// >>> TODO >>>
							final StorageCost storageUnitCost = null;
							// <<< TODO <<<
							alternatives.add(new ChainAndShipmentSize(size, transportChain,
									this.utilityFunction.computeUtility(commodity,
											annualShipment.getSingleInstanceAmount_ton(), transportUnitCost,
											storageUnitCost)));
						}
					}
				}

				if (alternatives.size() == 0) {
					new InsufficientDataException(TestSamgods.class, "Insufficient cost data for this transport chain.",
							commodity, od, null, null, null).log(TestSamgods.class, "dito");
				} else {
					for (int instance = 0; instance < annualShipment.getNumberOfInstances(); instance++) {
						final ChainAndShipmentSize choice = this.choiceModel.choose(alternatives, a -> a.utility);
						assert (choice != null);
						choices.add(choice);
					}
				}
			}
			return choices;
		}
	}
}
