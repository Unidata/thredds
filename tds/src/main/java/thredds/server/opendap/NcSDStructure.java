/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDStructure.java 51 2006-07-12 17:13:13Z caron $

package thredds.server.opendap;

import opendap.servers.*;
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
      super(Variable.getDAPName(s));
    this.ncVar = s;

    for (BaseType aList : list) addVariable(aList, 0);
    memberBTlist = list;
  }

  public NcSDStructure( NcSDStructure org, StructureData sdata) {
    super(org.getEncodedName());
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
