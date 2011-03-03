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


/**
 * Hold information about GrADS attributes
 * @author         Don Murray - CU/CIRES
 */
public class GradsAttribute {

    /** the global identifier */
    public static final String GLOBAL = "global";

    /** Grads String identifier */
    public static final String STRING = "String";

    /** Grads Btye identifier */
    public static final String BYTE = "Byte";

    /** GrADS Int16 identifier */
    public static final String INT16 = "Int16";

    /** GrADS UInt16 identifier */
    public static final String UINT16 = "UInt16";

    /** GrADS Int32 identifier */
    public static final String INT32 = "Int32";

    /** GrADS UInt32 identifier */
    public static final String UINT32 = "UInt32";

    /** GrADS Float32 identifier */
    public static final String FLOAT32 = "Float32";

    /** GrADS Float64 identifier */
    public static final String FLOAT64 = "Float64";

    /** variable name */
    private String varName;

    /** attribute type */
    private String type;

    /** attribute name */
    private String attrName;

    /** attribute value */
    private String attrValue;

    /**
     * Create a GradsAttribute
     *
     * @param vName  the variable
     * @param aType  the attribute type
     * @param aName  the attribute name
     * @param aValue  the attribute value
     */
    public GradsAttribute(String vName, String aType, String aName,
                          String aValue) {
        this.varName   = vName;
        this.type      = aType;
        this.attrName  = aName;
        this.attrValue = aValue;
    }

    /**
     * Parse an attribute spec
     *
     * @param attrSpec  the attribute spec (e.g. @ precip String units mm/day)
     *
     * @return the associated attribute
     */
    public static GradsAttribute parseAttribute(String attrSpec) {
        String[]     toks = attrSpec.split("\\s+");
        StringBuffer buf  = new StringBuffer();
        for (int i = 4; i < toks.length; i++) {
            buf.append(toks[i]);
            buf.append(" ");
        }

        // toks[0] is "@"
        return new GradsAttribute(toks[1], toks[2], toks[3],
                                  buf.toString().trim());
    }

    /**
     * Get the variable this is associated with
     *
     * @return  the variable name or GLOBAL
     */
    public String getVariable() {
        return varName;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @return the attrName
     */
    public String getName() {
        return attrName;
    }

    /**
     * @return the attrValue
     */
    public String getValue() {
        return attrValue;
    }

}

