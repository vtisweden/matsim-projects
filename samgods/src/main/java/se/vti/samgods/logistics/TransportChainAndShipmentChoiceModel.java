/**
 * se.vti.samgods
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
package se.vti.samgods.logistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import se.vti.samgods.logistics.Samgods.Commodity;
import se.vti.samgods.logistics.TransportCostModel.SingleShipmentCost;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChainAndShipmentChoiceModel {

	public enum ShipmentSizeClass {

		SIZE01(1e-3, 51), SIZE02(51, 201), SIZE03(201, 801), SIZE04(801, 3001), SIZE05(3001, 7501), SIZE06(7501, 12501),
		SIZE07(12501, 20001), SIZE08(20001, 30001), SIZE09(30001, 35001), SIZE10(35001, 40001), SIZE11(40001, 45001),
		SIZE12(45001, 100001), SIZE13(100001, 200001), SIZE14(200001, 400001), SIZE15(400001, 800001),
		SIZE16(800001, 2500000);

		public static final double MIN_SHIPMENT_SIZE = 1e-3;

		public final double lowerValue_ton;
		public final double upperValue_ton;

		private ShipmentSizeClass(double lowerValue_ton, double upperValue_ton) {
			if (lowerValue_ton < MIN_SHIPMENT_SIZE) {
				throw new IllegalArgumentException();
			}
			if (upperValue_ton < lowerValue_ton) {
				throw new IllegalArgumentException();
			}
			this.lowerValue_ton = lowerValue_ton;
			this.upperValue_ton = upperValue_ton;
		}

		public double representativeValue_ton() {
			return (this.lowerValue_ton + this.upperValue_ton) / 2.0;
		}

		@Override
		public String toString() {
			return "size[" + this.lowerValue_ton + "," + this.upperValue_ton + ")tons";
		}
	};

	public class Alternative {

		public final ShipmentSizeClass shipmentSizeClass;

		public final TransportChain transportChain;

		public final SingleShipmentCost singleShipmentCost;

		public final double frequency_1_yr;

		public Alternative(ShipmentSizeClass shipmentSizeClass, TransportChain transportChain,
				SingleShipmentCost singleShipmentCost, final double shipmentFrequency_1_yr) {
			this.shipmentSizeClass = shipmentSizeClass;
			this.transportChain = transportChain;
			this.singleShipmentCost = singleShipmentCost;
			this.frequency_1_yr = shipmentFrequency_1_yr;
		}

		public double getTotalMonetaryCost() {
			return this.singleShipmentCost.getMonetaryCost() * this.frequency_1_yr;
		}

		@Override
		public String toString() {
			return this.shipmentSizeClass + "; " + this.transportChain.getLegs() + "; singleShipmentCost="
					+ this.singleShipmentCost.getMonetaryCost() + "; frequency=" + this.frequency_1_yr + "; totalCost="
					+ this.getTotalMonetaryCost();

		}

	}

	private final TransportCostModel transportCostModel;

	public TransportChainAndShipmentChoiceModel(TransportCostModel transportCostModel) {
		this.transportCostModel = transportCostModel;
	}

	public List<Alternative> createChoiceSet(List<TransportChain> transportChains, double totalShipmentSize_ton,
			Commodity commodity) {
		final ArrayList<Alternative> result = new ArrayList<>(
				transportChains.size() * ShipmentSizeClass.values().length);
		for (ShipmentSizeClass sizeClass : ShipmentSizeClass.values()) {
			if (totalShipmentSize_ton >= sizeClass.upperValue_ton) {
				for (TransportChain transportChain : transportChains) {
					final SingleShipmentCost costForOneLargeShipment = this.transportCostModel
							.computeSingleShipmentCost(transportChain, commodity, sizeClass.upperValue_ton);
					final double frequencyOfLargeShipments_1_yr = totalShipmentSize_ton / sizeClass.upperValue_ton;
					result.add(new Alternative(sizeClass, transportChain, costForOneLargeShipment,
							frequencyOfLargeShipments_1_yr));
				}
			}
		}
		return result;
	}

	public static void main(String[] args) {

		TransportChainAndShipmentChoiceModel model = new TransportChainAndShipmentChoiceModel(new TransportCostModel() {

			@Override
			public SingleShipmentCost computeSingleShipmentCost(TransportChain transportChain, Commodity commodity,
					double shipmentSize_ton) {
				return new SingleShipmentCost() {

					@Override
					public TransportChain getTransportChain() {
						return transportChain;
					}

					@Override
					public Commodity getCommmodity() {
						return commodity;
					}

					@Override
					public double getAmount_ton() {
						return shipmentSize_ton;
					}

					@Override
					public double getMonetaryCost() {
						return 1.0 / (shipmentSize_ton);
					}
				};
			}
		});

		TransportChain testChain = new TransportChain() {

			@Override
			public List<TransportLeg> getLegs() {
				return null;
			}
		};

		for (Alternative alt : model.createChoiceSet(Arrays.asList(testChain), 10000, null)) {
			System.out.println(alt);
		}
	}
}
