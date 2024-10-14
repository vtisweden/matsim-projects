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
package se.vti.samgods.calibration.ascs;

import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.utils.misc.iterationlogging.LogEntry;
import se.vti.utils.misc.iterationlogging.LogWriter;

/**
 * 
 * @author GunnarF
 *
 */
class ASCLogger {

	private final LogWriter<TransportWorkAscCalibrator> vehicleASCWriter;
	private final LogWriter<TransportWorkAscCalibrator> railCommodityASCWriter;
	private final LogWriter<TransportWorkAscCalibrator> modeASCWriter;

	ASCLogger(Vehicles vehicles) {

		this.vehicleASCWriter = new LogWriter<>("./results/calibratedASCs/vehicleGroupASCs.txt", false);
		this.railCommodityASCWriter = new LogWriter<>("./results/calibratedASCs/railASCs.txt", false);
		this.modeASCWriter = new LogWriter<>("./results/calibratedASCs/modeASCs.txt", false);

		for (VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
			this.vehicleASCWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return vehicleType.getId().toString();
				}

				@Override
				public String value(TransportWorkAscCalibrator fleetCalibrator) {
					return LogEntry
							.toString(fleetCalibrator.createASCs().getVehicleTyp2ASC().getOrDefault(vehicleType, 0.0));
				}
			});
		}

		for (Commodity commodity : Commodity.values()) {
			this.railCommodityASCWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return commodity.toString();
				}

				@Override
				public String value(TransportWorkAscCalibrator fleetCalibrator) {
					return LogEntry
							.toString(fleetCalibrator.createASCs().getRailCommodity2ASC().getOrDefault(commodity, 0.0));
				}
			});
		}
		for (TransportMode mode : TransportMode.values()) {
			this.modeASCWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return mode.toString();
				}

				@Override
				public String value(TransportWorkAscCalibrator fleetCalibrator) {
					return LogEntry.toString(fleetCalibrator.createASCs().getMode2ASC().getOrDefault(mode, 0.0));
				}
			});
		}

	}

	public void log(TransportWorkAscCalibrator fleetCalibr) {
		this.vehicleASCWriter.writeToFile(fleetCalibr);
		this.railCommodityASCWriter.writeToFile(fleetCalibr);
		this.modeASCWriter.writeToFile(fleetCalibr);
	}
}
