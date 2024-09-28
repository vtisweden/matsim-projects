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

import java.util.Collections;
import java.util.Map;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.utils.misc.iterationlogging.LogEntry;
import se.vti.utils.misc.iterationlogging.LogWriter;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportationStatisticsLogger {

	private final LogWriter<Map<Commodity, Double>> commodityEfficiencyStatsWriter;
	private final LogWriter<Map<Commodity, Double>> commodityUnitcostStatsWriter;
	private final LogWriter<Map<Commodity, Map<TransportMode, Double>>> commodityRailDomesticTransportWork;

	private final LogWriter<Map<TransportMode, Double>> modeEfficiencyStatsWriter;
	private final LogWriter<Map<TransportMode, Double>> modeUnitcostStatsWriter;
	private final LogWriter<Map<TransportMode, Double>> modeDomesticTransportWorkWriter;

	private final LogWriter<Map<Commodity, Map<TransportMode, Double>>> commodityModeEfficiencyStatsWriter;
	private final LogWriter<Map<Commodity, Map<TransportMode, Double>>> commodityModeUnitcostStatsWriter;

	public TransportationStatisticsLogger() {

		this.commodityEfficiencyStatsWriter = new LogWriter<>("commodityEfficiency.txt", false);
		this.commodityUnitcostStatsWriter = new LogWriter<>("commodityUnitcost.txt", false);
		this.commodityRailDomesticTransportWork = new LogWriter<>("commodityRailDomesticTransportWork.txt", false);
		for (Commodity commodity : Commodity.values()) {
			this.commodityEfficiencyStatsWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return commodity.toString();
				}

				@Override
				public String value(Map<Commodity, Double> commodity2efficiency) {
					return LogEntry.toString(commodity2efficiency.get(commodity));
				}
			});
			this.commodityUnitcostStatsWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return commodity.toString();
				}

				@Override
				public String value(Map<Commodity, Double> commodity2unitcost) {
					return LogEntry.toString(commodity2unitcost.get(commodity));
				}
			});
			this.commodityRailDomesticTransportWork.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return commodity.toString();
				}

				@Override
				public String value(Map<Commodity, Map<TransportMode, Double>> commodity2mode2transportWork) {
					return LogEntry.toString(commodity2mode2transportWork
							.getOrDefault(commodity, Collections.emptyMap()).get(TransportMode.Rail));
				}
			});
		}

		this.modeEfficiencyStatsWriter = new LogWriter<>("modeEfficiency.txt", false);
		this.modeUnitcostStatsWriter = new LogWriter<>("modeUnitcost.txt", false);
		this.modeDomesticTransportWorkWriter = new LogWriter<>("modeDomesticTransportwork.txt", false);
		for (TransportMode mode : TransportMode.values()) {
			this.modeEfficiencyStatsWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return mode.toString();
				}

				@Override
				public String value(Map<TransportMode, Double> mode2efficiency) {
					return LogEntry.toString(mode2efficiency.get(mode));
				}
			});
			this.modeUnitcostStatsWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return mode.toString();
				}

				@Override
				public String value(Map<TransportMode, Double> mode2unitcost) {
					return LogEntry.toString(mode2unitcost.get(mode));
				}
			});
			this.modeDomesticTransportWorkWriter.addEntry(new LogEntry<>() {
				@Override
				public String label() {
					return mode.toString();
				}

				@Override
				public String value(Map<TransportMode, Double> mode2transportWork) {
					return LogEntry.toString(mode2transportWork.get(mode));
				}
			});
		}

		this.commodityModeEfficiencyStatsWriter = new LogWriter<>("commodityModeEfficency.txt", false);
		this.commodityModeUnitcostStatsWriter = new LogWriter<>("commodityModeUnitcost.txt", false);
		for (Commodity commodity : Commodity.values()) {
			for (TransportMode mode : TransportMode.values()) {
				this.commodityModeEfficiencyStatsWriter.addEntry(new LogEntry<>() {
					@Override
					public String label() {
						return commodity.toString() + "/" + mode.toString();
					}

					@Override
					public String value(Map<Commodity, Map<TransportMode, Double>> commodity2mode2efficiency) {
						return LogEntry.toString(
								commodity2mode2efficiency.getOrDefault(commodity, Collections.emptyMap()).get(mode));
					}
				});
				this.commodityModeUnitcostStatsWriter.addEntry(new LogEntry<>() {
					@Override
					public String label() {
						return commodity.toString() + "/" + mode.toString();
					}

					@Override
					public String value(Map<Commodity, Map<TransportMode, Double>> commodity2mode2unitcost) {
						return LogEntry.toString(
								commodity2mode2unitcost.getOrDefault(commodity, Collections.emptyMap()).get(mode));
					}
				});
			}
		}
	}

	public void log(TransportationStatistics transpStats) {
		this.commodityEfficiencyStatsWriter.writeToFile(transpStats.computeCommodity2efficiency());
		this.commodityUnitcostStatsWriter.writeToFile(transpStats.computeCommodity2unitCost_1_tonKm());
		this.commodityRailDomesticTransportWork
				.writeToFile(transpStats.getCommodity2mode2domesticTransportWork_tonKm());

		this.modeEfficiencyStatsWriter.writeToFile(transpStats.computeMode2efficiency());
		this.modeUnitcostStatsWriter.writeToFile(transpStats.computeMode2unitCost_1_tonKm());
		this.modeDomesticTransportWorkWriter.writeToFile(transpStats.computeMode2domesticTransportWork_tonKm());

		this.commodityModeEfficiencyStatsWriter.writeToFile(transpStats.computeCommodity2mode2efficiency());
		this.commodityModeUnitcostStatsWriter.writeToFile(transpStats.computeCommodity2mode2unitCost_1_tonKm());
	}

}
