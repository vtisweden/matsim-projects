/**
 * se.vti.matsim.dynameq
 * 
 * Copyright (C) 2023 by Filip Kristofferson (VTI) and Gunnar Flötteröd (VTI, LiU).
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
package se.vti.matsim.dynameq.population;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * 
 * @author FilipK
 *
 */
public class MatrixFileCleaner {

	public void removeRowsUntilAndIncluding(String inputFile, String outputFile, String targetString) {
		File file = new File(inputFile);
		File out = new File(outputFile);

		try (BufferedReader reader = new BufferedReader(new FileReader(file));
				BufferedWriter writer = new BufferedWriter(new FileWriter(out))) {

			String line;

			while ((line = reader.readLine()) != targetString) {
				if (line.equals(targetString)) {
					break;
				}
			}
			while ((line = reader.readLine()) != null) {
				writer.write(line);
				writer.newLine();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
