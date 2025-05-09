/*
 * Copyright 2018 Gunnar Flötteröd
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */ 
package se.vti.utils.misc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 
 * @author Gunnar Flötteröd
 *
 */
public class FractionalIterator<E> implements Iterator<E> {

	private final Iterator<E> iterator;

	private final double fraction;

	private double cumulative = 0.0;

	private E next = null;

	public FractionalIterator(final Iterator<E> iterator, final double fraction) {
		this.iterator = iterator;
		this.fraction = Math.max(0.0, Math.min(1.0, fraction));
		this.advance();
	}

	private void advance() {
		this.next = null;
		while (this.iterator.hasNext() && this.cumulative < 1.0) {
			this.next = this.iterator.next();
			this.cumulative += this.fraction;
			// if (this.cumulative < 1.0) {
			// System.out.print(".");
			// } else {
			// System.out.print("X");
			// }
		}
		if (this.cumulative >= 1.0) {
			this.cumulative -= 1.0;
		} else {
			this.next = null;
		}
	}

	@Override
	public boolean hasNext() {
		return (this.next != null);
	}

	@Override
	public E next() {
		final E result = this.next;
		this.advance();
		return result;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	public static void main(String[] args) {
		final List<Integer> all = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			all.add(i);
		}
		for (double f = 0; f <= 1.0; f += 0.1) {
			double cnt = 0;
			for (Iterator<Integer> it = new FractionalIterator<>(
					all.iterator(), f); it.hasNext();) {
				it.next();
				cnt++;
			}
			System.out.println(" should be: " + f + "; is "
					+ (cnt / all.size()));
		}
	}
}
