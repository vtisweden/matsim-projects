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
package se.vti.samgods.models.saana;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.TransportPrices;
import se.vti.samgods.TransportPrices.ShipmentPrices;
import se.vti.samgods.TransportPrices.TransshipmentPrices;
import se.vti.samgods.logistics.Shipment;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.logistics.choicemodel.ShipmentCostFunction;
import se.vti.samgods.logistics.choicemodel.UtilityFunction;

/**
 * 
 * @author GunnarF
 *
 */
public class SaanaCostAndUtilityFunction implements ShipmentCostFunction, UtilityFunction {

	// -------------------- INNER CLASS --------------------

	class Betas {
		final double transportCostCoeff;
		final double inTransitCaptialCostCoeff;
		final double valueDensityCoeff;
		final double carryingCostCoeff_1_yrTon;

		Betas(double transportCostCoeff, final double beta_yearlyInTransitCaptialCost, final double beta_valueDensity,
				final double carryingCostCoeff) {
			this.transportCostCoeff = transportCostCoeff;
			this.inTransitCaptialCostCoeff = beta_yearlyInTransitCaptialCost;
			this.valueDensityCoeff = beta_valueDensity;
			this.carryingCostCoeff_1_yrTon = carryingCostCoeff;
		}
	}

	// -------------------- MEMBERS --------------------

	private final TransportPrices<?, ?> transportPrices;

	// TODO empty
	private final Map<SamgodsConstants.Commodity, Betas> commodity2betas = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public SaanaCostAndUtilityFunction(TransportPrices<?, ?> transportPrices) {
		this.transportPrices = transportPrices;
	}

	// --------------- IMPLEMENTATION OF ShipmentCostFunction ---------------

	@Override
	public ShipmentCost computeCost(final Shipment shipment) {

		double transportCostSum = 0.0;
		double durationSum_min = 0.0;

		final TransportLeg firstLeg = shipment.getTransportChain().getLegs().get(0);
		final TransportLeg lastLeg = shipment.getTransportChain().getLegs()
				.get(shipment.getTransportChain().getLegs().size() - 1);

		for (int i = 0; i < shipment.getTransportChain().getLegs().size(); i++) {
			final TransportLeg leg = shipment.getTransportChain().getLegs().get(i);
			final ShipmentPrices shipmentPrices = this.transportPrices.getShipmentPrices(shipment.getCommmodity(),
					leg.getMode());

			if (leg == firstLeg) {
				transportCostSum += shipment.getSize_ton() * shipmentPrices.getLoadingPrice_1_ton(leg.getOrigin());
				durationSum_min += shipmentPrices.getLoadingDuration_min(leg.getOrigin());
			}

			// move along a leg

			for (Link link : leg.getRouteView()) {
				transportCostSum += shipment.getSize_ton() * shipmentPrices.getMovePrice_1_ton(link.getId());
				durationSum_min = shipmentPrices.getMoveDuration_min(link.getId());
			}

			if (leg != lastLeg) {
				// transshipment
				final TransshipmentPrices transshipmentPrices = this.transportPrices
						.getTransshipmentPrices(shipment.getCommmodity());
				transportCostSum += shipment.getSize_ton()
						* transshipmentPrices.getTransshipmentPrice_1_ton(null, null, null);
				durationSum_min = transshipmentPrices.getTransshipmentDuration_min(null, null, null);
			} else {
				transportCostSum += shipment.getSize_ton()
						* shipmentPrices.getUnloadingPrice_1_ton(leg.getDestination());
				durationSum_min += shipmentPrices.getUnloadingDuration_min(leg.getDestination());
			}
		}

		final double totalDuration_yr = durationSum_min / 60.0 / 24.0 / 365.0;
		final double interShipmentDuration_yr = 1.0 / shipment.getFrequency_1_yr();

		final Betas betas = this.commodity2betas.get(shipment.getCommmodity());

		final double capitalCost = betas.carryingCostCoeff_1_yrTon * totalDuration_yr * shipment.getSize_ton();
		final double valueDensity = betas.carryingCostCoeff_1_yrTon * interShipmentDuration_yr * shipment.getSize_ton();

		return new ShipmentCost(durationSum_min / 60.0, transportCostSum, capitalCost, valueDensity);
	}

	// -------------------- IMPLEMENTATION OF UtilityFunction --------------------

	@Override
	public double computeUtility(Shipment shipment, ShipmentCost shipmentCost) {
		final Betas betas = this.commodity2betas.get(shipment.getCommmodity());
		double utility = betas.transportCostCoeff * shipmentCost.transportCost
				+ betas.inTransitCaptialCostCoeff * shipmentCost.capitalCost
				+ betas.valueDensityCoeff * shipmentCost.valueDensity;
		return utility;
	}
}
