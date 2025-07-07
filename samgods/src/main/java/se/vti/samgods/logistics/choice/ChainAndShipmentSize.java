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
package se.vti.samgods.logistics.choice;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import se.vti.samgods.common.SamgodsConstants;
import se.vti.samgods.external.rail.ChainAndAnnualShipmentJsonSerializer;
import se.vti.samgods.logistics.AnnualShipment;
import se.vti.samgods.logistics.TransportChain;

/**
 * 
 * @author GunnarF
 *
 */
@JsonSerialize(using = ChainAndAnnualShipmentJsonSerializer.class)
public class ChainAndShipmentSize {

	// -------------------- PUBLIC CONSTANTS --------------------

	public final AnnualShipment annualShipment;

	public final SamgodsConstants.ShipmentSize sizeClass;

	public final TransportChain transportChain;

	public final double singleInstanceUtility;

	// -------------------- CONSTRUCTION --------------------

	public ChainAndShipmentSize(AnnualShipment annualShipment, final SamgodsConstants.ShipmentSize sizeClass,
			final TransportChain transportChain, final double singleInstanceUtility) {
		this.annualShipment = annualShipment;
		this.sizeClass = sizeClass;
		this.transportChain = transportChain;
		this.singleInstanceUtility = singleInstanceUtility;
	}

	public ChainAndShipmentSize createSingleInstance() {
		return new ChainAndShipmentSize(this.annualShipment.createSingleInstance(), this.sizeClass, this.transportChain, this.singleInstanceUtility);
	}
	
}
