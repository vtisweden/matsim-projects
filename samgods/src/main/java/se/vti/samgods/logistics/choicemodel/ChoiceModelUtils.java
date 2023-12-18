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
package se.vti.samgods.logistics.choicemodel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Can be instantiated for parallel use.
 * 
 * TODO Not ideal to have the ShipmentCost type everywhere
 * 
 * @author GunnarF
 *
 */
public class ChoiceModelUtils {

	private final Random rnd;

	public ChoiceModelUtils() {
		this.rnd = new Random();
	}

	public ChoiceModelUtils(final Random rnd) {
		this.rnd = rnd;
	}

	public <C extends ShipmentCost> Map<Alternative<C>, Double> computeUtilities(List<Alternative<C>> alternatives,
			ShipmentUtilityFunction<C> utilityFunction) {
		final Map<Alternative<C>, Double> result = new LinkedHashMap<>(alternatives.size());
		for (Alternative<C> alternative : alternatives) {
			result.put(alternative, utilityFunction.computeUtility(alternative.shipment, alternative.cost));
		}
		return result;
	}

	public <C extends ShipmentCost> Map<Alternative<C>, Double> computeLogitProbabilities(
			final Map<Alternative<C>, Double> alternative2utility) {
		Map<Alternative<C>, Double> result = new LinkedHashMap<>(alternative2utility.size());
		final double maxUtility = alternative2utility.values().stream().mapToDouble(v -> v).max().getAsDouble();
		double denom = 0.0;
		for (Map.Entry<Alternative<C>, Double> e : alternative2utility.entrySet()) {
			final double num = Math.exp(e.getValue() - maxUtility);
			result.put(e.getKey(), num);
			denom += num;
		}
		final double finalDenom = denom;
		result.entrySet().stream().forEach(e -> e.setValue(e.getValue() / finalDenom));
		return result;
	}

	public <C extends ShipmentCost> Alternative<C> chooseFromProbabilities(
			final Map<Alternative<C>, Double> alternative2probability) {
		double probaSum = 0.0;
		final double threshold = this.rnd.nextDouble();
		for (Map.Entry<Alternative<C>, Double> e : alternative2probability.entrySet()) {
			probaSum += e.getValue();
			if (threshold < probaSum) {
				return e.getKey();
			}
		}
		// May end up here for numerical reasons.
		return new ArrayList<>(alternative2probability.keySet()).get(this.rnd.nextInt(alternative2probability.size()));
	}

	public <C extends ShipmentCost> Alternative<C> choose(final List<Alternative<C>> alternatives,
			ShipmentUtilityFunction<C> utilityFunction) {
		return this.chooseFromProbabilities(
				this.computeLogitProbabilities(this.computeUtilities(alternatives, utilityFunction)));
	}

}
