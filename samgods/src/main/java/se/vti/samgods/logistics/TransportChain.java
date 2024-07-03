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
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChain {

	private final OD od;

	private final LinkedList<TransportEpisode> episodes = new LinkedList<>();

	public TransportChain(OD od) {
		this.od = od;
	}
	
	public OD getOD() {
		return this.od;
	}

	public List<List<List<Id<Link>>>> getRoutesView() {
		return this.episodes.stream().map(e -> e.getRoutesView()).collect(Collectors.toList());
	}

	public void addEpisode(final TransportEpisode episode, boolean checkConnectivity) {
		if (checkConnectivity && this.episodes.size() > 0) {
			if (!this.episodes.getLast().getUnloadingNode().equals(episode.getLoadingNode())) {
				throw new IllegalArgumentException();
			}
		}
		this.episodes.add(episode);
	}

	public Id<Node> getOriginNodeId() {
		return this.episodes.get(0).getLoadingNode();
	}

	public Id<Node> getDestinationNodeId() {
		return this.episodes.getLast().getUnloadingNode();
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

}
