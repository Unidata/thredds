/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
package thredds.server.ncSubset.params;

import java.util.List;

import thredds.server.ncSubset.validation.BoundingBoxConstraint;

/**
 * @author mhermida
 *
 */
@BoundingBoxConstraint
public class StationRequestParamsBean {
	
	//BBOX
	public Double north;
	public Double south;
	public Double east;
	public Double west;
	
	//STNS
	public List<String> stn;

	/**
	 * @return the north
	 */
	public Double getNorth() {
		return north;
	}

	/**
	 * @param north the north to set
	 */
	public void setNorth(Double north) {
		this.north = north;
	}

	/**
	 * @return the south
	 */
	public Double getSouth() {
		return south;
	}

	/**
	 * @param south the south to set
	 */
	public void setSouth(Double south) {
		this.south = south;
	}

	/**
	 * @return the east
	 */
	public Double getEast() {
		return east;
	}

	/**
	 * @param east the east to set
	 */
	public void setEast(Double east) {
		this.east = east;
	}

	/**
	 * @return the west
	 */
	public Double getWest() {
		return west;
	}

	/**
	 * @param west the west to set
	 */
	public void setWest(Double west) {
		this.west = west;
	}

	/**
	 * @return the stn
	 */
	public List<String> getStn() {
		return stn;
	}

	/**
	 * @param stn the stn to set
	 */
	public void setStn(List<String> stn) {
		this.stn = stn;
	}
	
}
