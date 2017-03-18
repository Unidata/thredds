/* Copyright 2009, UCAR/Unidata and OPeNDAP, Inc.
   See the LICENSE file for more information. */

package dap4.servlet;

import dap4.core.util.IndentWriter;
import dap4.dap4lib.DapProtocol;
import dap4.dap4lib.RequestMode;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Generate the DSR for a dataset.
 * Currently only generates a minimal DSR.
 */

public class DapDSR
{

    //////////////////////////////////////////////////
    // Constants

    static final boolean DEBUG = false;

    //////////////////////////////////////////////////
    // Constructor(s)

    public DapDSR()
    {
    }

    //////////////////////////////////////////////////
    // Accessors

    //////////////////////////////////////////////////
    // API

    public String generate(String dataseturl)
        throws IOException
    {
        StringWriter sw = new StringWriter();
        IndentWriter printer = new IndentWriter(sw);
        printer.marginPrintln("<DatasetServices");
        printer.indent(2);
        printer.marginPrintln("xmlns=\"http://xml.opendap.org/ns/DAP/4.0/dataset-services#\">");
        printer.outdent();
        printer.marginPrint("<DapVersion>");
        printer.print(DapProtocol.X_DAP_VERSION);
        printer.println("</DapVersion>");
        printer.marginPrint("<ServerSoftwareVersion>");
        printer.print(DapProtocol.X_DAP_SERVER);
        printer.println("</ServerSoftwareVersion>");

        printer.marginPrintln("<Service title=\"DAP4 Dataset Services\"");
        printer.indent(3);
        printer.marginPrintln("role=\"http://services.opendap.org/dap4/dataset-services\">");
        printer.outdent(3);
        printer.indent();
        printer.marginPrint("<link type=\"");
        printer.print(DapProtocol.contenttypes.get(RequestMode.DSR).contenttype);
        printer.println("\"");
        printer.indent(2);
        printer.marginPrint("href=\"");
        printer.print(dataseturl);
        printer.println("\">");
        printer.outdent(2);
        printer.indent();
        printer.marginPrintln("<alt type=\"text/xml\"/>");
        printer.outdent();
        printer.marginPrintln("</link>");
        printer.marginPrintln("<link type=\"text/xml\"");
        printer.indent(2);
        printer.marginPrint("href=\"");
        printer.print(dataseturl);
        printer.println(".xml\"/>");
        printer.outdent(2);
        printer.outdent();
        printer.marginPrintln("</Service>");

        printer.marginPrintln("<Service title=\"DAP4 Dataset Metadata\"");
        printer.indent(3);
        printer.marginPrintln("role=\"http://services.opendap.org/dap4/dataset-metadata\">");
        printer.outdent(3);
        printer.indent();
        printer.marginPrint("<link type=\"");
        printer.print(DapProtocol.contenttypes.get(RequestMode.DMR).contenttype);
        printer.println("\"");
        printer.indent(2);
        printer.marginPrint("href=\"");
        printer.print(dataseturl);
        printer.println(".dmr\">");
        printer.outdent(2);
        printer.indent();
        printer.marginPrintln("<alt type=\"text/xml\"/>");
        printer.outdent();
        printer.marginPrintln("</link>");
        printer.marginPrintln("<link type=\"text/xml\"");
        printer.indent(2);
        printer.marginPrint("href=\"");
        printer.print(dataseturl);
        printer.println(".dmr.xml\"/>");
        printer.outdent(2);
        printer.outdent();
        printer.marginPrintln("</Service>");

        printer.marginPrintln("<Service title=\"DAP4 Dataset Data\"");
        printer.indent(2);
        printer.marginPrintln("role=\"http://services.opendap.org/dap4/data\">");
        printer.outdent(2);
        printer.indent();
        printer.marginPrint("<link type=\"");
        printer.print(DapProtocol.contenttypes.get(RequestMode.DAP).contenttype);
        printer.println("\"");
        printer.indent(2);
        printer.marginPrint("href=\"");
        printer.print(dataseturl);
        printer.println(".dap\"/>");
        printer.outdent(2);
        printer.outdent();
        printer.marginPrintln("</Service>");
        printer.outdent();
        printer.marginPrintln("</DatasetServices>");

        printer.flush();
        printer.close();
        sw.close();
        return sw.toString();
    }

} // DapDSR
