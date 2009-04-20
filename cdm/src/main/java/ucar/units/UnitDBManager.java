// $Id: UnitDBManager.java 64 2006-07-12 22:30:50Z edavis $
/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.units;

import java.io.Serializable;

/**
 * Provides support for managing a default unit database.
 * 
 * @author Steven R. Emmerson
 * @version $Id: UnitDBManager.java 64 2006-07-12 22:30:50Z edavis $
 */
public final class UnitDBManager implements Serializable {
	private static final long	serialVersionUID	= 1L;
	/**
	 * The singleton instance of the default unit database.
	 * 
	 * @serial
	 */
	private static UnitDB		instance;

	/**
	 * Gets the default unit database.
	 * 
	 * @return The default unit database.
	 * @throws UnitDBException
	 *             The default unit database couldn't be created.
	 */
	public static final UnitDB instance() throws UnitDBException {
		synchronized (UnitDBManager.class) {
			if (instance == null) {
				instance = StandardUnitDB.instance();
			}
		}
		return instance;
	}

	/**
	 * Sets the default unit database. You'd better know what you're doing if
	 * you call this method!
	 * 
	 * @param instance
	 *            The unit database to be made the default one.
	 */
	public static final synchronized void setInstance(final UnitDB instance) {
		UnitDBManager.instance = instance;
	}
}
