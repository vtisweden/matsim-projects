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
package se.vti.samgods.deprecated;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;

import se.vti.samgods.InsufficientDataException;
import se.vti.samgods.SamgodsConstants;
import se.vti.samgods.logistics.TransportEpisode;
import se.vti.samgods.transportation.costs.BasicTransportCost;
import se.vti.samgods.transportation.costs.DetailedTransportCost;
import se.vti.samgods.transportation.costs.EpisodeCostModel;

/**
 * 
 * @author GunnarF
 *
 */
public class EpisodeCostModels implements EpisodeCostModel {

	// -------------------- MEMBERS --------------------

	private List<EpisodeCostModel> models = new LinkedList<>();

	// -------------------- CONSTRUCTION --------------------

	public EpisodeCostModels() {
	}

	public EpisodeCostModels(EpisodeCostModel... models) {
		for (EpisodeCostModel model : models) {
			this.add(model);
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public void add(EpisodeCostModel episodeCostModel) {
		this.models.add(episodeCostModel);
	}

	@Override
	public DetailedTransportCost computeUnitCost(TransportEpisode episode) throws InsufficientDataException {
		for (EpisodeCostModel model : this.models) {
			try {
				return model.computeUnitCost(episode);
			} catch (InsufficientDataException e) {
			}
		}
		throw new InsufficientDataException(this.getClass(), "No model available to compute episode unit cost.",
				episode);
	}

	@Override
	public void populateLink2transportCost(Map<Link, BasicTransportCost> link2cost,
			SamgodsConstants.Commodity commodity, SamgodsConstants.TransportMode mode, Boolean isContainer,
			Network network) throws InsufficientDataException {
		final Iterator<EpisodeCostModel> modelIt = this.models.iterator();
		while (modelIt.hasNext() && (link2cost.size() < network.getLinks().size())) {
			try {
				modelIt.next().populateLink2transportCost(link2cost, commodity, mode, isContainer, network);
			} catch (InsufficientDataException e) {
			}
		}
		if (link2cost.size() < network.getLinks().size()) {
			throw new InsufficientDataException(this.getClass(),
					"No model available to compute link transport cost for "
							+ (network.getLinks().size() - link2cost.size()) + " out of " + network.getLinks().size()
							+ " network links.",
					commodity, null, mode, isContainer, null);
		}
	}
}
