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
import se.vti.samgods.logistics.StorageCost;
import se.vti.samgods.transportation.DetailedTransportCost;

/**
 * 
 * @author GunnarF
 *
 */
public interface AnnualShipmentUtilityFunction {

	double computeUtility(SamgodsConstants.Commodity commodity, double amount_ton, DetailedTransportCost transportUnitCost,
			StorageCost storageUnitCost);

}