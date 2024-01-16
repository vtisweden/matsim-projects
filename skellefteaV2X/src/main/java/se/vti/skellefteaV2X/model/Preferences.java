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

	public static abstract class Component {
		
		private double logWeightThreshold = Double.NEGATIVE_INFINITY;

		public void setLogWeightThreshold(double threshold) {
			this.logWeightThreshold = threshold;
		}
		
		public boolean thresholdPassed(SimulatedRoundTrip simulatedRoundTrip) {
			return (this.logWeight(simulatedRoundTrip) >= this.logWeightThreshold);
		}

		public abstract double logWeight(SimulatedRoundTrip simulatedRoundTrip);

	}

	private List<Component> components = new ArrayList<>();

	private List<Double> weights = new ArrayList<>();

	public Preferences() {
	}

	public void addPreferences(Preferences other) {
		for (int i = 0; i < other.components.size(); i++) {
			this.addComponent(other.components.get(i), other.weights.get(i));
		}
	}

	public void addComponent(Component component, double weight) {
		this.components.add(component);
		this.weights.add(weight);
	}

	public void addComponent(Component component) {
		this.addComponent(component, 1.0);
	}

	@Override
	public double logWeight(RoundTrip<Location> roundTrip) {
		double result = 0.0;
		for (int i = 0; i < this.components.size(); i++) {
			result += this.weights.get(i) * this.components.get(i).logWeight((SimulatedRoundTrip) roundTrip);
		}
		return result;
	}

	public boolean thresholdPassed(SimulatedRoundTrip roundTrip) {
		for (Component component : this.components) {
			if (!component.thresholdPassed(roundTrip)) {
				return false;
			}
		}
		return true;
	}

}
