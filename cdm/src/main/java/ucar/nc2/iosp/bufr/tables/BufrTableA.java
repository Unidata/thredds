/*
 * Copyright 1997-2008 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package ucar.nc2.iosp.bufr.tables;

import java.util.Map;
import java.util.Formatter;

/**
 * Created by IntelliJ IDEA.
 * User: Robb Kambic
 * Date: Oct 6, 2008
 * Time: 8:40:01 AM
 * To change this template use File | Settings | File Templates.
 */
public class BufrTableA implements TableADataCategory
{
  String name;
  String location;
  Map<Short, String> map;

  public BufrTableA(String name, String location, Map<Short, String> map ) {
    this.name = name;
    this.location = location;
    this.map = map;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public Map<Short, String> getMap() {
    return map;
  }

  public void setMap(Map<Short, String> map) {
    this.map = map;
  }

  public String getDataCategory( short cat ) {
    return map.get( Short.valueOf( cat ) );
  }



  public void show( Formatter out) {

  }
   
}
