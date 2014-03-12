/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information.
*/

package dap4.core.dmr;

import dap4.core.util.DapException;
import dap4.core.util.Escape;

/**
 * The official format for an error response is defined here:
 * http://docs.opendap.org/index.php/DAP4_Web_Services_v3#DAP4_Error_Response.
 * Basically, the response looks something like this.
 * <Error httpcode="...">
 * <Message>...</Message>
 * <Context>...</Context>
 * <OtherInformation>...</OtherInformation>
 * </Error>
 */

public class ErrorResponse
{

    //////////////////////////////////////////////////
    // Instance variables

    int code = 0;
    String message = null;
    String context = null;
    String otherinfo = null;

    //////////////////////////////////////////////////
    // Constructor(s)

    public ErrorResponse() {}

    public ErrorResponse(int httpcode, String message,
                         String context, String other)
    {
	setCode(code);
	setMessage(message);
	setContext(context);
	setOtherInfo(other);
    }

    //////////////////////////////////////////////////
    // Accessors

    public int getCode() {return code;}

    public String getRawMessage() {return message;}

    public String getMessage() // Return XML escaped version
    {
	return (message == null ? null : Escape.entityEscape(message));
    }

    public String getRawContext() {return context;}

    public String getContext() // Return XML escaped version
    {
	return (context == null ? null : Escape.entityEscape(context));
    }

    public String getRawOtherInfo() {return otherinfo;}

    public String getOtherInfo() // Return XML escaped version
    {
	return (otherinfo == null ? null : Escape.entityEscape(otherinfo));
    }

    public void setCode(int code) {this.code = code;}
    public void setMessage(String message) {this.message = message;}
    public void setContext(String context) {this.context = context;}
    public void setOtherInfo(String other) {this.otherinfo = other;}

    //////////////////////////////////////////////////
    // Converters

    /**
     * Convert an ErrorResponse to the equivalent XML
     * @return  XML representation of the ErrorResponse
     */
    public String buildXML()
    {
        StringBuilder response = new StringBuilder();
        response.append("<Error");
        if(code > 0)
            response.append(String.format(" httpcode=\"%d\"", code));
        response.append(">\n");
        if(message != null)
            response.append("<Message>" + getMessage() + "</Message>\n");
        if(context != null)
            response.append("<Context>" + getContext() + "</Context>\n");
        if(otherinfo != null)
            response.append("<OtherInformation>" + getOtherInfo() + "</OtherInformation>\n");
        return response.toString();
    }

    /**
     * Convert an ErrorResponse to the equivalent DapException.
     * @return  DapException representation of the ErrorResponse
     */
    public DapException buildException()
    {
        String XML = buildXML();
        DapException dapex = new DapException(XML).setCode(code);
        return dapex;
    }

}
