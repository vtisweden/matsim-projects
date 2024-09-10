/**
 * se.vti.samgods.utils
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
package se.vti.samgods.utils;

/**
 * 
 * @author GunnarF
 *
 */
public class SingleAccessManager<T> {

	private T exclusiveObject;

	private Thread exclusiveUser = null;

	public SingleAccessManager(T exclusiveObject) {
		this.exclusiveObject = exclusiveObject;
	}

	public synchronized T acquire(Thread user) throws InterruptedException {
		while (this.exclusiveUser != null) {
			wait();
		}
		this.exclusiveUser = user;
		return exclusiveObject;
	}

	public synchronized void release(Thread user) {
		if (this.exclusiveUser == user) {
			this.exclusiveUser = null;
		}
		notifyAll();
	}
}
