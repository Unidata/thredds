/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.dap4shared;

/**
 * Define possible checksum modes:
 * DMR  => compute checksums for DMR requests only
 * DAP => compute checksums for Note requests only
 * ALL  => compute checksums for both kinds of requests
 */
public enum ChecksumMode
{
    DMR, DAP, ALL;

    static public boolean
    enabled(RequestMode rqm, ChecksumMode ckm)
    {
        switch (ckm) {
        case DMR:  return rqm == RequestMode.DMR;
        case DAP: return rqm == RequestMode.DAP;
        case ALL: return rqm == RequestMode.DMR || rqm == RequestMode.DAP;
        }
        return false;
    }

    static public ChecksumMode
    modeFor(String s)
    {
        if(s == null || s.length() == 0)
            return DAP;
        for(ChecksumMode mode: values()) {
            if(mode.name().equalsIgnoreCase(s))
                return mode;
        }
        return null;
    }
}
