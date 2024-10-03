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

import org.matsim.vehicles.VehicleType;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.utils.misc.iterationlogging.LogEntry;
import se.vti.utils.misc.iterationlogging.LogWriter;

/**
 * 
 * @author GunnarF
 *
 */
public class FleetCalibrationLogger {

//	private final LogWriter<FleetCostCalibrator> groupErrorWriter;
	private final LogWriter<FleetCostCalibrator> groupASCWriter;
	private final LogWriter<FleetCostCalibrator> gTonKmWriter;
	private final LogWriter<FleetCostCalibrator> gTonKmWriterDetail;

	public FleetCalibrationLogger(Vehicles vehicles) {

//		this.groupErrorWriter = new LogWriter<>("vehicleGroupErrors.txt", false);
		this.groupASCWriter = new LogWriter<>("vehicleGroupASCs.txt", false);
		this.gTonKmWriter = new LogWriter<>("GTonKm.txt", false);

		this.gTonKmWriterDetail = new LogWriter<>("GTonKmDetail.txt", false);
		for (VehicleType vehicleType : vehicles.getVehicleTypes().values()) {
			this.gTonKmWriterDetail.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return vehicleType.getId().toString();
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					return LogEntry.toString(fleetCalibrator.getLastFleetStatistics().getVehicleType2domesticTonKm().get(vehicleType));
				}
			});
		}
		
		
		for (FleetCostCalibrator.VehicleGroup group : FleetCostCalibrator.VehicleGroup.values()) {
//			this.groupErrorWriter.addEntry(new LogEntry<>() {
//				@Override
//				public String label() {
//					return group.toString();
//				}
//
//				@Override
//				public String value(FleetCostCalibrator fleetCalibrator) {
//					if (fleetCalibrator.group2lastNormalizedRealized == null) {
//						return "";
//					} else {
//						return LogEntry
//								.toString(100.0 * (fleetCalibrator.group2lastNormalizedRealized.getOrDefault(group, 0.0)
//										- fleetCalibrator.group2normalizedTarget.get(group)));
//					}
//				}
//			});
			this.groupASCWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return group.toString();
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					return LogEntry.toString(fleetCalibrator.createConcurrentGroup2asc().getOrDefault(group, 0.0));
				}
			});
			this.gTonKmWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return group.toString() + "_real";
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					if (fleetCalibrator.group2lastRealizedDomesticGTonKm == null) {
						return "";
					} else {
						return LogEntry
								.toString(fleetCalibrator.group2lastRealizedDomesticGTonKm.getOrDefault(group, 0.0));
					}
				}
			});
			this.gTonKmWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return group.toString() + "_target";
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					return LogEntry.toString(fleetCalibrator.group2targetDomesticGTonKm.get(group));
				}
			});
		}

		for (TransportMode mode : TransportMode.values()) {
			this.groupASCWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return mode.toString();
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					return LogEntry.toString(fleetCalibrator.createConcurrentMode2asc().getOrDefault(mode, 0.0));
				}
			});
//			this.groupErrorWriter.addEntry(new LogEntry<>() {
//				@Override
//				public String label() {
//					return mode.toString();
//				}
//
//				@Override
//				public String value(FleetCostCalibrator fleetCalibrator) {
//					if (fleetCalibrator.mode2lastNormalizedRealized == null
//							|| !fleetCalibrator.mode2normalizedTarget.containsKey(mode)) {
//						return "";
//					} else {
//						return LogEntry
//								.toString(100.0 * (fleetCalibrator.mode2lastNormalizedRealized.getOrDefault(mode, 0.0)
//										- fleetCalibrator.mode2normalizedTarget.get(mode)));
//					}
//				}
//			});

			this.gTonKmWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return mode.toString() + "_real";
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					if (fleetCalibrator.mode2lastRealizedDomesticGTonKm == null
							|| !fleetCalibrator.mode2targetDomesticGTonKm.containsKey(mode)) {
						return "";
					} else {
						return LogEntry
								.toString(fleetCalibrator.mode2lastRealizedDomesticGTonKm.getOrDefault(mode, 0.0));
					}
				}
			});
			this.gTonKmWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return mode.toString() + "_target";
				}

				@Override
				public String value(FleetCostCalibrator fleetCalibrator) {
					if (!fleetCalibrator.mode2targetDomesticGTonKm.containsKey(mode)) {
						return "";
					} else {
						return LogEntry.toString(fleetCalibrator.mode2targetDomesticGTonKm.get(mode));
					}
				}
			});

		}

//		this.groupErrorWriter.addEntry(new LogEntry<>() {
//			@Override
//			public String label() {
//				return "AbsErrorSum";
//			}
//
//			@Override
//			public String value(FleetCostCalibrator fleetCalibrator) {
//				if (fleetCalibrator.group2lastNormalizedRealized == null) {
//					return "";
//				} else {
//					double sum = 0.0;
//					for (FleetCostCalibrator.Group group : FleetCostCalibrator.Group.values()) {
//						sum += Math.abs(fleetCalibrator.group2lastNormalizedRealized.getOrDefault(group, 0.0)
//								- fleetCalibrator.group2normalizedTarget.get(group));
//					}
//					return LogEntry.toString(100.0 * sum);
//				}
//			}
//		});
	}

	public void log(FleetCostCalibrator fleetCalibr) {
//		this.groupErrorWriter.writeToFile(fleetCalibr);
		this.groupASCWriter.writeToFile(fleetCalibr);
		this.gTonKmWriter.writeToFile(fleetCalibr);
		this.gTonKmWriterDetail.writeToFile(fleetCalibr);
	}
}
