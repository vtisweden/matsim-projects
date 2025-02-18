/**
 * se.vti.roundtrips.parallel
 * 
 * Copyright (C) 2025 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.roundtrips.parallel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * (Starting here with a sequential implementation of the parallel logic.)
 * 
 * @author GunnarF
 *
 * @param <X>
 */
public class MHParallelProposal<X> {

	// -------------------- MEMBERS --------------------

	private final BiFunction<X, X, Double> proposalLogDistribution;

	private final Function<X, X> proposalGenerator;

	private final int threads;

	// -------------------- CONSTRUCTION --------------------

	public MHParallelProposal(BiFunction<X, X, Double> proposalDistribution,
			Supplier<Function<X, X>> proposalGeneratorFactory, int threads) {
		this.proposalLogDistribution = proposalDistribution;
		this.proposalGenerator = proposalGeneratorFactory.get();
		this.threads = threads;
	}

	// -------------------- IMPLEMENTATION --------------------

	public MHParallelTransition<X> newTransition(X state) {

		final List<X> allStates = new ArrayList<>(this.threads + 1);
		allStates.add(state);
		for (int i = 0; i < this.threads; i++) {
			allStates.add(this.proposalGenerator.apply(state));
		}
		final MHParallelTransition<X> parallelTransition = new MHParallelTransition<>(allStates);

		for (int i = 0; i < allStates.size(); i++) {
			for (int j = 0; j < allStates.size(); j++) {
				parallelTransition.setLogTransProba(i, j,
						this.proposalLogDistribution.apply(allStates.get(i), allStates.get(j)));
			}
		}
		return parallelTransition;
	}

}
