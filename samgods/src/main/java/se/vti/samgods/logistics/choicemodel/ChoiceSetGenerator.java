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
import java.util.Arrays;
import java.util.List;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.AnnualShipment;
import se.vti.samgods.logistics.TransportChain;

/**
 * 
 * @author GunnarF
 *
 */
public class ChoiceSetGenerator<C extends ShipmentCost> {

	// -------------------- MEMBERS --------------------

	private final ShipmentCostFunction<C> costCalculator;

	private final ShipmentUtilityFunction<C> utilityFunction;

	private final SizeClass[] allSizeClasses;
	private final SizeClass smallestSizeClass;

	// -------------------- CONSTRUCTION --------------------

	public ChoiceSetGenerator(final ShipmentCostFunction<C> costCalculator,
			final ShipmentUtilityFunction<C> utilityFunction, final SizeClass[] allSizeClasses) {
		this.costCalculator = costCalculator;
		this.utilityFunction = utilityFunction;
		this.allSizeClasses = allSizeClasses;
		this.smallestSizeClass = Arrays.stream(allSizeClasses)
				.reduce((a, b) -> a.getUpperValue_ton() < b.getUpperValue_ton() ? a : b).get();
	}

	// -------------------- PARTIAL IMPLEMENTATION --------------------

	public List<Alternative<C>> combineWithSizeClass(final List<TransportChain> transportChains,
			final SizeClass sizeClass, final double totalShipmentSize_ton, final Commodity commodity) {
		final ArrayList<Alternative<C>> result = new ArrayList<>(transportChains.size());
		for (TransportChain transportChain : transportChains) {
//			final double shipmentSize_ton = sizeClass.getUpperValue_ton();
//			final double frequency_1_yr = totalShipmentSize_ton / shipmentSize_ton;
			final AnnualShipment shipment = new AnnualShipment(commodity, transportChain, totalShipmentSize_ton);
			final C shipmentCost = this.costCalculator.computeCost(shipment, sizeClass);
			result.add(new Alternative<>(sizeClass, shipment, shipmentCost,
					this.utilityFunction.computeUtility(shipment, shipmentCost)));
		}
		return result;
	}

	public List<Alternative<C>> createChoiceSet(final List<TransportChain> transportChains,
			final double totalShipmentSize_ton, final Commodity commodity) {
		final ArrayList<Alternative<C>> result = new ArrayList<>(transportChains.size() * this.allSizeClasses.length);
		for (SizeClass sizeClass : this.allSizeClasses) {
			if (totalShipmentSize_ton >= sizeClass.getUpperValue_ton()) {
				result.addAll(this.combineWithSizeClass(transportChains, sizeClass, totalShipmentSize_ton, commodity));
			}
		}
		if (result.size() == 0) {
			result.addAll(
					this.combineWithSizeClass(transportChains, this.smallestSizeClass, totalShipmentSize_ton, commodity));
		}
		return result;
	}
}
