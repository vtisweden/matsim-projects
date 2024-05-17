/**
 * se.vti.samgods.readers
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
package se.vti.samgods.network;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import de.vandermeer.asciitable.AsciiTable;
import floetteroed.utilities.Units;
import se.vti.samgods.utils.ParseNumberUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class SamgodsNetworkUtils {

	private SamgodsNetworkUtils() {
	}

	public static String createNetworkStatsTable(Network network) {

		StringBuffer result = new StringBuffer();

		Map<String, Integer> mode2cnt = new LinkedHashMap<>();
		Map<String, Integer> mode2speed1cnt = new LinkedHashMap<>();
		Map<String, Integer> mode2speed2cnt = new LinkedHashMap<>();

		Map<String, Double> mode2speed1Sum = new LinkedHashMap<>();
		Map<String, Double> mode2speed2Sum = new LinkedHashMap<>();
		Map<String, Double> mode2lengthSum = new LinkedHashMap<>();
		Map<String, Double> mode2lanesSum = new LinkedHashMap<>();

		for (Link link : network.getLinks().values()) {
			String mode = SamgodsLinkAttributes.getSamgodsMode(link).toString();
			mode2cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
			mode2lengthSum.compute(mode, (m, l) -> l == null ? link.getLength() : l + link.getLength());
			mode2lanesSum.compute(mode, (m, l) -> l == null ? link.getNumberOfLanes() : l + link.getNumberOfLanes());
			SamgodsLinkAttributes attr = SamgodsLinkAttributes.getAttrs(link);
			if (attr.speed1_km_h != null && attr.speed1_km_h != 0) {
				mode2speed1Sum.compute(mode, (m, s) -> s == null ? attr.speed1_km_h : s + attr.speed1_km_h);
				mode2speed1cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
			}
			if (attr.speed2_km_h != null && attr.speed2_km_h != 0) {
				mode2speed2Sum.compute(mode, (m, s) -> s == null ? attr.speed2_km_h : s + attr.speed2_km_h);
				mode2speed2cnt.compute(mode, (m, c) -> c == null ? 1 : c + 1);
			}
		}
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Mode", "Links", "Avg. length [km]", "Avg. no. of lanes", "Avg. speed 1 [km/h] if >0",
				"Avg. speed 2 [km/h] if >0");
		table.addRule();
		for (Map.Entry<String, Integer> e : mode2cnt.entrySet()) {
			final String mode = e.getKey();
			final int cnt = e.getValue();
			table.addRow(mode, cnt, ParseNumberUtils.divideOrNothing(Units.KM_PER_M * mode2lengthSum.get(e.getKey()), cnt),
					ParseNumberUtils.divideOrNothing(mode2lanesSum.get(mode), cnt),
					ParseNumberUtils.divideOrNothing(mode2speed1Sum.get(mode), mode2speed1cnt.get(mode)),
					ParseNumberUtils.divideOrNothing(mode2speed2Sum.get(mode), mode2speed2cnt.get(mode)));
			table.addRule();
		}
		result.append(table.render());

		return result.toString();
	}

}
