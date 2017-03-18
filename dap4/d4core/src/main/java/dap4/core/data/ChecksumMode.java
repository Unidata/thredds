/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.data;

/**
 * Define possible checksum modes:
 *
 */
public enum ChecksumMode
{
    NONE,   // => serialized data has no checksums
    IGNORE, // => skip, but ignore any checksumming (client side)
    DMR,    // => compute checksums for DMR requests only
    DAP,    // => compute checksums for Data requests only
    ALL;    // => compute checksums for both kinds of requestsNONE, DMR, DAP, ALL;

    /**
     * Return true if the ckm mode is allowed with this, false otherwise
     * @param ckm
     * @return
     */
    public boolean
    enabled(ChecksumMode ckm)
    {
        if(ckm == null || this == NONE) return false;
        if(this == ckm) return true;
        if(this == ALL) return true;
        return false;
    }

    static public ChecksumMode
    modeFor(String s)
    {
        if(s == null || s.length() == 0)
            return DAP;
        for(ChecksumMode mode : values()) {
            if(mode.name().equalsIgnoreCase(s))
                return mode;
        }
        return null;
    }
}
