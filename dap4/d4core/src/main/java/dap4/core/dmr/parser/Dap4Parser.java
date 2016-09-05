/* Copyright 2012, UCAR/Unidata.
   See the LICENSE file for more information. */

package dap4.core.dmr.parser;

import dap4.core.dmr.*;
import dap4.core.util.DapException;
import dap4.core.util.DapSort;
import dap4.core.util.DapUtil;
import dap4.core.util.Escape;
import org.xml.sax.SAXException;

import java.math.BigInteger;
import java.util.*;

public interface Dap4Parser
{
    public ErrorResponse getErrorResponse();

    public DapDataset getDMR();

    public boolean parse(String input) throws SAXException;

    public void setDebugLevel(int level);
}
