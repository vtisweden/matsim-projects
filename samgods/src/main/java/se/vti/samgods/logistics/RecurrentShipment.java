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
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class RecurrentShipment {

	// -------------------- CONSTANTS --------------------

	private final Commodity commodity;

	private final TransportChain transportChain;

	private final double size_ton;

	private final double frequency_1_yr;

	// -------------------- CONSTRUCTION --------------------

	public RecurrentShipment(Commodity commodity, TransportChain transportChain, double size_ton, double frequency_1_yr) {
		this.commodity = commodity;
		this.transportChain = transportChain;
		this.size_ton = size_ton;
		this.frequency_1_yr = frequency_1_yr;
	}

	// -------------------- GETTERS --------------------

	public Commodity getCommmodity() {
		return this.commodity;
	}

	public TransportChain getTransportChain() {
		return this.transportChain;
	}

	public List<TransportMode> getModeSequence() {
		final List<TransportMode> result = new ArrayList<>(this.transportChain.getLegs().size());
		for (TransportLeg leg : this.transportChain.getLegs()) {
			result.add(leg.getMode());
		}
		return result;
	}

	public double getSize_ton() {
		return this.size_ton;
	}

	public double getFrequency_1_yr() {
		return this.frequency_1_yr;
	}
}
