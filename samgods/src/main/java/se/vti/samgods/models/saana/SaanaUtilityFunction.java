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

import floetteroed.utilities.Units;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;
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
public class SaanaUtilityFunction implements ShipmentCostFunction, UtilityFunction {

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

	private final Map<SamgodsConstants.Commodity, Betas> commodity2betas = new LinkedHashMap<>();

	private TransportPrices<?, ?> transportPrices;

	public SaanaUtilityFunction(TransportPrices<?, ?> transportPrices) {
		this.transportPrices = transportPrices;
	}

	@Override
	public ShipmentCost computeCost(final Shipment shipment) {

		double transportCostSum = 0.0;
		double durationSum_h = 0.0;

		// loading
		final ShipmentPrices initialShipmentPrices = this.transportPrices.getShipmentPrices(shipment.getCommmodity(),
				shipment.getTransportChain().getLegs().get(0).getMode());
		transportCostSum += shipment.getSize_ton() * initialShipmentPrices.getLoadingPrice_1_ton(null);
		durationSum_h += Units.H_PER_MIN * initialShipmentPrices.getLoadingDuration_min(null);

		for (TransportLeg leg : shipment.getTransportChain().getLegs()) {

			// move along a leg
			// TODO EACH LEG MAY HAVE MULTIPLE LINKS
			final ShipmentPrices shipmentPrices = this.transportPrices.getShipmentPrices(shipment.getCommmodity(),
					leg.getMode());
			transportCostSum += shipment.getSize_ton() * shipmentPrices.getMovePrice_1_ton(null);
			durationSum_h = shipmentPrices.getMoveDuration_h(null);

			// transshipment, unless last
			if (!shipment.getTransportChain().getDestination().equals(leg.getDestination())) {
				final TransshipmentPrices transshipmentPrices = this.transportPrices
						.getTransshipmentPrices(shipment.getCommmodity());
				transportCostSum += shipment.getSize_ton()
						* transshipmentPrices.getTransshipmentPrice_1_ton(null, null, null);
				durationSum_h = Units.H_PER_MIN * transshipmentPrices.getTransshipmentDuration_min(null, null, null);
			}
		}

		// unloading
		final ShipmentPrices finalShipmentPrices = this.transportPrices.getShipmentPrices(shipment.getCommmodity(),
				shipment.getTransportChain().getLegs().get(shipment.getTransportChain().getLegs().size() - 1)
						.getMode());
		transportCostSum += shipment.getSize_ton() * finalShipmentPrices.getUnloadingPrice_1_ton(null);
		durationSum_h += Units.H_PER_MIN * initialShipmentPrices.getUnloadingDuration_min(null);

		final double totalDuration_yr = durationSum_h / 24.0 / 365.0;
		final double interShipmentDuration_yr = 1.0 / shipment.getFrequency_1_yr();

		final Betas betas = this.commodity2betas.get(shipment.getCommmodity());

		final double capitalCost = betas.carryingCostCoeff_1_yrTon * totalDuration_yr * shipment.getSize_ton();
		final double valueDensity = betas.carryingCostCoeff_1_yrTon * interShipmentDuration_yr * shipment.getSize_ton();

		return new ShipmentCost(durationSum_h, transportCostSum, capitalCost, valueDensity);
	}

	@Override
	public double computeUtility(Shipment shipment, ShipmentCost shipmentCost) {
		final Betas betas = this.commodity2betas.get(shipment.getCommmodity());
		double utility = betas.transportCostCoeff * shipmentCost.transportCost
				+ betas.inTransitCaptialCostCoeff * shipmentCost.capitalCost
				+ betas.valueDensityCoeff * shipmentCost.valueDensity;
		return utility;
	}

	public double getMonetaryValue_1_ton(Commodity commodity) {
		// TODO Auto-generated method stub
		return 0;
	}

}
