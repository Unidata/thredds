// $Id: NcSDByte.java 51 2006-07-12 17:13:13Z caron $
/*
 * Copyright 1997-2006 Unidata Program Center/University Corporation for
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

package dods.servers.netcdf;

import dods.dap.Server.*;

import java.io.IOException;
import java.io.DataOutputStream;

import ucar.ma2.*;
import ucar.nc2.*;
import ucar.unidata.util.StringUtil;

/**
 * Wraps a netcdf scalar byte variable.
 *
 * @author jcaron
 * @version $Revision: 51 $
 */
public class NcSDByte extends SDByte implements HasNetcdfVariable {
  private Variable ncVar;

  /**
   * Constructor
   *
   * @param ncVar : the netcdf Variable
   */
  NcSDByte(Variable ncVar) {
    super(NcDDS.escapeName(ncVar.getShortName()));
    this.ncVar = ncVar;
  }

  public Variable getVariable() { return ncVar; }


  /**
   * Read the value (parameters are ignored).
   */
  public boolean read(String datasetName, Object specialO) throws IOException {
    setData(ncVar.read());
    return (false);
  }

  public void setData(Array data) {
    ArrayByte.D0 a = (ArrayByte.D0) data;
    setValue(a.get());
    setRead(true);
  }

  public void serialize(DataOutputStream sink, StructureData sdata, StructureMembers.Member m) throws IOException {
    setValue( sdata.getScalarByte(m));
    externalize(sink);
  }
}

/* Change History:
   $Log: NcSDByte.java,v $
   Revision 1.6  2006/04/20 22:25:21  caron
   dods server: handle name escaping consistently
   rename, reorganize servlets
   update Paths doc

   Revision 1.5  2005/07/27 23:25:37  caron
   ncdods refactor, add Structure (2)

   Revision 1.4  2005/07/25 23:26:49  caron
   ncdods refactor, add Structure

   Revision 1.3  2005/01/21 00:58:11  caron
   *** empty log message ***

   Revision 1.2  2004/09/24 03:26:26  caron
   merge nj22

   Revision 1.1.1.1  2004/03/19 19:48:31  caron
   move AS code here

   Revision 1.1.1.1  2001/09/26 15:34:30  caron
   checkin beta1


 */
