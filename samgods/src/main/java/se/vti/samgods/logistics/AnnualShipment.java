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

import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
@JsonSerialize(using = AnnualShipmentJsonSerializer.class)
public class AnnualShipment {

	// -------------------- CONSTANTS --------------------

	private final Commodity commodity;

	private final TransportChain transportChain;

	private final double totalAmount_ton;

	// -------------------- CONSTRUCTION --------------------

	public AnnualShipment(Commodity commodity, TransportChain transportChain, double totalAmount_ton) {
		this.commodity = commodity;
		this.transportChain = transportChain;
		this.totalAmount_ton = totalAmount_ton;
	}

	// -------------------- GETTERS --------------------

	public Commodity getCommmodity() {
		return this.commodity;
	}

	public TransportChain getTransportChain() {
		return this.transportChain;
	}

	public List<TransportMode> getModeSequence() {
		final List<TransportMode> result = new LinkedList<>();
		for (TransportEpisode episode : this.transportChain.getEpisodes()) {
			for (TransportLeg leg : episode.getLegs()) {
				result.add(leg.getMode());
			}
		}
		return result;
	}

	public double getTotalAmount_ton() {
		return this.totalAmount_ton;
	}

//	public double getFrequency_1_yr() {
//		return this.frequency_1_yr;
//	}
}
