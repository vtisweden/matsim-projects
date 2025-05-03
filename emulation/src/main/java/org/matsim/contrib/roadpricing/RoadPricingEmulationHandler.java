/**
 * org.matsim.contrib.emulation
 * 
 * Copyright (C) 2023 by Gunnar Flötteröd (VTI, LiU).
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
package org.matsim.contrib.roadpricing;

import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.VehicleEntersTrafficEvent;
import org.matsim.api.core.v01.events.VehicleLeavesTrafficEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleEntersTrafficEventHandler;
import org.matsim.api.core.v01.events.handler.VehicleLeavesTrafficEventHandler;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.emulation.handlers.EmulationHandler;
import org.matsim.core.api.experimental.events.EventsManager;

import com.google.inject.Inject;

/**
 *
 * @author Gunnar Flötteröd
 *
 */
public class RoadPricingEmulationHandler implements EmulationHandler, LinkEnterEventHandler,
		VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private final Network network;
	private final RoadPricingScheme scheme;

	private RoadPricingTollCalculator tollCalculator = null;

	@Inject
	public RoadPricingEmulationHandler(final Network network, final RoadPricingScheme scheme, EventsManager events) {
		this.network = network;
		this.scheme = scheme;
	}

	@Override
	public void configure(EventsManager eventsManager) {
		this.tollCalculator = new RoadPricingTollCalculator(this.network, this.scheme, eventsManager);
	}

	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		this.tollCalculator.handleEvent(event);
	}

	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		this.tollCalculator.handleEvent(event);
	}

	@Override
	public void handleEvent(LinkEnterEvent event) {
		this.tollCalculator.handleEvent(event);
	}
}
