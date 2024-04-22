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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChain {

	private final LinkedList<TransportEpisode> episodes = new LinkedList<>();

	public TransportChain() {
	}

	public void addEpisode(final TransportEpisode episode) {
		if (this.episodes.size() > 0) {
			if (!this.episodes.getLast().getUnloadingNode().equals(episode.getLoadingNode())) {
				throw new IllegalArgumentException();
			}
		}
		this.episodes.add(episode);
	}

//	public List<Id<Node>> createInternalNodeIds() {
//		List<Id<Node>> result = new ArrayList<>(this.episodes.size() - 1);
//		for (int i = 0; i < this.episodes.size() - 2; i++) {
//			result.add(this.episodes.get(i).getDestination());
//		}
//		return result;
//	}
//
//	public int getInternalNodeCnt() {
//		return this.episodes.size() - 1;
//	}

	public LinkedList<TransportEpisode> getEpisodes() {
		return this.episodes;
	}

	public Id<Node> getOrigin() {
		return this.episodes.get(0).getLoadingNode();
	}

	public Id<Node> getDestination() {
		return this.episodes.getLast().getUnloadingNode();
	}
}
