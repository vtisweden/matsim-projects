/**
 * org.matsim.contrib.emulation
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
package se.vti.samgods.io;

import java.util.LinkedList;
import java.util.List;

import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportLeg;

public class TransportChainImpl implements TransportChain {

	private final LinkedList<TransportLeg> legs = new LinkedList<>();
	
	public TransportChainImpl() {
		
	}
	
	public void addLeg(final TransportLeg leg) {
		if (this.legs.size() > 0) {
			if (!this.legs.getLast().getDestination().equals(leg.getOrigin())) {
				throw new IllegalArgumentException();
			}
		}
		this.legs.add(leg);
	}
	
	@Override
	public List<TransportLeg> getLegs() {
		return this.legs;
	}

}
