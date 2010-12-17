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
package ucar.unidata.geoloc;

/**
 * Implementation of Station
 * @author caron
 */
public class StationImpl extends EarthLocationImpl implements Station {
  protected String name, desc, wmoId;

  protected StationImpl() {};

  public StationImpl( String name, String desc, String wmoId, double lat, double lon, double alt) {
    super( lat, lon, alt);
    this.name = name;
    this.desc = desc;
    this.wmoId = wmoId;
  }

  /**
   * Station name or id. Must be unique within the collection
   * @return station name or id. May not be null.
   */
  public String getName() { return name; }

  /**
   * Station description
   * @return station description
   */
  public String getDescription() { return desc; }

  /**
   * WMO station id
   * @return WMO station id, or null
   */
  public String getWmoId() { return wmoId; }

  /////

  protected void setName(String name) { this.name = name; }
  protected void setDescription(String desc) { this.desc = desc; }
  protected void setWmoId(String wmoId) { this.wmoId = wmoId; }

  public int compareTo(Station so) {
    return name.compareTo( so.getName());
  }

  public String toString() {
    return "name="+name+" desc="+desc+" "+super.toString();
  }

}
