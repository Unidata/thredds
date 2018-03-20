/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

// $Id: NcDAS.java 51 2006-07-12 17:13:13Z caron $


package thredds.server.opendap;

import opendap.dap.DASException;
import ucar.nc2.*;
import ucar.nc2.constants.CDM;
import ucar.nc2.dods.*;
import ucar.ma2.DataType;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

import opendap.dap.AttributeExistsException;

/**
 * Netcdf DAS object
 *
 * @author jcaron
 */

public class NcDAS extends opendap.dap.DAS {
  static private org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(NcDAS.class);

  private Map<String, Dimension> usedDims = new HashMap<>(50);

  /**
   * Create a DAS for this netcdf file
   */
  NcDAS(NetcdfFile ncfile) {

    // Variable attributes
    Iterator iter = ncfile.getVariables().iterator();
    while (iter.hasNext()) {
      Variable v = (Variable) iter.next();
      doVariable(v, null);
    }

    // Global attributes
    opendap.dap.AttributeTable gtable = new opendap.dap.AttributeTable("NC_GLOBAL");
    int count = addAttributes(gtable, null, ncfile.getGlobalAttributes().iterator());
    if (count > 0)
      try {
        addAttributeTable("NC_GLOBAL", gtable);
      } catch (AttributeExistsException e) {
        log.error("Cant add NC_GLOBAL", e);
      }

    // unlimited  dimension
    iter = ncfile.getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension d = (Dimension) iter.next();
      if (d.isUnlimited()) {
        opendap.dap.AttributeTable table = new opendap.dap.AttributeTable("DODS_EXTRA");
        try {
          table.appendAttribute("Unlimited_Dimension", opendap.dap.Attribute.STRING, d.getShortName());
          addAttributeTable("DODS_EXTRA", table);
        } catch (Exception e) {
          log.error("Error adding Unlimited_Dimension", e);
        }
        break;
      }
    }

    // unused dimensions
    opendap.dap.AttributeTable dimTable = null;
    iter = ncfile.getDimensions().iterator();
    while (iter.hasNext()) {
      Dimension d = (Dimension) iter.next();
      if (null == usedDims.get(d.getShortName())) {
        if (dimTable == null) dimTable = new opendap.dap.AttributeTable("EXTRA_DIMENSION");
        try {
          dimTable.appendAttribute(d.getShortName(), opendap.dap.Attribute.INT32, Integer.toString(d.getLength()));
        } catch (Exception e) {
          log.error("Error adding Unlimited_Dimension", e);
        }
      }
    }
    if (dimTable != null)
      try {
        addAttributeTable("EXTRA_DIMENSION", dimTable);
      } catch (AttributeExistsException e) {
        log.error("Cant add EXTRA_DIMENSION", e);
      }

  }

  private void doVariable(Variable v, opendap.dap.AttributeTable parentTable) {

    for (Dimension dim :  v.getDimensions()) {
      if (dim.isShared())
        usedDims.put(dim.getShortName(), dim);
    }

    //if (v.getAttributes().size() == 0) return; // LOOK DAP 2 say must have empty

    // The variable names as taken from the variable,
    // are not dap escaped, so we need to make sure that happens.
    String name = Variable.getDAPName(v);

    opendap.dap.AttributeTable table;

    if (parentTable == null) {
      table = new opendap.dap.AttributeTable(name);
      try {
        addAttributeTable(name, table);
      } catch (AttributeExistsException e) {
        log.error("Cant add " + name, e);
      }
    } else {
      table = parentTable.appendContainer(name);
    }

    addAttributes(table, v, v.getAttributes().iterator());

    if (v instanceof Structure) {
      Structure s = (Structure) v;
      for (Variable nv : s.getVariables()) {
        doVariable(nv, table);
      }
    }
  }

  private int addAttributes(opendap.dap.AttributeTable table, Variable v, Iterator iter) {
    int count = 0;
    boolean isVbyte = (v != null && (v.getDataType() == DataType.BYTE));

    // always indicate if byte is signed or not ; see JIRA issue TDS-334
    if (isVbyte)
      try {
        table.appendAttribute(CDM.UNSIGNED, opendap.dap.Attribute.STRING, v.getDataType().isUnsigned() ? "true" : "false");
      } catch (DASException e) {
        log.error("Error appending unsigned attribute ", e);
      }

    // add attribute table for this variable
    while (iter.hasNext()) {
      Attribute att = (Attribute) iter.next();
      if (isVbyte && att.getShortName().equalsIgnoreCase(CDM.UNSIGNED)) continue; // got this covered

      int dods_type = DODSNetcdfFile.convertToDODSType(att.getDataType());

      try {
        // The attribute names as taken from the variable are not escaped, so we need to make sure that happens.
        String attName = att.getShortName();
        if (att.isString()) {
          /* do in Attribute.print()
          String value = EscapeStrings.backslashEscape(att.getStringValue(),"\"\\");
          table.appendAttribute(attName, dods_type, value);
          */
          table.appendAttribute(attName, dods_type, att.getStringValue());

        } else {
          // cant send signed bytes
          if (att.getDataType() == DataType.BYTE) {
            boolean signed = false;
            for (int i = 0; i < att.getLength(); i++) {
              if (att.getNumericValue(i).byteValue() < 0)
                signed = true;
            }
            if (signed) // promote to signed short
              dods_type = opendap.dap.Attribute.INT16;
          }

          for (int i = 0; i < att.getLength(); i++)
            table.appendAttribute(attName, dods_type, att.getNumericValue(i).toString());
        }
        count++;

      } catch (Exception e) {
        log.error("Error appending attribute " + att.getShortName() + " = " + att.getStringValue(), e);
      }
    } // loop over variable attributes

    // kludgy thing to map char arrays to DODS Strings
    if ((v != null) && (v.getDataType().getPrimitiveClassType() == char.class)) {
      int rank = v.getRank();
      int strlen = (rank == 0) ? 0 : v.getShape(rank - 1);
      Dimension dim = (rank == 0) ? null : v.getDimension(rank - 1);
      try {
        opendap.dap.AttributeTable dodsTable = table.appendContainer("DODS");
        dodsTable.appendAttribute("strlen", opendap.dap.Attribute.INT32, Integer.toString(strlen));
        if ((dim != null) && dim.isShared())
          dodsTable.appendAttribute("dimName", opendap.dap.Attribute.STRING, dim.getShortName());
        count++;
      } catch (Exception e) {
        log.error("Error appending attribute strlen\n", e);
      }
    }

    return count;
  }

}
