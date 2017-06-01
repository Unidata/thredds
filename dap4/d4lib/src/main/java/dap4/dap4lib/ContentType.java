/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/


package dap4.dap4lib;

public class ContentType
{
    protected RequestMode mode;
    protected ResponseFormat format;
    protected String mimetype;

    public ContentType(RequestMode mode, ResponseFormat format)
    {
        this.mode = mode;
        this.format = format;
        this.mimetype = format.mimetype();
    }

    public RequestMode getRequestMode()
    {
        return this.mode;
    }

    public ResponseFormat getResponseFormat()
    {
        return this.format;
    }

    public String getMimeType()
    {
        return this.mimetype;
    }

    public ContentType
    setMimeType(String mt)
    {
        this.mimetype = mt;
        return this;
    }

    public String toString()
    {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        if(this.mode != null) buf.append(this.mode);
        buf.append(",");
        if(this.format != null) buf.append(this.format);
        buf.append(",");
        if(mimetype != null) {
            buf.append("'");
            buf.append(this.mimetype);
            buf.append("'");
        }
        buf.append("}");
        return buf.toString();
    }

}
    
