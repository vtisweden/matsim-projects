/**
 * instances.vienna
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
package estimation;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import floetteroed.utilities.Tuple;
import floetteroed.utilities.tabularfileparser.AbstractTabularFileHandlerWithHeaderLine;
import floetteroed.utilities.tabularfileparser.TabularFileParser;

/**
 * 
 * @author GunnarF
 *
 */
public class MatrixDataReader {

	private MatrixDataReader() {
	}

	public static Map<Tuple<String, String>, Double> read(String fileName) {
		Map<Tuple<String, String>, Double> data = new LinkedHashMap<>();

		AbstractTabularFileHandlerWithHeaderLine handler = new AbstractTabularFileHandlerWithHeaderLine() {
			@Override
			public void startDataRow(final String[] row) {
				String rowLabel = row[0];
				for (Map.Entry<String, Integer> e : this.label2index.entrySet()) {
					if (e.getValue() > 0 && row[e.getValue()].length() > 0) {
						String columnLabel = e.getKey();
						double value = Double.parseDouble(row[e.getValue()]);
						data.put(new Tuple<>(rowLabel, columnLabel), value);
					}
				}
			}
		};

		TabularFileParser parser = new TabularFileParser();
		parser.setDelimiterTags(new String[] { "," });
		parser.setOmitEmptyColumns(false);
		try {
			parser.parse(fileName, handler);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return data;
	}
}
