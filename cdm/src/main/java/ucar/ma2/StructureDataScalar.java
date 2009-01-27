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

package ucar.ma2;

/**
 * Class Description.
 *
 * @author caron
 * @since Jan 26, 2009
 */
public class StructureDataScalar extends StructureDataW {

  public StructureDataScalar(String name) {
    super(new StructureMembers(name));
  }

  public void addMember(String name, String desc, String units, double val) {
    StructureMembers.Member m = members.addMember(name, desc, units, DataType.DOUBLE,  new int[0]);
    ArrayDouble.D0 data = new ArrayDouble.D0();
    data.set(val);
    setMemberData(m, data);
  }

  public void addMember(String name, String desc, String units, long val) {
    StructureMembers.Member m = members.addMember(name, desc, units, DataType.LONG,  new int[0]);
    ArrayLong.D0 data = new ArrayLong.D0();
    data.set(val);
    setMemberData(m, data);
  }
}
