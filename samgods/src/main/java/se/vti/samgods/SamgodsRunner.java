/**
 * se.vti.samgods
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
package se.vti.samgods;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.network.Network;
import org.matsim.vehicles.VehicleUtils;
import org.matsim.vehicles.Vehicles;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.ChainChoiReader;
import se.vti.samgods.logistics.TransportDemand;
import se.vti.samgods.network.NetworkDataProvider;
import se.vti.samgods.network.NetworkReader;
import se.vti.samgods.transportation.fleet.FleetDataProvider;
import se.vti.samgods.transportation.fleet.VehiclesReader;

/**
 * TODO public members only while moving this into TestSamgods.
 * 
 * @author GunnarF
 *
 */
public class SamgodsRunner {

	private final long defaultSeed = 4711;

	private final Commodity[] defaultConsideredCommodities = Commodity.values();

	private final int defaultMaxThreads = Integer.MAX_VALUE;

	private final int defautServiceInterval_days = 7;

	private final int defaultMaxIterations = 5;

	private final boolean defaultEnforceReroute = false;

	private final double defaultSamplingRate = 1.0;

	private final double defaultScale = 1.0;

	private Random rnd;

	public List<Commodity> consideredCommodities;

	public int maxThreads;

	private double samplingRate;

	// TODO concurrency?
	public final Map<SamgodsConstants.Commodity, Integer> commodity2serviceInterval_days = new LinkedHashMap<>();

	public int maxIterations;

	public boolean enforceReroute;

	public double scale;

	private Vehicles vehicles = null;
	private FleetDataProvider fleetDataProvider = null;

	public Network network = null;
	private NetworkDataProvider networkDataProvider = null;

	public TransportDemand transportDemand = null;

	public SamgodsRunner() {
		this.setRandomSeed(this.defaultSeed);
		this.setConsideredCommodities(this.defaultConsideredCommodities);
		this.setMaxThreads(this.defaultMaxThreads);
		this.setServiceInterval_days(this.defautServiceInterval_days);
		this.setMaxIterations(this.defaultMaxIterations);
		this.setEnforceReroute(this.defaultEnforceReroute);
		this.setSamplingRate(this.defaultSamplingRate);
		this.setScale(this.defaultScale);
	}

	public SamgodsRunner setScale(double scale) {
		this.scale = scale;
		return this;
	}

	public SamgodsRunner setRandomSeed(long seed) {
		this.rnd = new Random(seed);
		return this;
	}

	public SamgodsRunner setConsideredCommodities(Commodity... consideredCommodities) {
		this.consideredCommodities = Arrays.asList(consideredCommodities);
		return this;
	}

	public SamgodsRunner setMaxThreads(int maxThreads) {
		this.maxThreads = maxThreads;
		return this;
	}

	public SamgodsRunner setSamplingRate(double samplingRate) {
		this.samplingRate = samplingRate;
		return this;
	}

	public SamgodsRunner setServiceInterval_days(int serviceInterval_days) {
		Arrays.stream(Commodity.values())
				.forEach(c -> this.commodity2serviceInterval_days.put(c, serviceInterval_days));
		return this;
	}

	public SamgodsRunner setMaxIterations(int maxIterations) {
		this.maxIterations = maxIterations;
		return this;
	}

	public SamgodsRunner setEnforceReroute(boolean enforceReroute) {
		this.enforceReroute = enforceReroute;
		return this;
	}

	public SamgodsRunner loadVehicles(String vehicleParametersFileName, String transferParametersFileName,
			TransportMode samgodsMode) throws IOException {
		if (this.vehicles == null) {
			this.vehicles = VehicleUtils.createVehiclesContainer();
		}
		final VehiclesReader fleetReader = new VehiclesReader(this.vehicles);
		fleetReader.load_v12(vehicleParametersFileName, transferParametersFileName, samgodsMode);
		return this;
	}

	public SamgodsRunner loadNetwork(String nodesFile, String linksFile) throws IOException {
		this.network = new NetworkReader().load(nodesFile, linksFile);
		return this;
	}

	public SamgodsRunner loadTransportDemand(String demandFilePrefix, String demandFileSuffix) {
		this.transportDemand = new TransportDemand();
		for (Commodity commodity : this.consideredCommodities) {
			new ChainChoiReader(commodity, transportDemand).setSamplingRate(this.samplingRate, new Random(4711))
					.parse(demandFilePrefix + commodity.twoDigitCode() + demandFileSuffix);
		}
		return this;
	}
	
	public FleetDataProvider getOrCreateFleetDataProvider() {
		if (this.fleetDataProvider == null) {
			this.fleetDataProvider = new FleetDataProvider(this.vehicles);
		}
		return this.fleetDataProvider;
	}

	public NetworkDataProvider getOrCreateNetworkDataProvider() {
		if (this.networkDataProvider == null) {
			this.networkDataProvider = new NetworkDataProvider(this.network);
		}
		return this.networkDataProvider;
	}
	
	// -------------------- PREPARE CONSOLIDATION UNITS --------------------
	
	public void createOrLoadConsolidationUnits(String consolidationUnitsFileName) {
		
		// TODO CONTINUE HERE
		
	}
	
	
	
	
	
}
