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
package org.matsim.contrib.greedo.deprecated;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.network.io.MatsimNetworkReader;

public class PotentialBottlenecks {

	private final Map<Id<Link>, ? extends Link> allLinks;

	private final Map<Id<Link>, Link> upstreamBottlenecks = new LinkedHashMap<>();

	private final Map<Id<Link>, Link> downstreamBottlenecks = new LinkedHashMap<>();

	private final Map<Id<Link>, Link> combinedBottlenecks = new LinkedHashMap<>();

	public PotentialBottlenecks(final Network network, final double threshold) {

		this.allLinks = network.getLinks();

		for (Link link : network.getLinks().values()) {

			final boolean isUpstreamBottleneck = (link.getCapacity() < threshold
					* link.getFromNode().getInLinks().values().stream().mapToDouble(l -> l.getCapacity()).sum());
			final boolean isDownstreamBottleneck = (link.getToNode().getOutLinks().values().stream()
					.mapToDouble(l -> l.getCapacity()).min().getAsDouble() < threshold
							* link.getToNode().getInLinks().values().stream().mapToDouble(l -> l.getCapacity()).sum());

			if (isUpstreamBottleneck) {
				this.upstreamBottlenecks.put(link.getId(), link);
			}
			if (isDownstreamBottleneck) {
				this.downstreamBottlenecks.put(link.getId(), link);
			}
			if (isUpstreamBottleneck && isDownstreamBottleneck) {
				this.combinedBottlenecks.put(link.getId(), link);
			}
		}
	}

	public Map<Id<Link>, ? extends Link> getBottleneckLinks(boolean requireUpstreamBottlenecks,
			boolean requireDownstreamBottlenecks) {
		if (requireUpstreamBottlenecks) {
			if (requireDownstreamBottlenecks) {
				return this.combinedBottlenecks;
			} else {
				return this.upstreamBottlenecks;
			}
		} else {
			if (requireDownstreamBottlenecks) {
				return this.downstreamBottlenecks;
			} else {
				return this.allLinks;
			}
		}
	}

	public static void main(String[] args) {
		Network network = NetworkUtils.createNetwork();
		MatsimNetworkReader reader = new MatsimNetworkReader(network);
		reader.readFile("network.xml");
		System.out.println("threshold\ttotal\tupstream\tdownstream\toverall");
		for (double threshold = 0.0; threshold <= 1.01; threshold += 0.05) {
			PotentialBottlenecks pb = new PotentialBottlenecks(network, threshold);
			System.out.println(threshold + "\t" + pb.getBottleneckLinks(false, false).size() + "\t"
					+ pb.getBottleneckLinks(true, false).size() + "\t" + pb.getBottleneckLinks(false, true).size()
					+ "\t" + pb.getBottleneckLinks(true, true).size());

		}
	}

}
