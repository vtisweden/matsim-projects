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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import floetteroed.utilities.Tuple;

/**
 * 
 * @author GunnarF
 *
 * @param <F> first tuple element
 * @param <S> second tuple element
 */
public class TupleGrouping<F, S> {

	// -------------------- INNER CLASS --------------------

	public class Group {
		private final Set<Tuple<F, S>> tuples;

		private Group(Set<Tuple<F, S>> tuples) {
			this.tuples = new LinkedHashSet<>(tuples);
		}

		public Set<Tuple<F, S>> getTuplesView() {
			return Collections.unmodifiableSet(this.tuples);
		}

		public Set<F> getAllFirstView() {
			return this.tuples.stream().map(t -> t.getA()).collect(Collectors.toSet());
		}

		public Set<S> getAllSecondView() {
			return this.tuples.stream().map(t -> t.getB()).collect(Collectors.toSet());
		}

		public boolean contains(F first, S second) {
			return this.tuples.contains(new Tuple<>(first, second));
		}
	}

	// -------------------- MEMBERS --------------------

	private final Set<F> firstDomain = new LinkedHashSet<>();

	private final Set<S> secondDomain = new LinkedHashSet<>();

	private final Map<Tuple<F, S>, Group> tuple2group = new LinkedHashMap<>();

	// -------------------- CONSTRUCTION --------------------

	public TupleGrouping(Iterable<F> firstDomain, Iterable<S> secondDomain) {
		for (F first : firstDomain) {
			this.firstDomain.add(first);
		}
		for (S second : secondDomain) {
			this.secondDomain.add(second);
		}
	}

	// -------------------- INTERNALS --------------------

	private <T> List<T> replaceNullByAll(List<T> list, Set<T> all) {
		if (list == null) {
			return new ArrayList<>(all);
		} else {
			return list;
		}
	}

	// -------------------- IMPLEMENTATION --------------------

	public int groupCnt() {
		return this.tuple2group.size();
	}

	public Set<F> getAllFirst() {
		return this.tuple2group.keySet().stream().map(t -> t.getA()).collect(Collectors.toSet());
	}

	public Set<S> getAllSecond() {
		return this.tuple2group.keySet().stream().map(t -> t.getB()).collect(Collectors.toSet());
	}

	public Collection<Group> groupsView() {
		return Collections.unmodifiableCollection(this.tuple2group.values());
	}

	public void addCartesian(List<F> firstList, List<S> secondList) {
		firstList = this.replaceNullByAll(firstList, this.firstDomain);
		secondList = this.replaceNullByAll(secondList, this.secondDomain);

		final Set<Tuple<F, S>> tuples = new LinkedHashSet<>(firstList.size() * secondList.size());
		for (F first : firstList) {
			for (S second : secondList) {
				tuples.add(new Tuple<>(first, second));
			}
		}

		final Group newGroup = new Group(tuples);
		for (Tuple<F, S> tuple : newGroup.tuples) {
			Group oldGroup = this.tuple2group.get(tuple);
			if (oldGroup != null) {
				oldGroup.tuples.remove(tuple);
			}
			this.tuple2group.put(tuple, newGroup);
		}
	}

	public Group getGroup(F first, S second) {
		return this.tuple2group.get(new Tuple<>(first, second));
	}
}
