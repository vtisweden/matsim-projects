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
package se.vti.samgods.logistics;

import java.util.ArrayList;
import java.util.List;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.ShipmentCostCalculator.ShipmentCost;

public abstract class AbstractTransportChainAndShipmentChoiceModel implements TransportChainAndShipmentChoiceModel {

	public interface SizeClass {

		public double getLowerValue_ton();

		public double getUpperValue_ton();
	}

	protected class Alternative {

		public final SizeClass sizeClass;

		public final Shipment shipment;

		public final ShipmentCost cost;

		public final double utility;

		public Alternative(final SizeClass sizeClass, final Shipment shipment) {
			this.sizeClass = sizeClass;
			this.shipment = shipment;
			this.cost = costCalculator.computeCost(shipment);
			this.utility = utilityFunction.computeUtility(shipment, cost);
		}

		@Override
		public String toString() {
			return "TODO";
		}
	}

	public interface UtilityFunction {

		public double computeUtility(Shipment shipment, ShipmentCost shipmentCost);

	}

	protected final ShipmentCostCalculator costCalculator;

	protected final UtilityFunction utilityFunction;

	public AbstractTransportChainAndShipmentChoiceModel(ShipmentCostCalculator costCalculator,
			UtilityFunction utilityFunction) {
		this.costCalculator = costCalculator;
		this.utilityFunction = utilityFunction;
	}

	protected List<Alternative> createChoiceSet(List<TransportChain> transportChains, double totalShipmentSize_ton,
			Commodity commodity) {
		final ArrayList<Alternative> result = new ArrayList<>(transportChains.size() * this.allSizeClasses().length);
		for (SizeClass sizeClass : this.allSizeClasses()) {
			if (totalShipmentSize_ton >= sizeClass.getUpperValue_ton()) {
				for (TransportChain transportChain : transportChains) {
					final double shipmentSize_ton = sizeClass.getUpperValue_ton();
					final double frequency_1_yr = totalShipmentSize_ton / shipmentSize_ton;
					final Shipment shipment = new Shipment(commodity, transportChain, shipmentSize_ton, frequency_1_yr);
					result.add(new Alternative(sizeClass, shipment));
				}
			}
		}
		return result;
	}

	protected abstract SizeClass[] allSizeClasses();

}
