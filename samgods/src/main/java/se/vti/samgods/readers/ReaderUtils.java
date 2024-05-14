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
package se.vti.samgods.readers;

import java.util.LinkedHashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import de.vandermeer.asciitable.AsciiTable;
import floetteroed.utilities.Units;

/**
 * 
 * @author GunnarF
 *
 */
public class ReaderUtils {

	private ReaderUtils() {
	}

	public static Double parseDoubleOrDefault(String str, Double defaultValue) {
		if (str == null || "".equals(str)) {
			return defaultValue;
		} else {
			return Double.parseDouble(str);
		}
	}

	public static Double parseDoubleOrNull(String str) {
		return parseDoubleOrDefault(str, null);
	}

	public static Integer parseIntOrDefault(String str, Integer defaultValue) {
		if (str == null || "".equals(str)) {
			return defaultValue;
		} else {
			return Integer.parseInt(str);
		}
	}

	public static Integer parseIntOrNull(String str) {
		return parseIntOrDefault(str, null);
	}

	public static String createNetworkStatsTable(Network network) {

		StringBuffer result = new StringBuffer();

		Map<String, Integer> mode2cnt = new LinkedHashMap<>();
		Map<String, Integer> mode2speed1cnt = new LinkedHashMap<>();
		Map<String, Integer> mode2speed2cnt = new LinkedHashMap<>();

		Map<String, Double> mode2speed1Sum = new LinkedHashMap<>();
		Map<String, Double> mode2speed2Sum = new LinkedHashMap<>();
		Map<String, Double> mode2lengthSum = new LinkedHashMap<>();
		for (Link link : network.getLinks().values()) {
			mode2cnt.compute(link.getAllowedModes().iterator().next(), (m, c) -> c == null ? 1 : c + 1);
			SamgodsLinkAttributes attr = (SamgodsLinkAttributes) link.getAttributes()
					.getAttribute(SamgodsLinkAttributes.ATTRIBUTE_NAME);
			if (attr.speed1_km_h != null && attr.speed1_km_h != 0) {
				mode2speed1Sum.compute(link.getAllowedModes().iterator().next(),
						(m, s) -> s == null ? attr.speed1_km_h : s + attr.speed1_km_h);
				mode2speed1cnt.compute(link.getAllowedModes().iterator().next(), (m, c) -> c == null ? 1 : c + 1);
			}
			if (attr.speed2_km_h != null && attr.speed2_km_h != 0) {
				mode2speed2Sum.compute(link.getAllowedModes().iterator().next(),
						(m, s) -> s == null ? attr.speed2_km_h : s + attr.speed2_km_h);
				mode2speed2cnt.compute(link.getAllowedModes().iterator().next(), (m, c) -> c == null ? 1 : c + 1);
			}
			mode2lengthSum.compute(link.getAllowedModes().iterator().next(),
					(m, l) -> l == null ? link.getLength() : l + link.getLength());
		}
		final AsciiTable table = new AsciiTable();
		table.addRule();
		table.addRow("Mode", "Links", "Avg. speed 1 [km/h] if >0", "Avg. speed 2 [km/h] if >0", "Avg. length [km]");
		table.addRule();
		for (Map.Entry<String, Integer> e : mode2cnt.entrySet()) {
			table.addRow(e.getKey(), e.getValue(), mode2speed1Sum.get(e.getKey()) / mode2speed1cnt.get(e.getKey()),
					mode2speed2Sum.getOrDefault(e.getKey(), 0.0) / mode2speed2cnt.getOrDefault(e.getKey(), 1),
					Units.KM_PER_M * mode2lengthSum.get(e.getKey()) / e.getValue());
			table.addRule();
		}
		result.append(table.render());

		return result.toString();
	}

}
