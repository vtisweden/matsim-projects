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
package se.vti.utils.misc.math;

/**
 * Based on floetteroed.utilities.math.metropolishastings package.
 * 
 * @author GunnarF
 *
 */
public class MathHelpers {

	private static final MathHelpers globalInstance = new MathHelpers();
	
	public MathHelpers() {		
	}
	
	public static MathHelpers globalInstance() {
		return globalInstance;
	}
	
	public double round(final double x, final int digits) {
		final double fact = Math.pow(10.0, digits);
		return Math.round(x * fact) / fact;
	}

	public double overlap(final double start1, final double end1,
			final double start2, final double end2) {
		return Math.max(0, (Math.min(end1, end2) - Math.max(start1, start2)));
	}
	
}
