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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import se.vti.samgods.SamgodsConstants.Commodity;
import se.vti.samgods.SamgodsConstants.TransportMode;
import se.vti.samgods.transportation.consolidation.ConsolidationUnit;

/**
 * 
 * @author GunnarF
 *
 */
@SuppressWarnings("serial")
public class InsufficientDataException extends Exception {

	// -------------------- STATIC INTERNALS --------------------

//	private static final Logger log = Logger.getLogger(InsufficientDataException.class);
//
//	private static boolean logDuringRuntime = true;
//	private static boolean logUponShutdown = true;
//
//	private static List<InsufficientDataException> originalExceptions = new ArrayList<>();
//	private static List<InsufficientDataException> resultingExceptions = new ArrayList<>();
//
//	private static String logMsg(InsufficientDataException originalException,
//			InsufficientDataException resultingException) {
//		return (resultingException != null ? resultingException + " CAUSED BY " : "") + originalException;
//	}
//
//	static {
//		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//			@Override
//			public void run() {
//				if (logUponShutdown) {
//					StringBuffer msg = new StringBuffer(
//							"\n--------------------------------------------------------------------------------------------------------------------------------------------\n");
//					for (int i = 0; i < originalExceptions.size(); i++) {
//						msg.append(logMsg(originalExceptions.get(i), resultingExceptions.get(i)) + "\n");
//					}
//					msg.append(
//							"--------------------------------------------------------------------------------------------------------------------------------------------");
//					System.err.println(msg.toString());
//				}
//			}
//		}));
//	}

	// -------------------- GLOBAL STATIC --------------------

//	public static void setLogDuringRuntime(boolean log) {
//		logDuringRuntime = log;
//	}
//
//	public static void setLogUponShutdown(boolean log) {
//		logUponShutdown = log;
//	}
//
//	public static synchronized void log(InsufficientDataException originalException,
//			InsufficientDataException resultingException) {
//		originalExceptions.add(originalException);
//		resultingExceptions.add(resultingException);
//		if (logDuringRuntime) {
//			log.warn(logMsg(originalException, resultingException));
//		}
//	}

	// -------------------- MEMBERS --------------------

	public final Class<?> clazz;
	public final Commodity commodity;
	public final OD od;
	public final TransportMode samgodsMode;
	public final Boolean isContainer;
	public final Boolean containsFerry;

	// -------------------- CONSTRUCTION --------------------

	public InsufficientDataException(Class<?> clazz, String message, SamgodsConstants.Commodity commodity, OD od,
			SamgodsConstants.TransportMode mode, Boolean isContainer, Boolean containsFerry) {
		super(message);
		this.clazz = clazz;
		this.commodity = commodity;
		this.od = od;
		this.samgodsMode = mode;
		this.isContainer = isContainer;
		this.containsFerry = containsFerry;
	}

	public InsufficientDataException(Class<?> throwClass, String throwMessage, ConsolidationUnit consolidationUnit) {
		this(throwClass, throwMessage, consolidationUnit.commodity,
				new OD(consolidationUnit.nodeIds.get(0),
						consolidationUnit.nodeIds.get(consolidationUnit.nodeIds.size() - 1)),
				consolidationUnit.samgodsMode, consolidationUnit.isContainer, consolidationUnit.containsFerry);
	}

	public InsufficientDataException(Class<?> throwClass, String throwMessage) {
		this(throwClass, throwMessage, null, null, null, null, null);
	}

	// -------------------- IMPLEMENTATION --------------------

	public String toString() {
		final List<String> contextList = new LinkedList<>();
		if (this.commodity != null) {
			contextList.add("commodity=" + this.commodity);
		}
		if (this.od != null) {
			contextList.add("od=" + this.od);
		}
		if (this.samgodsMode != null) {
			contextList.add("mode=" + this.samgodsMode);
		}
		if (this.isContainer != null) {
			contextList.add("isContainer=" + this.isContainer);
		}
		if (this.containsFerry != null) {
			contextList.add("containsFerry=" + this.containsFerry);
		}
		return this.getMessage() + " in " + this.clazz.getSimpleName()
				+ (contextList.size() > 0 ? ", context: " + contextList.stream().collect(Collectors.joining(",")) : "");
	}
}
