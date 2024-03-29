/**
 * se.vti.samgods
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
package se.vti.samgods.logistics;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportDemand {

	// -------------------- MEMBERS --------------------

	private Map<Commodity, PWCMatrix> commodity2pwcMatrix = new LinkedHashMap<>(Commodity.values().length);

	private Map<Commodity, Map<OD, List<TransportChain>>> commodity2od2chains = new LinkedHashMap<>(
			Commodity.values().length);

	// -------------------- CONSTRUCTION --------------------

	public TransportDemand() {
	}

	// -------------------- SETTERS AND GETTERS --------------------

	public void setPWCMatrix(Commodity commodity, PWCMatrix matrix) {
		this.commodity2pwcMatrix.put(commodity, matrix);
	}

	public void setTransportChains(Commodity commodity, Map<OD, List<TransportChain>> chains) {
		this.commodity2od2chains.put(commodity, chains);
	}

	public PWCMatrix getPWCMatrix(final Commodity commodity) {
		return this.commodity2pwcMatrix.get(commodity);
	}

	public Map<OD, List<TransportChain>> getTransportChains(Commodity commodity) {
		return this.commodity2od2chains.get(commodity);
	}

	public List<TransportChain> getTransportChains(Commodity commodity, OD od) {
		return this.commodity2od2chains.get(commodity).get(od);
	}

	public List<TransportChain> getTransportChains(Commodity commodity, Id<Node> origin, Id<Node> destination) {
		return this.getTransportChains(commodity, new OD(origin, destination));
	}

	public void downsample(Commodity commodity, final double samplingRate) {
		final Random rnd = new Random();
		final Set<OD> downsampledODs = this.getTransportChains(commodity).keySet().stream()
				.filter(k -> rnd.nextDouble() < samplingRate).collect(Collectors.toSet());
		this.getTransportChains(commodity).keySet().retainAll(downsampledODs);
		this.getPWCMatrix(commodity).getOd2Amount_ton_yr().keySet().retainAll(downsampledODs);
	}

}
