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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.SamgodsConstants.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChain {

	// -------------------- MEMBERS --------------------

	private final Commodity commodity;

	private final boolean isContainer;

	private final LinkedList<TransportEpisode> episodes = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	public TransportChain(Commodity commodity, boolean isContainer) {
		this.commodity = commodity;
		this.isContainer = isContainer;
	}

	// -------------------- IMPLEMENTATION --------------------

	public boolean isContainer() {
		return this.isContainer;
	}

	public Commodity getCommodity() {
		return this.commodity;
	}

	public Boolean containsFerry() {
		boolean chainContainsFerry = false;
		boolean chainContainsNull = false;
		for (TransportEpisode episode : this.episodes) {
			final Boolean episodeContainsFerry = episode.containsFerry();
			if (episodeContainsFerry == null) {
				chainContainsNull = true;
			} else {
				chainContainsFerry |= episodeContainsFerry;
			}
		}
		if (chainContainsFerry) {
			return true;
		} else if (chainContainsNull) {
			return null;
		} else {
			return false;
		}
	}

	public OD getOD() {
		if (this.episodes.size() == 0) {
			return null;
		} else {
			return new OD(this.episodes.getFirst().getLoadingNode(), this.episodes.getLast().getUnloadingNode());
		}
	}

	public void addEpisode(final TransportEpisode episode) {
		episode.setParent(this);
		this.episodes.add(episode);
	}

	public Id<Node> getOriginNodeId() {
		if (this.episodes.size() == 0) {
			return null;
		} else {
			return this.episodes.get(0).getLoadingNode();
		}
	}

	public Id<Node> getDestinationNodeId() {
		if (this.episodes.size() == 0) {
			return null;
		} else {
			return this.episodes.getLast().getUnloadingNode();
		}
	}

	public LinkedList<TransportEpisode> getEpisodes() {
		return this.episodes;
	}

	public int getLegCnt() {
		return this.getEpisodes().stream().mapToInt(e -> e.getLegs().size()).sum();
	}

	public List<List<SamgodsConstants.TransportMode>> getTransportModeSequence() {
		return this.episodes.stream().map(e -> e.getLegs().stream().map(l -> l.getMode()).collect(Collectors.toList()))
				.collect(Collectors.toList());
	}

	public boolean isRouted() {
		for (TransportEpisode episode : this.episodes) {
			if (!episode.isRouted()) {
				return false;
			}
		}
		return true;
	}

	public Set<Id<Node>> getLoadingTransferUnloadingNodesSet() {
		final Set<Id<Node>> result = new LinkedHashSet<>();
		this.episodes.stream().flatMap(e -> e.getLegs().stream()).forEach(l -> {
			result.add(l.getOrigin());
			result.add(l.getDestination());
		});
		return result;
	}

	// -------------------- OVERRIDING OF Object --------------------

	private List<Object> asList() {
		return Arrays.asList(this.commodity, this.isContainer, this.episodes);
	}

	@Override
	public int hashCode() {
		return this.asList().hashCode();
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof TransportChain)) {
			return false;
		} else {
			return this.asList().equals(((TransportChain) other).asList());
		}
	}
}
