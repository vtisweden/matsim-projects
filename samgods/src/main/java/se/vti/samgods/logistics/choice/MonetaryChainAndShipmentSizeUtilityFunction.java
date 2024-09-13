/**
 * se.vti.samgods.logistics.choicemodel
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.samgods.logistics.choice;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.costs.NonTransportCost;
import se.vti.samgods.transportation.costs.DetailedTransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public class MonetaryChainAndShipmentSizeUtilityFunction implements ChainAndShipmentSizeUtilityFunction {

	public double totalMonetaryCost(double amount_ton, DetailedTransportCost transportUnitCost,
			NonTransportCost totalNonTransportCost) {
		return transportUnitCost.monetaryCost * amount_ton + totalNonTransportCost.totalOrderCost
				+ totalNonTransportCost.totalEnRouteMonetaryLoss + totalNonTransportCost.totalInventoryCost;
	}

	@Override
	public double computeUtility(Commodity commodity, double amount_ton, DetailedTransportCost transportUnitCost,
			NonTransportCost totalNonTransportCost) {
		return (-1.0) * this.totalMonetaryCost(amount_ton, transportUnitCost, totalNonTransportCost);
	}
}
