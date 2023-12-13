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
package se.vti.samgods.transportation.pricing;

import org.matsim.api.core.v01.network.Link;

import floetteroed.utilities.Units;
import se.vti.samgods.logistics.Shipment;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.logistics.choicemodel.ShipmentCostFunction;
import se.vti.samgods.transportation.pricing.TransportPrices.ShipmentPrices;
import se.vti.samgods.transportation.pricing.TransportPrices.TransshipmentPrices;

/**
 * 
 * @author GunnarF
 *
 */
public class BasicShipmentCostFunction implements ShipmentCostFunction<BasicShipmentCost> {

	private final TransportPrices<?, ?> transportPrices;

	public BasicShipmentCostFunction(TransportPrices<?, ?> transportPrices) {
		this.transportPrices = transportPrices;
	}

	public BasicShipmentCost computeCost(final Shipment shipment) {

		double transportCostSum = 0.0;
		double durationSum_min = 0.0;

		final TransportLeg firstLeg = shipment.getTransportChain().getLegs().get(0);
		final TransportLeg lastLeg = shipment.getTransportChain().getLegs()
				.get(shipment.getTransportChain().getLegs().size() - 1);

		TransportLeg previousLeg = null;
		for (int i = 0; i < shipment.getTransportChain().getLegs().size(); i++) {
			final TransportLeg leg = shipment.getTransportChain().getLegs().get(i);
			final ShipmentPrices shipmentPrices = this.transportPrices.getShipmentPrices(shipment.getCommmodity(),
					leg.getMode());

			if (leg == firstLeg) {
				// loading
				transportCostSum += shipment.getSize_ton() * shipmentPrices.getLoadingPrice_1_ton(leg.getOrigin());
				durationSum_min += shipmentPrices.getLoadingDuration_min(leg.getOrigin());
			}

			// move along a leg
			for (Link link : leg.getRouteView()) {
				transportCostSum += shipment.getSize_ton() * shipmentPrices.getMovePrice_1_ton(link.getId());
				durationSum_min = shipmentPrices.getMoveDuration_min(link.getId());
			}

			if (leg != lastLeg) {
				if (previousLeg != null) {
				// transshipment
				final TransshipmentPrices transshipmentPrices = this.transportPrices
						.getTransshipmentPrices(shipment.getCommmodity());
				transportCostSum += shipment.getSize_ton() * transshipmentPrices.getTransshipmentPrice_1_ton(
						previousLeg.getDestination(), previousLeg.getMode(), leg.getMode());
				durationSum_min = transshipmentPrices.getTransshipmentDuration_min(previousLeg.getDestination(),
						previousLeg.getMode(), leg.getMode());
				}
			} else {
				// unloading
				transportCostSum += shipment.getSize_ton()
						* shipmentPrices.getUnloadingPrice_1_ton(leg.getDestination());
				durationSum_min += shipmentPrices.getUnloadingDuration_min(leg.getDestination());
			}
			previousLeg = leg;
		}

		return new BasicShipmentCost(Units.H_PER_MIN * durationSum_min, transportCostSum);
	}
}
