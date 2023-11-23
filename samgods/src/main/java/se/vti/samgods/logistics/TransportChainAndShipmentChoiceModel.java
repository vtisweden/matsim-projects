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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

import se.vti.samgods.legacy.Samgods.Commodity;

/**
 * 
 * @author GunnarF
 *
 */
public class TransportChainAndShipmentChoiceModel {

	public interface UtilityFunction {

		public double computeUtility(Shipment shipment, ShipmentCost shipmentCost);

	}

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

		public final Shipment shipment;

		public final ShipmentCost shipmentCost;

		public final double utility;

		public Alternative(final ShipmentSizeClass shipmentSizeClass, final Shipment shipment) {
			this.shipmentSizeClass = shipmentSizeClass;
			this.shipment = shipment;
			this.shipmentCost = transportCostModel.computeCost(shipment);
			this.utility = utilityFunction.computeUtility(shipment, shipmentCost);
		}

		@Override
		public String toString() {
			return "TODO";
		}
	}

	private final TransportCostModel transportCostModel;

	private final UtilityFunction utilityFunction;

	public TransportChainAndShipmentChoiceModel(TransportCostModel transportCostModel,
			UtilityFunction utilityFunction) {
		this.transportCostModel = transportCostModel;
		this.utilityFunction = utilityFunction;
	}

	public List<Alternative> createChoiceSet(List<TransportChain> transportChains, double totalShipmentSize_ton,
			Commodity commodity) {
		final ArrayList<Alternative> result = new ArrayList<>(
				transportChains.size() * ShipmentSizeClass.values().length);
		for (ShipmentSizeClass sizeClass : ShipmentSizeClass.values()) {
			if (totalShipmentSize_ton >= sizeClass.upperValue_ton) {
				for (TransportChain transportChain : transportChains) {
					final double shipmentSize_ton = sizeClass.upperValue_ton;
					final double frequency_1_yr = totalShipmentSize_ton / shipmentSize_ton;
					final double monetaryValue = shipmentSize_ton
							* this.transportCostModel.getMonetaryValue_1_ton(commodity);
					final Shipment shipment = new Shipment(commodity, transportChain, shipmentSize_ton, frequency_1_yr,
							monetaryValue);
					result.add(new Alternative(sizeClass, shipment));
				}
			}
		}
		return result;
	}

	public static void main(String[] args) {

		TransportChainAndShipmentChoiceModel model = new TransportChainAndShipmentChoiceModel(new TransportCostModel() {

			@Override
			public UnitCost getUnitCost(Id<Node> node) {
				return null;
			}

			@Override
			public UnitCost getUnitCost(TransportLeg leg) {
				return null;
			}

			@Override
			public double getMonetaryValue_1_ton(Commodity commodity) {
				return 0.0;
			}

			@Override
			public ShipmentCost computeCost(Shipment shipment) {
				return new ShipmentCost() {
					public double getTransportDuration_h() {
						return 0.0;
					}

					public double getTransportCost() {
						return 0.0;
					}

					public double getCapitalCost() {
						return 0.0;
					}

					public double getValueDensity() {
						return 0.0;
					}

				};
			}
		}, new UtilityFunction() {

			@Override
			public double computeUtility(Shipment shipment, ShipmentCost shipmentCost) {
				return 0;
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
