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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

/**
 * 
 * @author GunnarF
 *
 */
public class MiscUtils {

	private MiscUtils() {
	}
	
	public static void ensureEmptyFolder(String folderName) {
		File folder = new File(folderName);
		if (folder.exists()) {
			try {
				FileUtils.deleteDirectory(folder);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		boolean created = folder.mkdirs();
		if (!created) {
			throw new RuntimeException("Could not create folder " + folder);
		}
	}

	private static <K, N extends Number> List<Map.Entry<K, N>> getSortedEntryList(Map<K, N> map, int sortCompSgn) {
		final List<Map.Entry<K, N>> entryList = new ArrayList<>(map.entrySet());
		Collections.sort(entryList, new Comparator<Map.Entry<?, N>>() {
			@Override
			public int compare(Entry<?, N> e1, Entry<?, N> e2) {
				return sortCompSgn * Double.compare(e1.getValue().doubleValue(), e2.getValue().doubleValue());
			}
		});
		return entryList;
	}

	public static <K, N extends Number> List<Map.Entry<K, N>> getSortedEntryListSmallestFirst(Map<K, N> map) {
		return getSortedEntryList(map, 1);
	}

	public static <K, N extends Number> List<Map.Entry<K, N>> getSortedEntryListLargestFirst(Map<K, N> map) {
		return getSortedEntryList(map, -1);
	}
}
