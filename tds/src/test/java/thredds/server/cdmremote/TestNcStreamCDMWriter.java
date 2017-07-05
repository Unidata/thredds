/*
Copyright (c) 1998-2017 University Corporation for Atmospheric Research/Unidata
See LICENSE.txt for license information.
*/

package thredds.server.cdmremote;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.nc2.*;
import ucar.nc2.stream.NcStream;
import ucar.nc2.stream.NcStreamCDMWriter;
import ucar.nc2.stream.NcStreamReader;
import ucar.unidata.util.test.UnitTestCommon;
import ucar.unidata.util.test.category.NeedsContentRoot;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@Category(NeedsContentRoot.class)
@RunWith(Parameterized.class)
public class TestNcStreamCDMWriter extends UnitTestCommon
{
    //////////////////////////////////////////////////
    // Constants

    //////////////////////////////////////////////////
    // Static variables

    protected static final Logger logger = LoggerFactory.getLogger(TestNcStreamCDMWriter.class);

    //////////////////////////////////////////////////
    // JNit methods

    @Before
    public void setup()
            throws Exception
    {
        tdsContentRootPath = System.getProperty("tds.content.root.path");
        Assert.assertTrue("tds.content.root.path not defined", tdsContentRootPath != null);
        this.path = canonjoin(tdsContentRootPath, "thredds/public/testdata", this.path);
        // Verify it exists
        File ffp = new File(this.path);
        if(!ffp.exists())
            throw new IOException("Test file does not exist: " + this.path);
        if(!ffp.canRead())
            throw new IOException("Test file not readable: " + this.path);
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> getTestParameters()
    {
        List<Object[]> result = new ArrayList<>();
        result.add(new Object[]{"testWrite.nc"});
        return result;
    }

    //////////////////////////////////////////////////
    // Instance Variables

    protected String tdsContentRootPath = null;

    String path;

    NetcdfFile ncf = null;
    NcStreamCDMWriter nscw = null;

    public TestNcStreamCDMWriter(String path)
            throws Exception
    {
        this.path = path;
    }

    @Test
    public void
    testHeader()
            throws Exception
    {
        // Open the path as a NetcdfFile
        this.ncf = NetcdfFile.open(this.path);

        // Prepare the ncstream target
        this.nscw = new NcStreamCDMWriter();

        // start with root group
        this.nscw.addRootGroup(ncf.getRootGroup());

        // Write root group contents
        traverseGroup(this.ncf.getRootGroup());

        // Extract the contents
        this.nscw.finish();
        byte[] contents = this.nscw.getContent();

        // Add the prefix info
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NcStream.writeBytes(baos, NcStream.MAGIC_HEADER);
        NcStream.writeVInt(baos, contents.length);
        NcStream.writeBytes(baos, contents);
        baos.close();
        byte[] payload = baos.toByteArray();

        //Now try to read
        ByteArrayInputStream bais = new ByteArrayInputStream(payload);

        NcStreamReader reader = new NcStreamReader();
        NetcdfFile ncresult = new NetcdfFileSubclass();
        reader.readStream(bais, ncresult);

        if(prop_visual) {
            String s = dumpmetadata(ncresult, this.path);
            visual("NcStream Read Header", s);
        }
    }

/*
    @Test
    public void
    dataTest() throws Exception
    {
    }
*/

    protected void
    traverseGroup(Group g)
            throws IOException
    {
        List<Dimension> ldim = g.getDimensions();
        for(int i = 0; i < ldim.size(); i++) {
            Dimension x = ldim.get(i);
            this.nscw.addDimension(g, x);
        }

        List<EnumTypedef> lenum = g.getEnumTypedefs();
        for(int i = 0; i < lenum.size(); i++) {
            EnumTypedef x = lenum.get(i);
            this.nscw.addEnumeration(g, x);
        }

        List<Attribute> latt = g.getAttributes();
        for(int i = 0; i < latt.size(); i++) {
            Attribute x = latt.get(i);
            this.nscw.addAttribute(g, x);
        }

        List<Variable> lvar = g.getVariables();
        for(int i = 0; i < lvar.size(); i++) {
            Variable x = lvar.get(i);
            Array vdata = null;
            this.nscw.addVariable(g, x, vdata);
        }

        // Recurse on sub-groups
        List<Group> lgrp = g.getGroups();
        for(int i = 0; i < lgrp.size(); i++) {
            Group x = lgrp.get(i);
            this.nscw.addGroup(g, x);
            traverseGroup(x);
        }

    }

    String
    dumpmetadata(NetcdfFile ncfile, String datasetname)
            throws Exception
    {
        StringWriter sw = new StringWriter();
        StringBuilder args = new StringBuilder("-strict -unsigned");
        if(datasetname != null) {
            args.append(" -datasetname ");
            args.append(datasetname);
        }
        // Print the meta-databuffer using these args to NcdumpW
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
                throw new Exception("NcdumpW failed");
        } catch (IOException ioe) {
            throw new Exception("NcdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

    String
    dumpdata(NetcdfFile ncfile, String datasetname)
            throws Exception
    {
        StringBuilder args = new StringBuilder("-strict -unsigned -vall");
        if(datasetname != null) {
            args.append(" -datasetname ");
            args.append(datasetname);
        }
        StringWriter sw = new StringWriter();
        // Dump the databuffer
        sw = new StringWriter();
        try {
            if(!ucar.nc2.NCdumpW.print(ncfile, args.toString(), sw, null))
                throw new Exception("NCdumpW failed");
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new Exception("NCdumpW failed", ioe);
        }
        sw.close();
        return sw.toString();
    }

}
