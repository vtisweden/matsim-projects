/**
 * se.vti.samgods.utils
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
package se.vti.samgods.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @author GunnarF
 *
 */
public class MiscUtils {

	private MiscUtils() {
	}

	public static <K, N extends Number> List<Map.Entry<K, N>> getSortedEntryListLargestFirst(Map<K, N> map) {
		final List<Map.Entry<K, N>> entryList = new ArrayList<>(map.entrySet());
		Collections.sort(entryList, new Comparator<Map.Entry<?, N>>() {
			@Override
			public int compare(Entry<?, N> e1, Entry<?, N> e2) {
				return -Double.compare(e1.getValue().doubleValue(), e2.getValue().doubleValue());
			}
		});
		return entryList;
	}

	public static void main(String[] args) {

		Map<String, Integer> name2age = new LinkedHashMap<>();
		name2age.put("Albert", 54);
		name2age.put("Bert", 12);
		name2age.put("Carsten", 40);
		name2age.put("Doris", 32);

		for (Map.Entry<?, ?> e : getSortedEntryListLargestFirst(name2age)) {
			System.out.println(e);
		}

	}

}
