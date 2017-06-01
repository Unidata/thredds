/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4lib;

/**
 * Define the enum for the possible Request/Response modes
 */
public enum RequestMode
{
    DMR("dmr", "application/vnd.opendap.dap4.dataset-metadata", ResponseFormat.XML),
    DAP("dap", "application/vnd.opendap.dap4.data", ResponseFormat.SERIAL),
    DSR("dsr", "application/vnd.opendap.dap4.dataset-services", ResponseFormat.XML),
    CAPABILITIES(null, null, ResponseFormat.XML),
    ERROR(null, null, ResponseFormat.XML);

    private String extension;
    private String normative;
    private ResponseFormat defaultformat;

    RequestMode(String extension, String norm, ResponseFormat fmt)
    {
        this.extension = extension;
        this.normative = norm;
        this.defaultformat = fmt;
    }

    public String extension()
    {
        return this.extension;
    }

    public String normative()
    {
        return this.normative;
    }

    public ResponseFormat defaultFormat()
    {
        return this.defaultformat;
    }

    static public RequestMode modeFor(String s)
    {
        for(RequestMode mode : RequestMode.values()) {
            if(mode.extension() != null && s.equalsIgnoreCase(mode.extension)
                    || s.equalsIgnoreCase("." + mode.extension))
                return mode;
        }
        return null;
    }

}


