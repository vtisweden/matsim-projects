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

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.logistics.TransportChain;
import se.vti.samgods.logistics.TransportEpisode;

/**
 * 
 * @author GunnarF
 *
 */
@SuppressWarnings("serial")
public class InsufficientDataException extends Exception {

	private static Logger log = Logger.getLogger(InsufficientDataException.class);

	private static LinkedList<InsufficientDataException> trace = new LinkedList<>();

	static {
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				StringBuffer msg = new StringBuffer(
						"\n--------------------------------------------------------------------------------------------------------------------------------------------\n");
				for (InsufficientDataException e : trace) {
					msg.append("  " + e.toString() + "\n");
				}
				msg.append(
						"--------------------------------------------------------------------------------------------------------------------------------------------");
				log.warn(msg);
			}
		}));
	}

	private Class<?> throwClass;

	private Class<?> catchClass = null;
	private String catchMessage = null;

	public InsufficientDataException(Class<?> throwClass, String throwMessage) {
		super(throwMessage);
		this.throwClass = throwClass;
	}

	public synchronized void log(Class<?> catchClass, String catchMessage) {
		this.catchMessage = catchMessage;
		this.catchClass = catchClass;
		trace.add(this);
		log.warn(this.toString());
	}

	public InsufficientDataException(Class<?> throwClass, String throwMessage, SamgodsConstants.Commodity commodity,
			OD od, SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry) {
		this(throwClass, throwMessage + " " + context(commodity, od, mode, isContainer, containsFerry));
	}

	public InsufficientDataException(Class<?> throwClass, String throwMessage, TransportEpisode episode) {
		this(throwClass, throwMessage + " " + context(episode.getCommodity(), episode.getOD(), episode.getMode(),
				episode.isContainer(), episode.containsFerry()));
	}

	public InsufficientDataException(Class<?> throwClass, String throwMessage, TransportChain chain) {
		this(throwClass, throwMessage + " "
				+ context(chain.getCommodity(), chain.getOD(), null, chain.isContainer(), chain.containsFerry()));
	}

	public synchronized void log(Class<?> catchClass, String catchMessage, SamgodsConstants.Commodity commodity, OD od,
			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry) {
		this.log(catchClass, catchMessage + " " + context(commodity, od, mode, isContainer, containsFerry));
	}

	public synchronized void log(Class<?> catchClass, String catchMessage, TransportEpisode episode) {
		this.log(catchClass, catchMessage + " " + context(episode.getCommodity(), episode.getOD(), episode.getMode(),
				episode.isContainer(), episode.containsFerry()));
	}

	public synchronized void log(Class<?> catchClass, String catchMessage, TransportChain chain) {
		this.log(catchClass, catchMessage + " "
				+ context(chain.getCommodity(), chain.getOD(), null, chain.isContainer(), chain.containsFerry()));
	}

	public synchronized void log() {
		this.log(this.throwClass, "Dito");
	}

	private synchronized static String context(SamgodsConstants.Commodity commodity, OD od,
			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry) {
		List<String> msgList = new LinkedList<>();
		if (commodity != null) {
			msgList.add("commodity=" + commodity);
		}
		if (mode != null) {
			msgList.add("mode=" + mode);
		}
		if (isContainer != null) {
			msgList.add("isContainer=" + isContainer);
		}
		if (containsFerry != null) {
			msgList.add("containsFerry=" + containsFerry);
		}
		return msgList.stream().collect(Collectors.joining(", "));
	}

	public String toString() {
		return this.throwClass.getSimpleName() + "[" + this.getMessage() + "] ==> " + this.catchClass.getSimpleName()
				+ "[" + this.catchMessage + "]";
	}

}
