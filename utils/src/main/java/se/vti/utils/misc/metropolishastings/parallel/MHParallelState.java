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

import se.vti.utils.misc.metropolishastings.MHProposal;
import se.vti.utils.misc.metropolishastings.MHState;
import se.vti.utils.misc.metropolishastings.MHWeight;

/**
 * 
 * @author GunnarF
 *
 * @param <X>
 */
class MHParallelState<X> implements MHState<X> {

	private final List<X> elements;

	private int selectedIndex;

	MHParallelState(X initial, int size) {
		this.elements = new ArrayList<>(size);
		this.selectedIndex = 0;
		this.elements.add(initial);
		this.elements.addAll(Collections.nCopies(size - 1, null));
	}

	MHParallelState(MHParallelState<X> parent, MHProposal<X> proposal,
			MHTransitionKernelFactory<X> transitionKernelFactory, MHWeight<X> weight, Random rnd) {

		this.elements = new ArrayList<>(parent.size());
		this.elements.add(parent.getState());
		for (int i = 0; i < parent.size() - 1; i++) {
			this.elements.add(proposal.newTransition(parent.getState()).getNewState());
		}

		final List<MHTransitionKernel<X>> transitionKernels = new ArrayList<>(parent.size());
		for (X from : this.elements) {
			transitionKernels.add(transitionKernelFactory.newInstance(from));
		}

		final RealVector lnKsum = new ArrayRealVector(this.elements.size());
		for (int i = 0; i < this.elements.size(); i++) {
			final MHTransitionKernel<X> fwdKernel = transitionKernels.get(i);
			for (int l = 0; l < this.elements.size(); l++) {
				if (l != i) {
					lnKsum.addToEntry(i, Math.log(fwdKernel.transitionProba(this.elements.get(l))));
				}
			}
		}

		final double _N = this.elements.size() - 1;
		final RealMatrix _A = new Array2DRowRealMatrix(this.elements.size(), this.elements.size());
		for (int i = 0; i < this.elements.size(); i++) {
			_A.setEntry(i, i, 1.0);
			for (int j = 0; j < this.elements.size(); j++) {
				final double _lnRij = weight.logWeight(this.elements.get(j)) + lnKsum.getEntry(j)
						- weight.logWeight(this.elements.get(i)) - lnKsum.getEntry(i);
				final double _Aij = Math.min(1.0, Math.exp(_lnRij)) / _N;
				_A.setEntry(i, j, _Aij);
				_A.addToEntry(i, i, -_Aij);
			}
		}

		final RealMatrix lpA = new Array2DRowRealMatrix(this.elements.size() + 1, this.elements.size());
		lpA.setSubMatrix(_A.transpose().getData(), 0, 0);
		for (int j = 0; j < _A.getColumnDimension(); j++) {
			lpA.setEntry(_A.getRowDimension() - 1, j, 1.0);
		}

		final RealVector lpb = new ArrayRealVector(this.elements.size() + 1);
		lpb.setEntry(lpb.getDimension() - 1, 1.0);
		DecompositionSolver solver = new LUDecomposition(lpA).getSolver();
		RealVector stationarySelectionProbas = solver.solve(lpb);

		this.selectedIndex = -1;
		final double _U = rnd.nextDouble() * Arrays.stream(stationarySelectionProbas.toArray()).sum();
		double sum = 0.0;
		for (int i = 0; i < stationarySelectionProbas.getDimension(); i++) {
			sum += stationarySelectionProbas.getEntry(i);
			if (_U < sum) {
				this.selectedIndex = i;
				break;
			}
		}
		if (this.selectedIndex == -1) {
			this.selectedIndex = this.elements.size() - 1;
		}
	}

	List<X> getElements() {
		return this.elements;
	}

	int getSelectedIndex() {
		return this.selectedIndex;
	}

	int size() {
		return this.getElements().size();
	}

	@Override
	public X getState() {
		return this.getElements().get(this.getSelectedIndex());
	}
}
