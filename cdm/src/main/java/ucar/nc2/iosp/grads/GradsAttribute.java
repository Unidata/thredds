/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
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

