package opendap.dap.parser;

import opendap.dap.parser.Dapparse.*;
import opendap.dap.parser.ParseException;
import opendap.dap.DAP2Exception;
import java.io.*;

public class ErrorParser extends DapParser
{
    public ErrorParser(java.io.InputStream stream)
    {
        super(stream);
    }

    public void ErrorObject(DAP2Exception exception)
	throws ParseException
    {
	/* Invoke the parser */
	Errorbody();
	/* set the exception contents */
	if(svcerr) {
	    exception.setErrorMessage(message);
	    exception.setProgramSource(prog);
	    try {
	        int n = (code == null?-1:Integer.decode((String)code));
	        exception.setErrorCode(n);
	    } catch (NumberFormatException nfe) {
		throw new ParseException("Error code is not a legal integer");
	    }
	    try {
	        int n = (ptype == null?-1:Integer.decode((String)ptype));
	        exception.setProgramType(n);
	    } catch (NumberFormatException nfe) {
		throw new ParseException("Error program type is not a legal integer");
	    }
        } else
	    throw new ParseException("Errorbody not found");
    }
}
