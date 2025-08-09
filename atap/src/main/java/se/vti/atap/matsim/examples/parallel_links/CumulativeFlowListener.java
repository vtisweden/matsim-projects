/**
 * se.vti.atap.examples.parallel_links
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
package se.vti.atap.matsim.examples.parallel_links;

import java.nio.file.Paths;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.LinkLeaveEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.LinkLeaveEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.core.controler.events.AfterMobsimEvent;
import org.matsim.core.controler.events.BeforeMobsimEvent;
import org.matsim.core.controler.listener.AfterMobsimListener;
import org.matsim.core.controler.listener.BeforeMobsimListener;

/**
 * 
 * @author GunnarF
 *
 */
public class CumulativeFlowListener
		implements BeforeMobsimListener, AfterMobsimListener, VehicleEntersTrafficEventHandler, LinkEnterEventHandler,
		LinkLeaveEventHandler, VehicleLeavesTrafficEventHandler {

	private final CumulativeFlows inflows = new CumulativeFlows();
	private final CumulativeFlows outflows = new CumulativeFlows();

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.inflows.add(event.getTime(), event.getLinkId());
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.inflows.add(event.getTime(), event.getLinkId());
	}

	@Override
	public void handleEvent(LinkLeaveEvent event) {
		this.outflows.add(event.getTime(), event.getLinkId());
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.outflows.add(event.getTime(), event.getLinkId());
	}

	@Override
	public void notifyBeforeMobsim(BeforeMobsimEvent event) {
		this.inflows.clear();
		this.outflows.clear();
	}

	@Override
	public void notifyAfterMobsim(AfterMobsimEvent event) {
		String outputDirectory = event.getServices().getConfig().controller().getOutputDirectory();
		this.inflows.writeToFile(Paths.get(outputDirectory,
				"inflows." + event.getIteration() + ".tsv").toString());
		this.outflows.writeToFile(Paths.get(outputDirectory,
				"outflows." + event.getIteration() + ".tsv").toString());
	}
}
