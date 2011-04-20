/*
 * Copyright 1998-2011 University Corporation for Atmospheric Research/Unidata
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

package ucar.nc2.iosp.grads;


import ucar.nc2.units.SimpleUnit;


/**
 * Hold information about a GrADS variable
 * @author         Don Murray - CU/CIRES
 */
public class GradsVariable {

    /** the variable name */
    private String varName;

    /** the number of levels */
    private int numLevs;

    /** the level params */
    private int[] levVals;

    /** the level unit */
    private int unit;

    /** the level unit values */
    private int[] unitVals;

    /** the variable description */
    private String description;

    /** the unit name */
    private String unitName = null;

    /**
     * Create a variable from the descriptor
     *
     * @param varDescriptor  the variable descriptor
     */
    public GradsVariable(String varDescriptor) {
        String[] toks = varDescriptor.split("\\s+");
        varName = toks[0].trim();
        // handle new "LongVarName=>gradsname" syntax
        // used in dtype netcdf
        int arrowIndex = varName.indexOf("=>");
        if (arrowIndex > 0) {
            varName = varName.substring(arrowIndex + 2);
        }
        String levelString = toks[1];
        if (levelString.indexOf(",") > 0) {
            String[] levs = levelString.split(",");
            levVals = new int[levs.length];
            for (int i = 0; i < levs.length; i++) {
                if (levs[i].trim().isEmpty()) {
                    continue;
                }
                levVals[i] = Integer.parseInt(levs[i]);
            }
            numLevs = levVals[0];
        } else {
            numLevs = Integer.parseInt(levelString);
        }
        String unitString = toks[2];
        if (unitString.indexOf(",") > 0) {
            String[] units = unitString.split(",");
            unitVals = new int[units.length];
            for (int i = 0; i < units.length; i++) {
                if (units[i].trim().isEmpty()) {
                    continue;
                }
                unitVals[i] = Integer.parseInt(units[i]);
            }
            unit = unitVals[0];
        } else {
            unit = Integer.parseInt(unitString);
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 3; i < toks.length; i++) {
            buf.append(toks[i]);
            buf.append(" ");
        }
        description = buf.toString().trim();
        // see if there is a unit in there
        int uStart = description.indexOf("[");
        if (uStart >= 0) {
            int uEnd = description.indexOf("]");
            if (uEnd > uStart) {
                unitName = description.substring(uStart + 1, uEnd);
            }
        } else {  // see if we can parse (unit)
            int uEnd = description.lastIndexOf(")");
            if (uEnd > 0) {
                uStart = description.lastIndexOf("(");
                if (uStart >= 0 && uEnd > uStart) {
                    String possibleUnitName = description.substring(uStart
                                                  + 1, uEnd);
                    try {
                        SimpleUnit u = SimpleUnit.factoryWithExceptions(
                                           possibleUnitName);
                        unitName = possibleUnitName;
                    } catch (Exception e) {
                        unitName = null;
                    }
                }
            }
        }
    }

    /**
     * Get the variable name
     *
     * @return the varName
     */
    public String getName() {
        return varName;
    }

    /**
     * Get the number of levels
     *
     * @return the numLevs
     */
    public int getNumLevels() {
        return numLevs;
    }

    /**
     * Get the variable description
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the unit name
     *
     * @return the unitName
     */
    public String getUnitName() {
        return unitName;
    }

    /**
     * Return a String representation of this object
     *
     * @return a String representation of this object
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("Variable: ");
        buf.append(varName);
        buf.append("\n");
        buf.append("\tNum levels: ");
        buf.append(numLevs);
        buf.append("\n");
        buf.append("\tUnit: ");
        buf.append(unit);
        buf.append("\n");
        buf.append("\tDescription: ");
        buf.append(description);
        buf.append("\n");

        return buf.toString();
    }
}

