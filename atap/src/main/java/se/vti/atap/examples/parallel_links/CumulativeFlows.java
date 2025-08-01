/**
 * se.vti.atap.examples.parallel_links
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.atap.examples.parallel_links;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

/**
 * 
 * @author GunnarF
 *
 */
public class CumulativeFlows {

	private final List<Double> times_s = new LinkedList<>();
	private final List<Id<Link>> linkIds = new LinkedList<>();

	void add(double time_s, Id<Link> linkId) {
		this.times_s.add(time_s);
		this.linkIds.add(linkId);
	}

	void clear() {
		this.times_s.clear();
		this.linkIds.clear();
	}

	void writeToFile(String fileName) {
		List<Id<Link>> allLinkIds = new ArrayList<>(this.linkIds.stream().collect(Collectors.toSet()));
		Map<Id<Link>, Integer> linkId2cumulative = new LinkedHashMap<>();
		try {
			PrintWriter writer = new PrintWriter(fileName);
			writer.println(
					"time[s]\t" + allLinkIds.stream().map(id -> id.toString()).collect(Collectors.joining("\t")));
			writer.println("0.0\t" + allLinkIds.stream().map(id -> "0").collect(Collectors.joining("\t")));
			for (int i = 0; i < this.times_s.size(); i++) {
				linkId2cumulative.compute(this.linkIds.get(i), (l, c) -> c == null ? 1 : c + 1);
				writer.println(this.times_s.get(i) + "\t"
						+ allLinkIds.stream().map(id -> Integer.toString(linkId2cumulative.getOrDefault(id, 0)))
								.collect(Collectors.joining("\t")));
			}
			writer.flush();
			writer.close();
		} catch (FileNotFoundException e) {
			throw new RuntimeException();
		}
	}

}
