/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.core.util;

/**
Define the enum for the possible Response/Response modes
*/
public enum ResponseFormat {
    TEXT("txt"),
    XML("xml"),
    HTML("html"),
    NONE(null);

    private String format;
    ResponseFormat(String format) {this.format = format;}
    public String format() {return format;}
    static public ResponseFormat formatFor(String s)
    {
        for(ResponseFormat format: ResponseFormat.values())  {
	    if(s.equalsIgnoreCase(format.format)
	       || s.equalsIgnoreCase("."+format.format))
		    return format;
        }
        return null;
    }

    public String toString() {return format;}
}


