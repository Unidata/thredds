// $Id: NcSDStructure.java 51 2006-07-12 17:13:13Z caron $
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
package thredds.server.opendap;

import opendap.Server.*;
import opendap.dap.BaseType;
import opendap.dap.NoSuchVariableException;

import java.io.IOException;
import java.io.DataOutputStream;
import java.util.List;
import ucar.nc2.Variable;
import ucar.nc2.Structure;
import ucar.ma2.*;

import org.slf4j.*;

/**
 * Wraps a netcdf Structure, as an SDStructure.
 *
 * @author jcaron
 */

public class NcSDStructure extends SDStructure  {
  static private Logger log = LoggerFactory.getLogger(NcSDStructure.class);

  private Structure ncVar = null;
  protected List<BaseType> memberBTlist;

  protected NcSDStructure org;
  protected StructureData sdata;

  /** Constructor.
   *  @param s the netcdf Structure
   *  @param list of the member variables
   */
  public NcSDStructure( Structure s, List<BaseType> list) {
      //super( NcDDS.escapeName(s.getShortName()));
      super((s.getShortName()));
    this.ncVar = s;

    for (BaseType aList : list) addVariable(aList, 0);
    memberBTlist = list;
  }

  public NcSDStructure( NcSDStructure org, StructureData sdata) {
    super( org.getEncodedName());
    this.org = org;
    this.sdata = sdata;
  }

   public Variable getVariable() { return ncVar; }

  // called if its scalar
  public boolean read(String datasetName, Object specialO) throws NoSuchVariableException,
    IOException {

    // read the scalar structure into memory
    StructureData sdata = ncVar.readStructure();
    setData( sdata);
    return(false);
  }

  // LOOK - should modify to use hasNetcdf.setData( StructureData) for efficiency
  public void setData(StructureData sdata) {
    int count = 0;

    StructureMembers sm = sdata.getStructureMembers();
    java.util.Enumeration vars = getVariables();
    while (vars.hasMoreElements()) {
      // loop through both structures
      HasNetcdfVariable hasNetcdf = (HasNetcdfVariable) vars.nextElement();
      StructureMembers.Member m = sm.getMember(count++);

      // extract the data and set it into the dods object
      Array data = sdata.getArray(m);
      hasNetcdf.setData( data);
    }

    setRead(true);
  }

  ////////////////////////////////////////////////////////////////////////////////
 // overrride for array of Structures
  public void serialize(String dataset,DataOutputStream sink,CEEvaluator ce,Object specialO)
                                    throws NoSuchVariableException, DAP2ServerSideException, IOException {

    if (org == null) {
      super.serialize(dataset, sink, ce, specialO);
      return;
    }

    // use the projection info in the original
    java.util.Enumeration vars = org.getVariables();

    // run through each structure member
    StructureMembers sm = sdata.getStructureMembers();

    int count = 0;
    while (vars.hasMoreElements()) {
      HasNetcdfVariable sm_org = (HasNetcdfVariable) vars.nextElement();
      boolean isProjected = ((ServerMethods) sm_org).isProject();
      if (isProjected) {
        StructureMembers.Member m = sm.getMember(count);
        sm_org.serialize( sink, sdata, m);
      }
      count++;
    }

  }
}
