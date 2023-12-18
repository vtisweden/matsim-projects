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
package org.matsim.contrib.greedo.shouldbeelsewhere;

import java.io.File;

import org.apache.commons.io.FileUtils;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class Hacks {

	public static void append2file(File file, String line) {
		try {
			FileUtils.writeStringToFile(file, line, true);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void append2file(String fileName, String line) {
		append2file(new File(fileName), line);
	}

}
