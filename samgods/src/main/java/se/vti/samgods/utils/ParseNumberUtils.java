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

/**
 * 
 * @author GunnarF
 *
 */
public class ParseNumberUtils {

	private ParseNumberUtils() {
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
}
