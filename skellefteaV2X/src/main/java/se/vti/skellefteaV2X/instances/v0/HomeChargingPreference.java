package se.vti.skellefteaV2X.instances.v0;

import java.util.List;
import java.util.Random;

import se.vti.roundtrips.model.Episode;
import se.vti.roundtrips.model.ParkingEpisode;
import se.vti.roundtrips.preferences.PreferenceComponent;
import se.vti.roundtrips.single.Location;
import se.vti.skellefteaV2X.electrifiedroundtrips.single.ElectrifiedRoundTrip;
import se.vti.skellefteaV2X.model.ElectrifiedLocation;
import se.vti.skellefteaV2X.model.ElectrifiedScenario;
import se.vti.skellefteaV2X.model.ElectrifiedVehicleState;

public class HomeChargingPreference extends PreferenceComponent<ElectrifiedRoundTrip>{


	private final ElectrifiedScenario scenario;

	private final Location location;

	private final double minCharging_kWh;
	private final double homeChargeShare;
	
	private final Random r = new Random();

	public HomeChargingPreference(ElectrifiedScenario scenario, Location location, double minCharging_kWh, double homeChargeShare) {
		this.scenario = scenario;
		this.location = location;
		this.minCharging_kWh = minCharging_kWh;
		this.homeChargeShare=homeChargeShare;
	}

	private double amountChargedAtHome_kWh(ElectrifiedRoundTrip roundTrip) {
		double amount_kWh = 0.0;
		ParkingEpisode<ElectrifiedLocation, ElectrifiedVehicleState> home = (ParkingEpisode<ElectrifiedLocation, ElectrifiedVehicleState>) roundTrip.getEpisodes().get(0);
		if (this.location.equals(home.getLocation())){
			amount_kWh += home.getFinalState().getBatteryCharge_kWh() - home.getInitialState().getBatteryCharge_kWh();
		}

		return amount_kWh;
	}

	@Override
	public boolean accept(ElectrifiedRoundTrip roundTrip) {
		double rnd = r.nextDouble();
		if (rnd<=this.homeChargeShare) {
			// System.out.println("home charge amount: "+this.amountChargedAtHome_kWh(roundTrip));
			return (this.amountChargedAtHome_kWh(roundTrip) >= this.minCharging_kWh);
		}
		return false;
	}

	@Override
	public double logWeight(ElectrifiedRoundTrip roundTrip) {
		return this.amountChargedAtHome_kWh(roundTrip) / this.scenario.getMaxCharge_kWh() - 1.0;
	}



}
