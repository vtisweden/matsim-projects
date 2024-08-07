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

import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.NonTransportCost;
import se.vti.samgods.transportation.DetailedTransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public interface ChainAndShipmentSizeUtilityFunction {

	/**
	 * Assumes that consolidation and storage cost have been transferred into
	 * respective unit costs, meaning that total costs result from a scale-up of
	 * unit costs with annualAmount_ton.
	 * 
	 * The purpose of this interface is to enable the separate weighting of
	 * individual cost terms (monetary, time, distance) into one commodity-specific
	 * utility value.
	 * 
	 */
	double computeUtility(SamgodsConstants.Commodity commodity, double annualAmount_ton,
			DetailedTransportCost transportUnitCost, NonTransportCost nonTransportCost);

}