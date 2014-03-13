/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4shared;

/**
Define the enum for the possible Request/Response modes
*/
public enum RequestMode {
    DMR("dmr"),
    DAP("dap"),
    DSR("dsr"),
    CAPABILITIES(null),
    ERROR(null);

    private String extension;
    RequestMode(String extension) {this.extension = extension;}
    public String extension() {return extension;}
    static public RequestMode modeFor(String s)
    {
        for(RequestMode mode: RequestMode.values())  {
            if(mode.extension() != null && s.equalsIgnoreCase(mode.extension)
               || s.equalsIgnoreCase("."+mode.extension))
                    return mode;
        }
        return null;
    }

    public String toString() {return extension;}
}


