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
package se.vti.samgods.transportation;

import se.vti.samgods.InsufficientDataException;

/**
 * 
 * @author GunnarF
 *
 */
public class DetailedTransportCost extends BasicTransportCost {

	public final double loadingCost;
	public final double unloadingCost;
	public final double transferCost;
	public final double moveCost;

	public final double loadingDuration_h;
	public final double unloadingDuration_h;
	public final double transferDuration_h;
	public final double moveDuration_h;

	private DetailedTransportCost(double amount_ton, double loadingCost, double unloadingCost, double transferCost,
			double moveCost, double loadingDuration_h, double unloadingDuration_h, double transferDuration_h,
			double moveDuration_h) {

		super(amount_ton, loadingCost + unloadingCost + transferCost + moveCost,
				loadingDuration_h + unloadingDuration_h + transferDuration_h + moveDuration_h);

		this.loadingCost = loadingCost;
		this.unloadingCost = unloadingCost;
		this.transferCost = transferCost;
		this.moveCost = moveCost;

		this.loadingDuration_h = loadingDuration_h;
		this.unloadingDuration_h = unloadingDuration_h;
		this.transferDuration_h = transferDuration_h;
		this.moveDuration_h = moveDuration_h;
	}

	@Override
	public DetailedTransportCost computeUnitCost() {
		return new DetailedTransportCost(1.0, this.loadingCost / this.amount_ton, this.unloadingCost / this.amount_ton,
				this.transferCost / this.amount_ton, this.moveCost / this.amount_ton, this.loadingDuration_h,
				this.unloadingDuration_h, this.transferDuration_h, this.moveDuration_h);
	}

	// -------------------- BUILDER --------------------

	public static class Builder {
		private Double amount_ton;

		private Double loadingCost;
		private Double unloadingCost;
		private Double transferCost;
		private Double moveCost;

		private Double loadingDuration_h;
		private Double unloadingDuration_h;
		private Double transferDuration_h;
		private Double moveDuration_h;

		public Builder() {
		}

		private double sum(Double sum, double addend) {
			if (sum == null) {
				return addend;
			} else {
				return sum + addend;
			}
		}

		public Builder addAmount_ton(Double amount_ton) {
			this.amount_ton = this.sum(this.amount_ton, amount_ton);
			return this;
		}

		public Builder addLoadingCost(Double loadingCost) {
			this.loadingCost = this.sum(this.loadingCost, loadingCost);
			return this;
		}

		public Builder addUnloadingCost(Double unloadingCost) {
			this.unloadingCost = this.sum(this.unloadingCost, unloadingCost);
			return this;
		}

		public Builder addTransferCost(Double transferCost) {
			this.transferCost = this.sum(this.transferCost, transferCost);
			return this;
		}

		public Builder addMoveCost(Double moveCost) {
			this.moveCost = this.sum(this.moveCost, moveCost);
			return this;
		}

		public Builder addLoadingDuration_h(Double loadingDuration_h) {
			this.loadingDuration_h = this.sum(this.loadingDuration_h, loadingDuration_h);
			return this;
		}

		public Builder addUnloadingDuration_h(Double unloadingDuration_h) {
			this.unloadingDuration_h = this.sum(this.unloadingDuration_h, unloadingDuration_h);
			return this;
		}

		public Builder addTransferDuration_h(Double transferDuration_h) {
			this.transferDuration_h = this.sum(this.transferDuration_h, transferDuration_h);
			return this;
		}

		public Builder addMoveDuration_h(Double moveDuration_h) {
			this.moveDuration_h = this.sum(this.moveDuration_h, moveDuration_h);
			return this;
		}

		public DetailedTransportCost build() throws InsufficientDataException {
			try {
				// Null values raise exception when cast to primitive double.
				return new DetailedTransportCost(this.amount_ton, this.loadingCost, this.unloadingCost,
						this.transferCost, this.moveCost, this.loadingDuration_h, this.unloadingDuration_h,
						this.transferDuration_h, this.moveDuration_h);
			} catch (Exception e) {
				throw new InsufficientDataException(this.getClass(),
						"Insufficent data to build detailed transport cost.");
			}
		}
	}
}
