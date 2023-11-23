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

import se.vti.samgods.legacy.Samgods.Commodity;

public class Shipment {

	private final Commodity commodity;

	private final TransportChain transportChain;

	private final double size_ton;

	private final double frequency_1_yr;

	public Shipment(Commodity commodity, TransportChain transportChain, double size_ton, double frequency_1_yr) {
		this.commodity = commodity;
		this.transportChain = transportChain;
		this.size_ton = size_ton;
		this.frequency_1_yr = frequency_1_yr;
	}

	public Commodity getCommmodity() {
		return this.commodity;
	}

	public TransportChain getTransportChain() {
		return this.transportChain;
	}

	public double getSize_ton() {
		return this.size_ton;
	}

	public double getFrequency_1_yr() {
		return this.frequency_1_yr;
	}
}
