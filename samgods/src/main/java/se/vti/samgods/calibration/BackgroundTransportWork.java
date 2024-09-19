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

import java.util.LinkedHashMap;
import java.util.Map;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;

/**
 * 
 * @author GunnarF
 *
 */
public class BackgroundTransportWork {

	private final Map<Commodity, Map<TransportMode, Double>> targetUnitCost_1_tonKm = new LinkedHashMap<>();

	public BackgroundTransportWork() {
	}

	public BackgroundTransportWork setTargetUnitCost_1_tonKm(Commodity commodity, TransportMode mode,
			double targetUnitCost_1_tonKm) {
		this.targetUnitCost_1_tonKm.computeIfAbsent(commodity, c -> new LinkedHashMap<>()).put(mode,
				targetUnitCost_1_tonKm);
		return this;
	}

}
