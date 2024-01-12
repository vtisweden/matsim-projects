/**
 * se.vti.skellefeaV2X
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
package se.vti.skellefteaV2X.model;

import java.util.ArrayList;
import java.util.List;

import se.vti.skellefteaV2X.roundtrips.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class Preferences implements MHWeight<RoundTrip<Location>> {

	public interface Component {
		public double logWeight(List<Episode> episodes);
	}

	private final Simulator simulator;

	private List<Component> components = new ArrayList<>();

	public Preferences(Simulator simulator) {
		this.simulator = simulator;
	}

	public void addComponent(Component component) {
		this.components.add(component);
	}

	@Override
	public double logWeight(RoundTrip<Location> roundTrip) {
		List<Episode> episodes = this.simulator.simulate(roundTrip);
		double result = 0.0;
		for (Component component : this.components) {
			result += component.logWeight(episodes);
		}
		return result;
	}

}
