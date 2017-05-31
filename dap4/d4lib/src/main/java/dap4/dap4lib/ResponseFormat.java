/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4lib;

/**
Define the enum for the possible Response formats
*/

public enum ResponseFormat {
    TEXT("txt","text/plain"),
    XML("xml","text/xml"),
    HTML("html","text/html"),
    PROTOBUF("proto3","application/protobuf3");

    private String format;
    private String mimetype;

    ResponseFormat(String format, String mimetype)
    {
	this.format = format;
	this.mimetype = mimetype;
    }

    public String format() {return format;}
    public String mimetype() {return mimetype;}
    // Currently always utf-8
    public String charset() {return "utf-8";} 

    static public ResponseFormat formatFor(String s)
    {
        if(s == null) return null;
        for(ResponseFormat format: ResponseFormat.values())  {
	    if(s.equalsIgnoreCase(format.format)
	       || s.equalsIgnoreCase("."+format.format))
		    return format;
        }
        return null;
    }

    public String toString() {return format;}
}


