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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants.TransportMode;

public class TransportChain {

	private final LinkedList<TransportLeg> legs = new LinkedList<>();

	public TransportChain() {
	}

	public void addLeg(final TransportLeg leg) {
		if (this.legs.size() > 0) {
			if (!this.legs.getLast().getDestination().equals(leg.getOrigin())) {
				throw new IllegalArgumentException();
			}
		}
		this.legs.add(leg);
	}

	public List<TransportLeg> getLegs() {
		return this.legs;
	}

	public Id<Node> getOrigin() {
		return this.legs.get(0).getOrigin();
	}

	public Id<Node> getDestination() {
		return this.legs.get(this.legs.size() - 1).getDestination();
	}

	// FIXME Simplifies away transshipments within the same mode.
//	public void mergeLegs() {
//		if (this.legs.size() == 0) {
//			return;
//		}
//		final LinkedList<TransportLeg> mergedLegs = new LinkedList<>();
//
//		TransportMode currentMode = this.legs.getFirst().getMode();
//		Id<Node> currentOrigin = this.legs.getFirst().getOrigin();
//		for (TransportLeg nextLeg : this.legs) {
//			if (!nextLeg.getMode().equals(currentMode)) {
//				mergedLegs.add(new TransportLeg(new OD(currentOrigin, nextLeg.getOrigin()), currentMode));
//				currentOrigin = nextLeg.getOrigin();
//				currentMode = nextLeg.getMode();
//			}
//		}
//		mergedLegs.add(new TransportLeg(new OD(currentOrigin, this.legs.getLast().getDestination()),
//				this.legs.getLast().getMode()));
//		
//		this.legs.clear();
//		this.legs.addAll(mergedLegs);
//	}

}
