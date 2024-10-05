/**
 * se.vti.samgods.logistics.choicemodel
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
package se.vti.samgods.logistics.choice;

import java.util.List;

import se.vti.samgods.OD;
import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.logistics.AnnualShipment;
import se.vti.samgods.logistics.TransportChain;

/**
 * 
 * @author GunnarF
 *
 */
public class ChoiceJob {

	public static final ChoiceJob TERMINATE = new ChoiceJob(null, null, null, null) {
	};

	public final Commodity commodity;
	public final OD od;
	public final List<TransportChain> transportChains;
	public final List<AnnualShipment> annualShipments;

	public ChoiceJob(Commodity commodity, OD od, List<TransportChain> transportChains,
			List<AnnualShipment> annualShipments) {
		this.commodity = commodity;
		this.od = od;
		this.transportChains = transportChains;
		this.annualShipments = annualShipments;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[commodity=" + this.commodity + ",od=" + this.od
				+ ",numberOfTransportChains=" + (this.transportChains != null ? this.transportChains.size() : null)
				+ ",numberOfAnnualShipments=" + (this.annualShipments != null ? this.annualShipments.size() : null)
				+ "]";
	}
}
