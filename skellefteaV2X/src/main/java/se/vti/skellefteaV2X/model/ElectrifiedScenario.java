/**
 * se.vti.skellefeaV2X
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
package se.vti.skellefteaV2X.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

import se.vti.roundtrips.model.Scenario;
import se.vti.roundtrips.preferences.Preferences;
import se.vti.roundtrips.single.PossibleTransitionFactory;
import se.vti.roundtrips.single.PossibleTransitions;
import se.vti.roundtrips.single.RoundTripDepartureProposal;
import se.vti.roundtrips.single.RoundTripLocationProposal;
import se.vti.roundtrips.single.RoundTripProposal;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.PossibleElectrifiedTransitions;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.RoundTripChargingProposal;
import se.vti.utils.misc.metropolishastings.MHAlgorithm;

/**
 * 
 * @author GunnarF
 *
 */
public class ElectrifiedScenario extends se.vti.roundtrips.model.Scenario<ElectrifiedLocation> {

	private final Random rnd;

	private double chargingRate_kW = 11.0;
	private double maxCharge_kWh = 60.0;
	private double consumptionRate_kWh_km = 0.2;
	private double defaultSpeed_km_h = 60.0;
	private boolean allowHomeCharging = true;

	public ElectrifiedScenario(Random rnd) {
		super(new ElectrifiedLocationFactory());
		this.rnd = rnd;
	}

	public ElectrifiedScenario() {
		this(new Random());
	}

	@Override
	public void setDistance_km(ElectrifiedLocation from, ElectrifiedLocation to, double dist_km) {
		super.setDistance_km(from, to, dist_km);
		super.setTime_h(from, to, dist_km / this.defaultSpeed_km_h);
	}

	public double getChargingRate_kW() {
		return chargingRate_kW;
	}

	public double getMaxCharge_kWh() {
		return maxCharge_kWh;
	}

	public double getConsumptionRate_kWh_km() {
		return consumptionRate_kWh_km;
	}

	public double getDefaultSpeed_km_h() {
		return defaultSpeed_km_h;
	}

	public void setChargingRate_kW(double chargingRate_kW) {
		this.chargingRate_kW = chargingRate_kW;
	}

	public void setMaxCharge_kWh(double maxCharge_kWh) {
		this.maxCharge_kWh = maxCharge_kWh;
	}

	public void setConsumptionRate_kWh_km(double consumptionRate_kWh_km) {
		this.consumptionRate_kWh_km = consumptionRate_kWh_km;
	}

	public void setDefaultSpeed_km_h(double defaultSpeed_km_h) {
		this.defaultSpeed_km_h = defaultSpeed_km_h;
	}

	public MHAlgorithm<ElectrifiedRoundTrip> createMHAlgorithm(
			Preferences<ElectrifiedRoundTrip> preferences, ElectrifiedSimulator simulator) {

		double locationProposalWeight = 0.1;
		double departureProposalWeight = 0.6;
		double chargingProposalWeight = 0.3;

//		final RoundTripConfiguration<ElectrifiedLocation> configuration = new RoundTripConfiguration<>(
//				this.getMaxParkingEpisodes(), getBinCnt(), locationProposalWeight, departureProposalWeight,
//				chargingProposalWeight, doNothingWeight);
//		configuration.addLocations(this.getLocationsView());

		RoundTripProposal<ElectrifiedRoundTrip> proposal0 = new RoundTripProposal<>(simulator,
				this.rnd);
		proposal0.addProposal(new RoundTripLocationProposal<ElectrifiedRoundTrip, ElectrifiedLocation>(this,
				new PossibleTransitionFactory<ElectrifiedLocation, ElectrifiedRoundTrip>() {
					@Override
					public PossibleTransitions<ElectrifiedLocation> createPossibleTransitions(
							ElectrifiedRoundTrip state, Scenario<ElectrifiedLocation> scenario) {
						return new PossibleElectrifiedTransitions(state, scenario);
					}
				}), locationProposalWeight);
		proposal0.addProposal(new RoundTripDepartureProposal<>(this), departureProposalWeight);
		proposal0.addProposal(new RoundTripChargingProposal(this.rnd), chargingProposalWeight);

//		SimulatedRoundTripProposal proposal = new SimulatedRoundTripProposal(proposal0, simulator);
		MHAlgorithm<ElectrifiedRoundTrip> algo = new MHAlgorithm<ElectrifiedRoundTrip>(proposal0, preferences,
				new Random());

		ElectrifiedRoundTrip initialState = new ElectrifiedRoundTrip(
				Arrays.asList(
						new ArrayList<>(this.getLocationsView()).get(this.rnd.nextInt(this.getLocationsView().size()))),
				Arrays.asList(this.rnd.nextInt(this.getBinCnt())), Arrays.asList(this.rnd.nextBoolean()), this.rnd);
		initialState.setEpisodes(simulator.simulate(initialState));
		algo.setInitialState(initialState);

		return algo;
	}

	public boolean isAllowHomeCharging() {
		return allowHomeCharging;
	}

	public void setAllowHomeCharging(boolean allowHomeCharging) {
		this.allowHomeCharging = allowHomeCharging;
	}
}
