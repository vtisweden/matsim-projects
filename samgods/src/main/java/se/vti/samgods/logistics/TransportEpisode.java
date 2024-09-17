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

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportEpisode {

	// -------------------- MEMBERS --------------------

	private final TransportMode mode;

	private final LinkedList<OD> legODs = new LinkedList<>();

	private TransportChain parent;

	private List<ConsolidationUnit> consolidationUnits = null;

	// -------------------- CONSTRUCTION/COMPOSITION --------------------

	public TransportEpisode(TransportMode mode) {
		this.mode = mode;
	}

	/* package */ void setParent(TransportChain parent) {
		this.parent = parent;
	}

	public void addLegOD(OD od) {
		this.legODs.add(od);
	}

	public void setConsolidationUnits(List<ConsolidationUnit> consolidationUnits) {
		this.consolidationUnits = consolidationUnits;
	}

	// -------------------- IMPLEMENTATION --------------------

	public TransportMode getMode() {
		return this.mode;
	}

	public LinkedList<OD> getLegODs() {
		return this.legODs;
	}

	public Id<Node> getLoadingNodeId() {
		if (this.legODs.size() == 0) {
			return null;
		} else {
			return this.legODs.getFirst().origin;
		}
	}

	public Id<Node> getUnloadingNodeId() {
		if (this.legODs.size() == 0) {
			return null;
		} else {
			return this.legODs.getLast().destination;
		}
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

	public List<ConsolidationUnit> getConsolidationUnits() {
		return this.consolidationUnits;
	}

	public boolean hasSignatures() {
		return (this.consolidationUnits != null);
	}

	public boolean isRouted() {
		return (this.hasSignatures()
				&& (this.consolidationUnits.stream().allMatch(s -> (s != null) && (s.linkIds != null))));
	}
}
