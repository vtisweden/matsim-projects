/**
 * se.vti.samgods.calibration
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
package se.vti.samgods.calibration;

import se.vti.utils.misc.iterationlogging.LogEntry;
import se.vti.utils.misc.iterationlogging.LogWriter;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetCalibrationLogger {

	private final LogWriter<FleetCostCalibrator> groupErrorWriter;
	private final LogWriter<FleetCostCalibrator> groupASCWriter;

	public FleetCalibrationLogger() {

		this.groupErrorWriter = new LogWriter<>("vehicleGroupErrors.txt", false);
		this.groupASCWriter = new LogWriter<>("vehicleGroupASCs.txt", false);
		for (FleetCostCalibrator.Group group : FleetCostCalibrator.Group.values()) {
			this.groupErrorWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return group.toString();
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					if (fleetCalibrator.group2lastNormalizedRealized == null) {
						return "";
					} else {
						return LogEntry
								.toString(100.0 * (fleetCalibrator.group2lastNormalizedRealized.getOrDefault(group, 0.0)
										- fleetCalibrator.group2normalizedTarget.get(group)));
					}
				}
			});
			this.groupASCWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return group.toString();
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					return LogEntry.toString(fleetCalibrator.computeGroupASCs().getOrDefault(group, 0.0));
				}
			});
		}
	}

	public void log(FleetCostCalibrator fleetCalibr) {
		this.groupErrorWriter.writeToFile(fleetCalibr);
		this.groupASCWriter.writeToFile(fleetCalibr);
	}
}
