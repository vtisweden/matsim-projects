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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportEpisode {

	// -------------------- MEMBERS --------------------

	private final TransportMode mode;

	private final LinkedList<TransportLeg> legs = new LinkedList<>();

	private TransportChain parent;

	// -------------------- CONSTRUCTION --------------------

	public TransportEpisode(TransportMode mode) {
		this.mode = mode;
	}

	// -------------------- IMPLEMENTATION --------------------

	void setParent(TransportChain parent) {
		this.parent = parent;
	}

	public OD getOD() {
		return new OD(this.getLoadingNode(), this.getUnloadingNode());
	}

	public Boolean containsFerry() {
		boolean episodeContainsFerry = false;
		boolean episodeContainsNull = false;
		for (TransportLeg leg : this.legs) {
			final Boolean legContainsFerry = leg.containsFerry();
			if (legContainsFerry == null) {
				episodeContainsNull = true;
			} else {
				episodeContainsFerry |= legContainsFerry;
			}
		}
		if (episodeContainsFerry) {
			return true;
		} else if (episodeContainsNull) {
			return null;
		} else {
			return false;
		}
	}

	public void addLeg(final TransportLeg leg) {
		leg.setParent(this);
		this.legs.add(leg);
	}

	public LinkedList<TransportLeg> getLegs() {
		return this.legs;
	}

	public TransportMode getMode() {
		return this.mode;
	}

	public Commodity getCommodity() {
		if (this.parent == null) {
			return null;
		} else {
			return parent.getCommodity();
		}
	}

	public Boolean isContainer() {
		if (this.parent == null) {
			return null;
		} else {
			return this.parent.isContainer();
		}
	}

	public Id<Node> getLoadingNode() {
		if (this.legs.size() == 0) {
			return null;
		} else {
			return this.legs.getFirst().getOrigin();
		}
	}

	public Id<Node> getUnloadingNode() {
		if (this.legs.size() == 0) {
			return null;
		} else {
			return this.legs.getLast().getDestination();
		}
	}

	public Integer getTransferNodeCnt() {
		if (this.legs.size() == 0) {
			return null;
		} else {
			return this.legs.size() - 1;
		}
	}

	private List<Id<Node>> createNodeList(boolean onlyTransfers) {
		if (this.legs == null) {
			return null;
		} else if (this.legs.size() == 0) {
			return Arrays.asList(this.getOD().origin, this.getOD().destination);
		} else {
			final LinkedList<Id<Node>> result = new LinkedList<>();
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
	}

	public List<Id<Node>> createTransferNodesList() {
		return this.createNodeList(true);
	}

	public List<Id<Node>> createLoadingTransferUnloadingNodeList() {
		return this.createNodeList(false);
	}

	public Boolean isRouted() {
		if (this.legs == null) {
			return null;
		} else {
			for (TransportLeg leg : this.legs) {
				if (!leg.isRouted()) {
					return false;
				}
			}
			return true;
		}
	}

	// -------------------- OVERRIDING OF Object --------------------

	private List<Object> asList() {
		return Arrays.asList(this.getCommodity(), this.isContainer(), this.mode, this.legs);
	}

	@Override
	public int hashCode() {
		return this.asList().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof TransportEpisode)) {
			return false;
		} else {
			return this.asList().equals(((TransportEpisode) other).asList());
		}
	}
}
