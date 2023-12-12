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

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.Shipment;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.choicemodel.ShipmentCostFunction.ShipmentCost;

/**
 * 
 * @author GunnarF
 *
 */
public class ChoiceSetGenerator {

	// -------------------- MEMBERS --------------------

	private final ShipmentCostFunction costCalculator;

	private final UtilityFunction utilityFunction;

	private final SizeClass[] allSizeClasses;

	// -------------------- CONSTRUCTION --------------------

	public ChoiceSetGenerator(final ShipmentCostFunction costCalculator, final UtilityFunction utilityFunction,
			final SizeClass[] allSizeClasses) {
		this.costCalculator = costCalculator;
		this.utilityFunction = utilityFunction;
		this.allSizeClasses = allSizeClasses;
	}

	// -------------------- PARTIAL IMPLEMENTATION --------------------

	public List<Alternative> createChoiceSet(final List<TransportChain> transportChains,
			final double totalShipmentSize_ton, final Commodity commodity) {
		final ArrayList<Alternative> result = new ArrayList<>(transportChains.size() * this.allSizeClasses.length);
		for (SizeClass sizeClass : this.allSizeClasses) {
			if (totalShipmentSize_ton >= sizeClass.getUpperValue_ton()) {
				for (TransportChain transportChain : transportChains) {
					final double shipmentSize_ton = sizeClass.getUpperValue_ton();
					final double frequency_1_yr = totalShipmentSize_ton / shipmentSize_ton;
					final Shipment shipment = new Shipment(commodity, transportChain, shipmentSize_ton, frequency_1_yr);
					final ShipmentCost shipmentCost = this.costCalculator.computeCost(shipment);
					result.add(new Alternative(sizeClass, shipment, shipmentCost,
							this.utilityFunction.computeUtility(shipment, shipmentCost)));
				}
			}
		}
		return result;
	}
}
