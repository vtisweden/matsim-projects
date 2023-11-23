/**
 * org.matsim.contrib.emulation
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

import se.vti.samgods.legacy.Samgods;
import se.vti.samgods.legacy.Samgods.Commodity;
import se.vti.samgods.logistics.Shipment;
import se.vti.samgods.logistics.ShipmentCostCalculator;
import se.vti.samgods.logistics.TransportLeg;
import se.vti.samgods.transportation.TransportPrices;
import se.vti.samgods.transportation.TransportPrices.UnitPrice;

public class SaanaUtilityFunction implements ShipmentCostCalculator, TransportChainAndShipmentChoiceModelImpl.UtilityFunction {

	final boolean intitialTransshipmentCosts = true;
	final boolean finalTransshipmentCosts = true;

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

	private final Map<Samgods.Commodity, Betas> commodity2betas = new LinkedHashMap<>();

	private TransportPrices transportPrices;
	
	public SaanaUtilityFunction(TransportPrices transportPrices) {
		this.transportPrices = transportPrices;
	}

	@Override
	public ShipmentCost computeCost(final Shipment shipment) {

		double transportCostSum = 0.0;
		double durationSum_h = 0.0;
		if (this.intitialTransshipmentCosts) {
			final UnitPrice initialTransshipmentCost = this.transportPrices.getUnitPrice(shipment.getCommmodity(),
					shipment.getTransportChain().getOrigin());
			transportCostSum += shipment.getSize_ton() * initialTransshipmentCost.getTransportPrice_1_ton();
			durationSum_h += initialTransshipmentCost.getTransportDuration_h();
		}
		for (TransportLeg leg : shipment.getTransportChain().getLegs()) {
			final UnitPrice transportCost = this.transportPrices.getUnitPrice(shipment.getCommmodity(), leg);
			transportCostSum += shipment.getSize_ton() * transportCost.getTransportPrice_1_ton();
			durationSum_h = transportCost.getTransportDuration_h();
			if (this.finalTransshipmentCosts
					|| !shipment.getTransportChain().getDestination().equals(leg.getDestination())) {
				final UnitPrice transshipmentCost = this.transportPrices.getUnitPrice(shipment.getCommmodity(), leg.getDestination());
				transportCostSum += shipment.getSize_ton() * transshipmentCost.getTransportPrice_1_ton();
				durationSum_h = transshipmentCost.getTransportDuration_h();
			}
		}

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
