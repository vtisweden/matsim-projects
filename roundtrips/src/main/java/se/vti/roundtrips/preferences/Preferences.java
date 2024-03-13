/**
 * se.vti.roundtrips
 * 
 * Copyright (C) 2023,2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.preferences;

import java.util.ArrayList;
import java.util.List;

import se.vti.roundtrips.model.Location;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 */
public class Preferences<R extends RoundTrip<L>, L extends Location> implements MHWeight<R> {

	public static abstract class Component<R extends RoundTrip<L>, L extends Location> {

		private double logWeightThreshold = Double.NEGATIVE_INFINITY;

		public void setLogWeightThreshold(double threshold) {
			this.logWeightThreshold = threshold;
		}

		public boolean thresholdPassed(R roundTrip) {
			return (this.logWeight(roundTrip) >= this.logWeightThreshold);
		}

		public abstract double logWeight(R roundTrip);

	}

	private List<Component<R, L>> components = new ArrayList<>();

	private List<Double> weights = new ArrayList<>();

	public Preferences() {
	}

	public void addPreferences(Preferences<R, L> other) {
		for (int i = 0; i < other.components.size(); i++) {
			this.addComponent(other.components.get(i), other.weights.get(i));
		}
	}

	public void addComponent(Component<R, L> component, double weight) {
		this.components.add(component);
		this.weights.add(weight);
	}

	public void addComponent(Component<R, L> component) {
		this.addComponent(component, 1.0);
	}

	@Override
	public double logWeight(R roundTrip) {
		double result = 0.0;
		for (int i = 0; i < this.components.size(); i++) {
			result += this.weights.get(i) * this.components.get(i).logWeight(roundTrip);
		}
		return result;
	}

	public boolean thresholdPassed(R roundTrip) {
		for (Component<R, L> component : this.components) {
			if (!component.thresholdPassed(roundTrip)) {
				return false;
			}
		}
		return true;
	}
}
