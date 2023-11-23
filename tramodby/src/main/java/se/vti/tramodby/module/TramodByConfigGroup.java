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
 */
package se.vti.tramodby.module;

import org.matsim.core.config.ReflectiveConfigGroup;

/**
 * This class is the Tramod By module for MATSim and contains getters and setters
 * for relevant attributes.
 * 
 */
public class TramodByConfigGroup extends ReflectiveConfigGroup {

	public static final String NAME = "tramod_by";

	public TramodByConfigGroup() {
		super(NAME);
	}

	String zoneDefinitionFile = null;

	@StringGetter("zoneDefinitionFile")
	public String getZoneDefinitionFile() {
		return this.zoneDefinitionFile;
	}

	@StringSetter("zoneDefinitionFile")
	public void setZoneDefinitionFile(final String zoneDefinitionFile) {
		this.zoneDefinitionFile = zoneDefinitionFile;
	}

	private Integer startTime = null;

	@StringGetter("startTime_s")
	public int getStartTime() {
		return this.startTime;
	}

	@StringSetter("startTime_s")
	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	private Integer binSize = null;

	@StringGetter("binSize_s")
	public int getBinSize() {
		return this.binSize;
	}

	@StringSetter("binSize_s")
	public void setBinSize(int binSize) {
		this.binSize = binSize;
	}

	private Integer binCount = null;

	@StringGetter("binCount")
	public int getBinCount() {
		return this.binCount;
	}

	@StringSetter("binCount")
	public void setBinCount(int binCount) {
		this.binCount = binCount;
	}
	
	String odFilePrefix = null;

	@StringGetter("odFilePrefix")
	public String getOdFilePrefix() {
		return this.odFilePrefix;
	}

	@StringSetter("odFilePrefix")
	public void setOdFilePrefix(final String odFilePrefix) {
		this.odFilePrefix = odFilePrefix;
	}

	private Integer odFileStartIndex = null;

	@StringGetter("odFileStartIndex")
	public int getOdFileStartIndex() {
		return this.odFileStartIndex;
	}

	@StringSetter("odFileStartIndex")
	public void setOdFileStartIndex(int odFileStartIndex) {
		this.odFileStartIndex = odFileStartIndex;
	}
	
	public String getOdFileName(int bin) {
		return this.getOdFilePrefix() + (this.getOdFileStartIndex() + bin) + ".txt";
	}

	String costFilePrefix = null;

	@StringGetter("costFilePrefix")
	public String getCostFilePrefix() {
		return this.costFilePrefix;
	}

	@StringSetter("costFilePrefix")
	public void setCostFilePrefix(final String costFilePrefix) {
		this.costFilePrefix = costFilePrefix;
	}
	
	public String getCostFileName(int bin) {
		return this.getCostFilePrefix() + (this.getOdFileStartIndex() + bin) + ".txt";
	}

	private Double samplingRate = null;

	@StringGetter("samplingRate")
	public double getSamplingRate() {
		return this.samplingRate;
	}

	@StringSetter("samplingRate")
	public void setSamplingRate(double samplingRate) {
		this.samplingRate = samplingRate;
	}
	
	private Integer sampledLinksPerZone = null;

	@StringGetter("sampledLinksPerZone")
	public int getSampledLinksPerZone() {
		return this.sampledLinksPerZone;
	}

	@StringSetter("sampledLinksPerZone")
	public void setSampledLinksPerZone(int sampledLinksPerZone) {
		this.sampledLinksPerZone = sampledLinksPerZone;
	}

	
}
