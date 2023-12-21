/**
 * se.vti.utils
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
package se.vti.utils.misc.metropolishastings;

/**
  * Based on floetteroed.utilities.math.metropolishastings package.
 * 
 * @author GunnarF
 * 
 * @param <S>
 */
public class MHTransition<S extends Object> {

	// -------------------- CONSTANTS --------------------

	private final S oldState;

	private final S newState;

	private final double fwdLogProb;

	private final double bwdLogProb;

	// -------------------- CONSTRUCTION --------------------

	public MHTransition(final S oldState, final S newState,
			final double fwdLogProb, final double bwdLogProb) {
		this.oldState = oldState;
		this.newState = newState;
		this.fwdLogProb = fwdLogProb;
		this.bwdLogProb = bwdLogProb;
	}

	// -------------------- CONTENT ACCESS --------------------

	public S getOldState() {
		return this.oldState;
	}

	public S getNewState() {
		return this.newState;
	}

	public double getFwdLogProb() {
		return this.fwdLogProb;
	}

	public double getBwdLogProb() {
		return this.bwdLogProb;
	}
}
