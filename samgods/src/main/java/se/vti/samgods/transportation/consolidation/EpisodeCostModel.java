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

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import se.vti.samgods.BasicTransportCost;
import se.vti.samgods.DetailedTransportCost;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.utils.TupleGrouping;

/**
 * 
 * @author GunnarF
 *
 */
public interface EpisodeCostModel {

	DetailedTransportCost computeCost_1_ton(TransportEpisode episode);

	Map<Id<Link>, BasicTransportCost> createLinkTransportCosts(
			TupleGrouping<SamgodsConstants.Commodity, SamgodsConstants.TransportMode>.Group commodityAndModeGrouping,
			Network network, boolean container);
}