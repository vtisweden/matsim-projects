/**
 * se.vti.matsim.dynameq
 * 
 * Copyright (C) 2023 by Filip Kristofferson (VTI) and Gunnar Flötteröd (VTI, LiU).
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
package se.vti.matsim.dynameq.population;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;

import se.vti.matsim.dynameq.population.CentroidSystem.Centroid;
import se.vti.matsim.dynameq.utils.TimeDiscretization;

/**
 * This class acts as a container for Origin-Destination (OD) matrices and is
 * sliced into time bins
 *
 * @author FilipK based on
 * @author Gunnar Flötteröd
 */
public class ODMatrices {

	public class Matrix {

		private Map<Id<Centroid>, Map<Id<Centroid>, Double>> orig2dest2val;

		/**
		 * Main constructor
		 */
		Matrix() {
			this.orig2dest2val = new LinkedHashMap<>();
		}

		/**
		 * This method gets the map of destinations for a specific origin centroid
		 * 
		 * @param orig - the origin centroid id
		 * @return map with the demand for each associated destination
		 */
		private Map<Id<Centroid>, Double> getDest2Val(Id<Centroid> orig) {
			return this.orig2dest2val.computeIfAbsent(orig, orig2 -> new LinkedHashMap<Id<Centroid>, Double>());
		}

		/**
		 * This method sets the demand between a origin and a destination centroid
		 * 
		 * @param orig - the origin centroid id
		 * @param dest - the destination centroid id
		 * @param val  - the demand
		 */
		synchronized void put(Id<Centroid> orig, Id<Centroid> dest, Double val) {
			// Removes the destination if the demand to it is zero.
			if (val == 0.0) {
				this.getDest2Val(orig).remove(dest);
			}
			// Sets the demand to the destination.
			else {
				this.getDest2Val(orig).put(dest, val);
			}
		}

		/**
		 * This method gets the demand between a origin and a destination centroid
		 * 
		 * @param orig - the origin centroid id
		 * @param dest - the destination centroid id
		 * @return the demand
		 */
		public synchronized double get(Id<Centroid> orig, Id<Centroid> dest) {
			return this.getDest2Val(orig).getOrDefault(dest, 0.0);
		}

		/**
		 * This method adds demand between between a origin and a destination centroid
		 * 
		 * @param orig - the origin centroid id
		 * @param dest - the destination centroid id
		 * @param val  - the demand to add
		 */
		synchronized void add(Id<Centroid> orig, Id<Centroid> dest, double val) {
			this.put(orig, dest, val + this.get(orig, dest));
		}

		synchronized void clear() {
			this.orig2dest2val.clear();
		}

	}

	private final String matrixFile;

	private final TimeDiscretization timeDiscr;

	private final List<Matrix> matrixList;

	/**
	 * Main constructor
	 * 
	 * @param timeDiscr  - the time discretization data
	 * @param matrixFile - the name of the matrix file to later load from
	 */
	public ODMatrices(TimeDiscretization timeDiscr, String matrixFile) {
		this.timeDiscr = timeDiscr;
		this.matrixFile = matrixFile;
		// Creates a OD matrix for each time bin.
		this.matrixList = new ArrayList<>(timeDiscr.getBinCnt());
		for (int bin = 0; bin < timeDiscr.getBinCnt(); bin++) {
			this.matrixList.add(new Matrix());
		}
	}

	/**
	 * This method gets the source matrix file name
	 * 
	 * @return the name of the matrix source file
	 */
	String getMatrixFile() {
		return this.matrixFile;
	}

	/**
	 * This method gets the time discretization data
	 * 
	 * @return the time discretization data
	 */
	TimeDiscretization getTimeDiscretization() {
		return this.timeDiscr;
	}

	/**
	 * This method gets OD-matrix for each time bin as a list
	 * 
	 * @return list of OD-matrices
	 */
	public List<Matrix> getMatrixListView() {
		return Collections.unmodifiableList(this.matrixList);
	}

	/**
	 * This method adds demand between a origin and destination centroid for a
	 * specific time bin
	 * 
	 * @param from - origin centroid id
	 * @param to   - destination centroid id
	 * @param bin  - time bin
	 * @param val  - demand to add
	 */
	synchronized void addSynchronized(final Id<Centroid> from, final Id<Centroid> to, final int bin, final double val) {
		this.matrixList.get(bin).add(from, to, val);
	}

}
