/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4lib;

public class ContentType
{
    protected RequestMode mode;
    protected ResponseFormat format;
    protected String mimetype;

    public ContentType(RequestMode mode, ResponseFormat format, String mime)
    {
	this.mode = mode;
	this.format = format;
	this.mimetype = mime;
    }

    public RequestMode getRequestMode() {return this.mode;}
    public ResponseFormat getResponseFormat() {return this.format;}
    public String getMimeType() {return this.mimetype;}

}
    
