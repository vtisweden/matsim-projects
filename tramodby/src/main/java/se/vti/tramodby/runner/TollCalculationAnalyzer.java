/**
 * se.vti.tramodby
 * 
 * Copyright (C) 2023 Gunnar Flötteröd (VTI, LiU), Rasmus Ringdahl (LiU). 
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
 */package se.vti.tramodby.runner;

import java.io.IOException;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.contrib.roadpricing.RoadPricingScheme;
import org.matsim.contrib.roadpricing.RoadPricingSchemeImpl.Cost;

import floetteroed.utilities.DynamicData;
import floetteroed.utilities.DynamicDataXMLFileIO;
import se.vti.tramodby.module.TramodByConfigGroup;

public class TollCalculationAnalyzer {

	private final double nothing = 0.0;
	private final double onlyTolled = 1.0;
	private final double onlyUsed = 2.0;
	private final double tolledAndUsed = 3.0;
	
	private final DynamicData<Id<Link>> tollData;

	public TollCalculationAnalyzer(TramodByConfigGroup tramodByConfig) {
		this.tollData = new DynamicData<>(tramodByConfig.getStartTime(), tramodByConfig.getBinSize(),
				tramodByConfig.getBinCount());
	}

	public void registerAllTollData(RoadPricingScheme scheme) {
		for (Id<Link> linkId : scheme.getTolledLinkIds()) {
			for (int bin = 0; bin < this.tollData.getBinCnt(); bin++) {
				double time_s = this.tollData.binStart_s(bin) + 0.5 * this.tollData.getBinSize_s();
				Cost cost = scheme.getLinkCostInfo(linkId, time_s, null, null);
				if ((cost != null) && (cost.amount > 0)) {
					this.tollData.put(linkId, bin, this.onlyTolled);
				}
			}
		}
	}

	public synchronized void registerUsedLink(Id<Link> linkId, double time_s) {
		final int bin = this.tollData.bin((int) time_s);
		if (bin >= this.tollData.getBinCnt()) {
			return;
		}
		final double oldValue = this.tollData.getBinValue(linkId, bin);
		if (oldValue == this.nothing) {
			this.tollData.put(linkId, bin, this.onlyUsed);
		} else if (oldValue == this.onlyTolled) {
			this.tollData.put(linkId, bin, this.tolledAndUsed);
		} else if (oldValue == this.onlyUsed) {
			// keep old value
		} else if (oldValue == this.tolledAndUsed) {
			// keep old value
		} else {
			throw new RuntimeException("unknown value " + oldValue);
		}
	}

	public void writeToFile(String fileName) {
		DynamicDataXMLFileIO<Id<Link>> fileIO = new DynamicDataXMLFileIO<Id<Link>>() {
			@Override
			protected String key2attrValue(Id<Link> key) {
				return key.toString();
			}

			@Override
			protected Id<Link> attrValue2key(String string) {
				return null;
			}
		};
		try {
			fileIO.write(fileName, this.tollData);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
