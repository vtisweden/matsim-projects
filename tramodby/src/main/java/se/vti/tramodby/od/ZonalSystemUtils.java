/**
 * se.vti.tramodby
 * 
 * Copyright (C) 2023 Gunnar Flötteröd (VTI, LiU), Rasmus Ringdahl (LiU). 
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
package se.vti.tramodby.od;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.math.MathHelpers;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;
import se.vti.tramodby.module.TramodByConfigGroup;
import se.vti.tramodby.od.ZonalSystem.Zone;

/**
 * This class contains utility methods for the ZonalSystem class.
 * 
 */
public class ZonalSystemUtils {

	/**
	 * This method creates a ZonalSystem object from the file defined by the zoneDefinitionFile attribute
	 * in the Tramod By config group.
	 *  
	 * @param tramodByConfig - the config group
	 * @param network - the MATSim network
	 * @return ZonalSystem
	 * 
	 * @throws IOException
	 */
	public static ZonalSystem createZonalSystemFromFile(TramodByConfigGroup tramodByConfig, Network network)
			throws IOException {
		// Creates a new ZonalSystem object.
		final ZonalSystem zonalSystem = new ZonalSystem();
		
		// Creates a handler for parsing the zone definition file.
		AbstractTabularFileHandlerWithHeaderLine handler = new AbstractTabularFileHandlerWithHeaderLine() {
			@Override
			public void startCurrentDataRow() {
				
				final Id<Link> linkId = Id.createLinkId(this.getStringValue("linkID"));
				if (network.getLinks().containsKey(linkId)) {
					
					// Adds the Zone to the ZonalSystem.
					final Id<ZonalSystem.Zone> zoneId = Id.create(this.getStringValue("bsu"), ZonalSystem.Zone.class);
					final String zoneName = this.getStringValue("bsuname");
					zonalSystem.add(linkId, zoneId, zoneName);
				}
			}
		};
		
		// Setting parser parameters.
		TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		
		// Parses the zonal definition file.
		parser.parse(tramodByConfig.getZoneDefinitionFile(), handler);
		return zonalSystem;
	}

	/**
	 * This class handles random draw of a link within a zone. 
	 *
	 */
	public static class LinkFromZoneSampler {

		private Random rnd;
		private Map<Id<Zone>, Map<Id<Link>, Double>> zoneId2linkId2weight = new LinkedHashMap<>();
		private Map<Id<Zone>, Double> zoneId2weightSum = new LinkedHashMap<>();

		/**
		 * This is the private constructor of the LinkFromZoneSampler.
		 * @param zonalSystem - the zonal system
		 * @param network - the MATSim network
		 * @param rnd - random generator
		 */
		private LinkFromZoneSampler(ZonalSystem zonalSystem, Network network, Random rnd) {
			this.rnd = rnd;
			
			// Adds each link and it's corresponding zone to a maps for the random draw.
			for (Zone zone : zonalSystem.getAllZones().values()) {
				final Map<Id<Link>, Double> linkId2weight = new LinkedHashMap<>();
				for (Id<Link> linkId : zone.getLinkIds()) {
					final Link link = network.getLinks().get(linkId);
					
					// Weighting each link by length and number of lanes.
					if (link != null) {
						final double weight = link.getLength() * link.getNumberOfLanes();
						linkId2weight.put(link.getId(), weight);
					}
				}
				this.zoneId2linkId2weight.put(zone.getId(), linkId2weight);
				this.zoneId2weightSum.put(zone.getId(), linkId2weight.values().stream().mapToDouble(w -> w).sum());
			}
		}

