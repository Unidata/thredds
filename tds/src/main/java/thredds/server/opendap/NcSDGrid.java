/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcSDGrid.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import opendap.servers.*;
import opendap.dap.BaseType;
import opendap.dap.NoSuchVariableException;
import ucar.nc2.Variable;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Wraps a netcdf variable with rank > 0, whose dimensions all
 * have coordinate variables, as an SDGrid.
 *
 * @author jcaron
 */

public class NcSDGrid extends SDGrid {

  /** Constructor.
   *  @param name of the Grid
   *  @param list of the variables, first data then maps
   */
  public NcSDGrid( String name, ArrayList list) {
      super(name);
    addVariable( (BaseType) list.get(0), ARRAY);

    for (int i=1; i<list.size(); i++)
      addVariable( (BaseType) list.get(i), MAPS);
  }

  public boolean read(String datasetName, Object specialO) throws NoSuchVariableException, IOException {

    java.util.Enumeration vars = getVariables();
    while (vars.hasMoreElements()) {
      SDArray bt = (SDArray) vars.nextElement();
      bt.read( datasetName, specialO);
    }

    setRead(true);
    return(false);
  }
}
