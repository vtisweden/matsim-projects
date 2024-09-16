/**
 * se.vti.samgods.transportation.consolidation
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
package se.vti.samgods.transportation.consolidation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import se.vti.samgods.logistics.choice.ChainAndShipmentSize;

/**
 * 
 * @author GunnarF
 *
 */
public class ConsolidationJob {

	public static final ConsolidationJob TERMINATE = new ConsolidationJob(null, new ArrayList<>(0), 0);

	// concurrency: only references in this job
	public final ConsolidationUnit consolidationUnit;

	// concurrency: list entries possibly referenced in many jobs
	public final CopyOnWriteArrayList<ChainAndShipmentSize> choices;

	public final int serviceInterval_days;

	public ConsolidationJob(ConsolidationUnit consolidationUnit, List<ChainAndShipmentSize> choices,
			int serviceInterval_days) {
		this.consolidationUnit = consolidationUnit;
		this.choices = new CopyOnWriteArrayList<>(choices);
		this.serviceInterval_days = serviceInterval_days;
	}

}
