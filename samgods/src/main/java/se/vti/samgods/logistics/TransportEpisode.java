/**
 * se.vti.samgods.logistics
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
package se.vti.samgods.logistics;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.external.ntmcalc.VehicleEpisode2NTMCalcSerializer;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportEpisode {

	private final TransportMode mode;

	private final LinkedList<TransportLeg> legs = new LinkedList<>();

	public TransportEpisode(TransportMode mode) {
		this.mode = mode;
	}

	public void addLeg(final TransportLeg leg) {
		this.legs.add(leg);
	}

	public LinkedList<TransportLeg> getLegs() {
		return this.legs;
	}

	public TransportMode getMode() {
		return this.mode;
	}

	public Id<Node> getLoadingNode() {
		return this.legs.getFirst().getOrigin();
	}

	public Id<Node> getUnloadingNode() {
		return this.legs.getLast().getDestination();
	}

	public int getTransferNodeCnt() {
		return this.legs.size() - 1;
	}
	
	private List<Id<Node>> createNodeList(boolean onlyTransfers) {
		LinkedList<Id<Node>> result = new LinkedList<>();
		if (!onlyTransfers) {
			result.add(this.legs.getFirst().getOrigin());
		}
		for (TransportLeg leg : this.legs) {
			result.add(leg.getDestination());
		}
		if (onlyTransfers) {
			result.removeLast();
		}
		return result;
	}

	public List<Id<Node>> createTransferNodesList() {
		return this.createNodeList(true);
	}

	public List<Id<Node>> createAllNodesList() {
		return this.createNodeList(false);
	}
}