		/**
		 * This method draws the id of a random link within a specific zone.
		 *  
		 * @param zoneId - the specified zone
		 * @return Id of the link
		 */
		public Id<Link> drawLinkId(Id<Zone> zoneId) {
			// Returns null if the zone does not exist or has no links.
			if ((this.zoneId2weightSum.get(zoneId) == null) || (this.zoneId2weightSum.get(zoneId) == 0.0)) {
				return null;
			} else {
				// Draws a random link.
				return MathHelpers.draw(this.zoneId2linkId2weight.get(zoneId), this.zoneId2weightSum.get(zoneId),
						this.rnd);
			}
		}

		/**
		 * This method draws a list of random link ids from a zone.
		 *  
		 * @param zoneId - the specific zone id
		 * @param sampleCnt - number of samples
		 * @return list of link id's
		 */
		public List<Id<Link>> drawLowVariance(Id<Zone> zoneId, int sampleCnt) {
			// Create a list of candidate link id's.
			final List<Tuple<Id<Link>, Double>> candidates = new ArrayList<>(this.zoneId2linkId2weight.get(zoneId)
																									  .size());
			
			// Populates the candidate list with link id's and the corresponding link weight.  
			for (Map.Entry<Id<Link>, Double> entry : this.zoneId2linkId2weight.get(zoneId).entrySet()) {
				candidates.add(new Tuple<>(entry.getKey(), entry.getValue()));
			}
			
			// Shuffles and draws a number of link id's.
			Collections.shuffle(candidates);
			return ZonalSystemUtils.drawLowVariance(candidates, sampleCnt);
		}

	}

	/**
	 * This is the public constructor of the LinkFromZoneSampler.
	 * 
	 * @param zonalSystem - the zonal system
	 * @param network - the MATSim network
	 * @param rnd - random generator
	 * 
	 * @return Zonal link sampler
	 */
	public static LinkFromZoneSampler createLinkFromZoneSampler(ZonalSystem zonalSystem, Network network, Random rnd) {
		return new LinkFromZoneSampler(zonalSystem, network, rnd);
	}

	// TODO this should become a utility
	public static <T> List<T> drawLowVariance(final List<Tuple<T, Double>> objectsAndWeights, final int sampleCnt) {
		// Calculates the sum of all weights for the candidate links.
		final double weightSum = objectsAndWeights.stream().mapToDouble(Tuple::getB).sum();
		
		// Returns an empty list if the zones has no weight.
		if (weightSum < 1e-8) {
			return Collections.emptyList();
		}
		
		// Initializing output result and calculation variables.
		final List<T> result = new ArrayList<>(sampleCnt);
		final double deltaWeight = weightSum / sampleCnt;
		int candIndex = 0;
		double weightSumSoFar = 0.0;
		// Finds a suitable links from the candidate links.
		for (double targetWeightSum = 0.5 * deltaWeight; targetWeightSum < weightSum; targetWeightSum += deltaWeight) {
			while (weightSumSoFar + objectsAndWeights.get(candIndex).getB() < targetWeightSum) {
				weightSumSoFar += objectsAndWeights.get(candIndex).getB();
				candIndex++;
			}
			result.add(objectsAndWeights.get(candIndex).getA());
		}
		return result;
	}

	// for testing only
	public static void main(String[] args) {
		List<Tuple<String, Double>> tuples = new ArrayList<>(3);
		tuples.add(new Tuple<>("one", 1.0));
		tuples.add(new Tuple<>("two", 2.0));
		tuples.add(new Tuple<>("three", 3.0));

		System.out.println("one\ttwo\tthree");
		for (int cnt = 1; cnt < 100; cnt++) {
			Collections.shuffle(tuples);
			List<String> samples = drawLowVariance(tuples, cnt);
			Map<String, Double> counts = new LinkedHashMap<>();
			counts.put("one", 0.0);
			counts.put("two", 0.0);
			counts.put("three", 0.0);
			for (String sample : samples) {
				counts.put(sample, 1.0 + counts.get(sample));
			}
			System.out.println(
					counts.get("one") / cnt + "\t" + counts.get("two") / cnt + "\t" + counts.get("three") / cnt);
		}
	}
}
