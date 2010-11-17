package opendap.dap.parser;

import opendap.dap.parser.Dapparse.*;
import opendap.dap.parser.ParseException;
import java.io.*;

public class DDSParser extends DapParser
{
    public DDSParser(InputStream stream)
	throws ParseException
    {
	super(stream);
    }
}
