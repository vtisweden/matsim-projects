/**
 * se.vti.utils.misc.metropolishastings.parallel
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
package se.vti.utils.misc.metropolishastings.parallel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import se.vti.utils.misc.metropolishastings.MHOneStepLogic;
import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHState;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 * @param <X>
 */
public class MHParallelOneStepLogic<X> implements MHOneStepLogic<X> {

	private class ParallelState implements MHState<X> {

		private final List<X> elements;

		private int selectedIndex;

		private ParallelState(List<X> elements, int selectedIndex) {
			this.elements = elements;
			this.selectedIndex = selectedIndex;
		}

		private ParallelState(X initial, int size) {
			this.elements = new ArrayList<>(size);
			this.selectedIndex = 0;
			this.elements.add(initial);
			this.elements.addAll(Collections.nCopies(size - 1, null));
		}

		@Override
		public X getState() {
			return this.elements.get(this.selectedIndex);
		}
	}

	private final MHProposal<X> proposal;

	private final MHTransitionKernelFactory<X> transitionKernelFactory;

	private final MHWeight<X> weight;

	private final Random rnd;

	private final int proposalCnt;

	public MHParallelOneStepLogic(MHProposal<X> proposal, MHTransitionKernelFactory<X> transitionKernelFactory,
			MHWeight<X> weight, Random rnd, int proposalCnt) {
		this.proposal = proposal;
		this.transitionKernelFactory = transitionKernelFactory;
		this.weight = weight;
		this.rnd = rnd;
		this.proposalCnt = proposalCnt;
	}

	@Override
	public MHState<X> createInitial(X initial) {
		return new ParallelState(initial, this.proposalCnt + 1);
	}

	@Override
	public MHState<X> drawNext(MHState<X> current) {

		final ParallelState parent = (ParallelState) current;

		final List<X> elements = new ArrayList<>(parent.elements.size());
		elements.add(parent.getState());
		for (int i = 0; i < parent.elements.size() - 1; i++) {
			elements.add(this.proposal.newTransition(parent.getState()).getNewState());
		}

		final List<MHTransitionKernel<X>> transitionKernels = new ArrayList<>(elements.size());
		for (X from : elements) {
			transitionKernels.add(this.transitionKernelFactory.newInstance(from));
		}

		final RealVector lnKsum = new ArrayRealVector(elements.size());
		for (int i = 0; i < elements.size(); i++) {
			final MHTransitionKernel<X> fwdKernel = transitionKernels.get(i);
			for (int l = 0; l < elements.size(); l++) {
				if (l != i) {
					lnKsum.addToEntry(i, Math.log(fwdKernel.transitionProba(elements.get(l))));
				}
			}
		}

		final double _N = elements.size() - 1;
		final RealMatrix _A = new Array2DRowRealMatrix(elements.size(), elements.size());
		for (int i = 0; i < elements.size(); i++) {
			_A.setEntry(i, i, 1.0);
			for (int j = 0; j < elements.size(); j++) {
				final double _lnRij = this.weight.logWeight(elements.get(j)) + lnKsum.getEntry(j)
						- this.weight.logWeight(elements.get(i)) - lnKsum.getEntry(i);
				final double _Aij = Math.min(1.0, Math.exp(_lnRij)) / _N;
				_A.setEntry(i, j, _Aij);
				_A.addToEntry(i, i, -_Aij);
			}
		}

		final RealMatrix lpA = new Array2DRowRealMatrix(elements.size() + 1, elements.size());
		lpA.setSubMatrix(_A.transpose().getData(), 0, 0);
		for (int j = 0; j < _A.getColumnDimension(); j++) {
			lpA.setEntry(_A.getRowDimension() - 1, j, 1.0);
		}

		final RealVector lpb = new ArrayRealVector(elements.size() + 1);
		lpb.setEntry(lpb.getDimension() - 1, 1.0);
		DecompositionSolver solver = new LUDecomposition(lpA).getSolver();
		RealVector stationarySelectionProbas = solver.solve(lpb);

		final double _U = rnd.nextDouble() * Arrays.stream(stationarySelectionProbas.toArray()).sum();
		double sum = 0.0;
		for (int i = 0; i < stationarySelectionProbas.getDimension(); i++) {
			sum += stationarySelectionProbas.getEntry(i);
			if (_U < sum) {
				return new ParallelState(elements, i);
			}
		}
		return new ParallelState(elements, elements.size() - 1); // should not happen
	}
}
