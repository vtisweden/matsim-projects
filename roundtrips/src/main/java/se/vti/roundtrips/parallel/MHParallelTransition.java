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

import java.util.Collections;
import java.util.List;

/**
 * 
 * @author GunnarF
 *
 * @param <X>
 */
public class MHParallelTransition<X> {

	// -------------------- CONSTANTS --------------------

	private final List<X> states;

	private final double[][] logTransProbas;

	// -------------------- CONSTRUCTION --------------------

	public MHParallelTransition(List<X> states) {
		this.states = Collections.unmodifiableList(states);
		this.logTransProbas = new double[states.size()][states.size()];
	}

	public void setLogTransProba(int i, int j, double logTransProba) {
		this.logTransProbas[i][j] = logTransProba;
	}

	// -------------------- CONTENT ACCESS --------------------

	public List<X> getStates() {
		return this.states;
	}

	public double getLogTransProba(int i, int j) {
		return this.logTransProbas[i][j];
	}

}
