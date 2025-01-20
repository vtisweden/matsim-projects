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
package se.vti.utils.misc.math;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Function;

/**
 * 
 * @author GunnarF
 *
 */
public class LogitChoiceModel {

	// -------------------- CONSTANTS --------------------

	private final Random rnd;

	// -------------------- CONSTRUCTION --------------------

	public LogitChoiceModel() {
		this.rnd = new Random();
	}

	public LogitChoiceModel(final Random rnd) {
		this.rnd = rnd;
	}

	// -------------------- IMPLEMENTATION --------------------

	public <A> double[] computeUtilities(final List<A> alternatives, final Function<A, Double> alternative2utility) {
		return alternatives.stream().mapToDouble(a -> alternative2utility.apply(a)).toArray();
	}

	public double[] computeLogitProbabilities(final double[] utilities) {
		final double[] probabilities = new double[utilities.length];
		final double maxUtility = Arrays.stream(utilities).max().getAsDouble();
		double denom = 0.0;
		for (int i = 0; i < utilities.length; i++) {
			final double utility = utilities[i];
			final double num = Math.exp(utility - maxUtility);
			probabilities[i] = num;
			denom += num;
		}
		for (int i = 0; i < probabilities.length; i++) {
			probabilities[i] = probabilities[i] / denom;
		}
		return probabilities;
	}

	public <A> A chooseFromProbabilities(final List<A> alternatives, final double[] probabilities) {
		double probaSum = 0.0;
		final double threshold = this.rnd.nextDouble();
		for (int i = 0; i < alternatives.size(); i++) {
			probaSum += probabilities[i];
			if (threshold < probaSum) {
				return alternatives.get(i);
			}
		}
		// May end up here very rarely for numerical reasons.
		return alternatives.get(this.rnd.nextInt(alternatives.size()));
	}

	public <A> A choose(final List<A> alternatives, final Function<A, Double> alternative2utility) {
		return this.chooseFromProbabilities(alternatives,
				this.computeLogitProbabilities(this.computeUtilities(alternatives, alternative2utility)));
	}

	// -------------------- MAIN-FUNCTION, ONLY FOR TESTING --------------------

	public static void main(String[] args) {

		int one = 1;
		int two = 2;
		List<Integer> alternatives = Arrays.asList(one, two);

		Function<Integer, Double> alt2utl = new Function<>() {
			@Override
			public Double apply(Integer alt) {
				return alt.doubleValue();
			}
		};

		double p1 = Math.exp(one - two) / (Math.exp(one - two) + Math.exp(two - two));

		LogitChoiceModel model = new LogitChoiceModel();

		double oneCnt = 0;
		for (int r = 0; r < 1000 * 1000; r++) {
			int choice = model.choose(alternatives, alt2utl);
			if (choice == one) {
				oneCnt++;
			}
			if (r % 1000 == 0) {
				System.out.println(oneCnt / r - p1);
			}
		}
	}
}
