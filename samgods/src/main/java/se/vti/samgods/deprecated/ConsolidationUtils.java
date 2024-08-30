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
package se.vti.samgods.deprecated;

import java.util.ArrayList;
import java.util.List;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportDemand;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationUtils {

	private ConsolidationUtils() {
	}

//	public static Map<Signature.Episode, List<TransportChain>> createEpisodeSignature2chains(
//			Map<OD, List<TransportChain>> od2chains) {
//		final Map<Signature.Episode, List<TransportChain>> signature2chains = new LinkedHashMap<>();
//		for (Map.Entry<OD, List<TransportChain>> odAndChains : od2chains.entrySet()) {
//			for (TransportChain chain : odAndChains.getValue()) {
//				for (TransportEpisode episode : chain.getEpisodes()) {
//					final Signature.Episode signature = new Signature.Episode(episode);
//					signature2chains.computeIfAbsent(signature, s -> new LinkedList<>()).add(chain);
//				}
//			}
//		}
//		return signature2chains;
//	}

	public static List<Shipment> disaggregateIntoAnalysisPeriod(TransportDemand.AnnualShipment annualShipment,
			int analysisPeriod_days, SamgodsConstants.ShipmentSize sizeClass) {

		final double amountPerPeriod_ton = annualShipment.getTotalAmount_ton() * analysisPeriod_days / 365.0;
		final double shipmentsPerPeriod = amountPerPeriod_ton / sizeClass.getRepresentativeValue_ton();

		final int completeShipmentsPerPeriod = (int) Math.floor(shipmentsPerPeriod);
		final double fractionalShipmentsPerPeriod = shipmentsPerPeriod - completeShipmentsPerPeriod;
		final double singleShipmentSize_ton = amountPerPeriod_ton / Math.ceil(shipmentsPerPeriod);

		final List<Shipment> shipments = new ArrayList<>(
				completeShipmentsPerPeriod + (fractionalShipmentsPerPeriod > 0 ? 1 : 0));
		for (int i = 0; i < completeShipmentsPerPeriod; i++) {
			shipments.add(new Shipment(annualShipment.getCommodity(), singleShipmentSize_ton, 1.0));
		}
		if (fractionalShipmentsPerPeriod > 0) {
			shipments.add(
					new Shipment(annualShipment.getCommodity(), singleShipmentSize_ton, fractionalShipmentsPerPeriod));
		}

		return shipments;
	}
}
