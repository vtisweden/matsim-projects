/**
 * se.vti.atap.examples.minimalframework.parallel_links.ods
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
package se.vti.atap.examples.minimalframework.parallel_links.ods;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * 
 * @author GunnarF
 *
 */
public class LogComparisonPrinter<L extends LoggerImpl> {

	private List<String> loggerLabels = new ArrayList<>();
	private List<L> loggers = new ArrayList<>();

	private List<String> statisticLabels = new ArrayList<>();
	private List<Function<DescriptiveStatistics, Double>> statistics = new ArrayList<>();

	public LogComparisonPrinter() {
	}

	public LogComparisonPrinter<L> addLogger(String label, L logger) {
		this.loggerLabels.add(label);
		this.loggers.add(logger);
		return this;
	}

	public LogComparisonPrinter<L> addStatistic(String label, Function<DescriptiveStatistics, Double> statistic) {
		this.statisticLabels.add(label);
		this.statistics.add(statistic);
		return this;
	}

	private void writeToStream(OutputStream out) throws IOException {
		BufferedWriter internalWriter = new BufferedWriter(new OutputStreamWriter(out));

		internalWriter.write("Iteration\t");
		for (String loggerLabel : this.loggerLabels) {
			for (String statisticLabel : this.statisticLabels) {
				internalWriter.write(statisticLabel + "(" + loggerLabel + ")");
				internalWriter.write("\t");
			}
		}
		internalWriter.write("\n");
		out.flush();

		int maxIterations = this.loggers.stream().mapToInt(l -> l.getNumberOfIterations()).max().getAsInt();
		for (int iteration = 0; iteration < maxIterations; iteration++) {
			internalWriter.write(Integer.toString(iteration));
			internalWriter.write("\t");
			for (L logger : this.loggers) {
				var data = logger.getDataOrNull(iteration);
				for (var statistic : this.statistics) {
					internalWriter.write(data == null ? "" : Double.toString(statistic.apply(data)));
					internalWriter.write("\t");
				}
			}
			internalWriter.write("\n");
			out.flush();
		}
		
		internalWriter.close();
	}

	public void printToConsole() {
		try {
			this.writeToStream(System.out);
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}

	public void printToFile(String fileName) {
		try {
			var out = new FileOutputStream(fileName);
			this.writeToStream(out);
			out.flush();
			out.close();
		} catch (IOException e) {
			throw new RuntimeException();
		}
	}
}
