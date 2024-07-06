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
package se.vti.samgods.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * Can be instantiated for parallel use.
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

	public <A> List<Double> computeLogitProbabilities(final List<A> alternatives,
			final Function<A, Double> alternative2utility) {
		final List<Double> result = new ArrayList<>(alternatives.size());
		final double maxUtility = alternatives.stream().mapToDouble(a -> alternative2utility.apply(a)).max()
				.getAsDouble();
		double denom = 0.0;
		for (A alternative : alternatives) {
			final double num = Math.exp(alternative2utility.apply(alternative) - maxUtility);
			result.add(num);
			denom += num;
		}
		for (int i = 0; i < result.size(); i++) {
			result.set(i, result.get(i) / denom);
		}
		return result;
	}

	public <A> A chooseFromProbabilities(final List<A> alternatives, final List<Double> probabilities) {
		double probaSum = 0.0;
		final double threshold = this.rnd.nextDouble();
		for (int i = 0; i < alternatives.size(); i++) {
			probaSum += probabilities.get(i);
			if (threshold < probaSum) {
				return alternatives.get(i);
			}
		}
		// May end up here very rarely for numerical reasons?
		return alternatives.get(this.rnd.nextInt(alternatives.size()));
	}

	public <A> A choose(final List<A> alternatives, final Function<A, Double> alternative2utility) {
		return this.chooseFromProbabilities(alternatives,
				this.computeLogitProbabilities(alternatives, alternative2utility));
	}
}
