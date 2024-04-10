/**
 * od2roundtrips.model
 * 
 * Copyright (C) 2024 by Gunnar Flötteröd (VTI, LiU).
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
package se.vti.od2roundtrips.targets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import se.vti.od2roundtrips.model.MultiRoundTripWithOD;
import se.vti.od2roundtrips.model.TAZ;
import se.vti.roundtrips.single.RoundTrip;
import se.vti.utils.misc.metropolishastings.MHStateProcessor;

/**
 * 
 * @author GunnarF
 *
 */
public class TargetLogger implements MHStateProcessor<MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>>> {

	private final String fileName;

	private final Target target;

	private final int interval;

	private long iteration;

	public TargetLogger(int interval, Target target, String fileName) {
		this.interval = interval;
		this.target = target;
		this.fileName = fileName;
	}

	@Override
	public void start() {
		this.iteration = 0;
		new File(this.fileName).delete();
	}

	@Override
	public void processState(MultiRoundTripWithOD<TAZ, RoundTrip<TAZ>> state) {

		if (this.iteration++ % this.interval == 0) {

			new File(this.fileName).delete();
			try {
				PrintWriter writer = new PrintWriter(this.fileName);
				double[] targets = this.target.computeTarget();
				double[] samples = this.target.computeSample(state);
				String[] labels = this.target.createLabels();
				writer.println("label\ttarget\tsample");
				for (int i = 0; i < targets.length; i++) {
					writer.println(labels[i] + "\t"+ targets[i] + "\t" + samples[i]);
				}
				writer.flush();
				writer.close();
			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}
	}

	@Override
	public void end() {
	}

}
