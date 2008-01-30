/*
 * $Id: IDV-Style.xjs,v 1.3 2007/02/16 19:18:30 dmurray Exp $
 *
 * Copyright 1997-2007 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */




package ucar.nc2.iosp.gempak;


import ucar.nc2.iosp.grid.GridParameter;


import ucar.unidata.util.StringUtil;

import java.io.*;

// When ucar.unidata.util is in common, revert to using this
//import ucar.unidata.util.IOUtil;
import java.net.URL;
import java.net.URLConnection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Class to hold a lookup of gempak parameters
 *
 *
 * @author IDV Development Team
 * @version $Revision: 1.3 $
 */
public class GempakParameterTable {

    /** table to hold the  values */
    private static HashMap<String, GridParameter> paramMap =
        new HashMap<String, GridParameter>(256);

    /** table to hold the template values */
    private static HashMap<String, GridParameter> templateParamMap =
        new HashMap<String, GridParameter>(20);

    /** indices of breakpoints in the table */
    private static int[] indices = {
        0, 4, 38, 59, 72, 79, 90, 97
    };

    /** lengths */
    private static int[] lengths = {
        4, 33, 21, 13, 7, 11, 6, 6
    };

    /**
     * Create a new table.
     */
    public GempakParameterTable() {}

    /*
ID# NAME                             UNITS                GNAM         SCALE   MISSING  HZREMAP DIRECTION
    */

    /**
     * Add parameters from the table
     *
     * @param tbl   table location
     *
     * @throws IOException   problem reading table.
     */
    public static void addParameters(String tbl) throws IOException {
        //String content = IOUtil.readContents(tbl, GempakParameterTable.class);
        InputStream is = getInputStream(tbl);
        if (is == null) {
            throw new IOException("Unable to open " + tbl);
        }
        String         content = readContents(is);
        List           lines   = StringUtil.split(content, "\n", false);
        List<String[]> result  = new ArrayList<String[]>();
        for (int i = 0; i < lines.size(); i++) {
            String line  = (String) lines.get(i);
            String tline = line.trim();
            if (tline.length() == 0) {
                continue;
            }
            if (tline.startsWith("!")) {
                continue;
            }
            String[] words = new String[indices.length];
            for (int idx = 0; idx < indices.length; idx++) {
                if (indices[idx] >= tline.length()) {
                    continue;
                }
                if (indices[idx] + lengths[idx] > tline.length()) {
                    words[idx] = line.substring(indices[idx]);
                } else {
                    words[idx] = line.substring(indices[idx],
                            indices[idx] + lengths[idx]);
                }
                //if (trimWords) {
                words[idx] = words[idx].trim();
                //}
            }
            result.add(words);
        }
        for (int i = 0; i < result.size(); i++) {
            GridParameter p = makeParameter((String[]) result.get(i));
            if (p != null) {
                if (p.getName().indexOf("(") >= 0) {
                    templateParamMap.put(p.getName(), p);
                } else {
                    paramMap.put(p.getName(), p);
                }
            }
        }

    }

    /**
     * Make a parameter from the tokens
     *
     * @param words   the tokens
     *
     * @return  a grid parameter (may be null)
     */
    private static GempakGridParameter makeParameter(String[] words) {
        int    num = 0;
        String description;
        if (words[0] != null) {
            num = (int) Double.parseDouble(words[0]);
        }
        if ((words[3] == null) || words[3].equals("")) {  // no param name
            return null;
        }
        String name = words[3];
        if (name.indexOf("-") >= 0) {
            int          first = name.indexOf("-");
            int          last  = name.lastIndexOf("-");
            StringBuffer buf   = new StringBuffer(name.substring(0, first));
            buf.append("(");
            for (int i = first; i <= last; i++) {
                buf.append("\\d");
            }
            buf.append(")");
            buf.append(name.substring(last + 1));
            name = buf.toString();
        }

        if ((words[1] == null) || words[1].equals("")) {
            description = words[3];
        } else {
            description = words[1];
        }
        String unit = words[2];
        if (unit != null) {
            unit = unit.replaceAll("\\*\\*", "");
            if (unit.equals("-")) {
                unit = "";
            }
        }
        int decimalScale = 0;
        try {
            decimalScale = Integer.parseInt(words[4].trim());
        } catch (NumberFormatException ne) {
            decimalScale = 0;
        }

        return new GempakGridParameter(num, name, description, unit,
                                       decimalScale);
    }

    /**
     * Get the parameter for the given name
     *
     * @param name   name of the parameter (eg:, TMPK);
     *
     * @return  corresponding parameter or null if not found in table
     */
    public static GempakGridParameter getParameter(String name) {
        GempakGridParameter param = (GempakGridParameter) paramMap.get(name);
        if (param == null) {  // try the regex list
            Set<String> keys = templateParamMap.keySet();
            if ( !keys.isEmpty()) {
                for (Iterator iter = keys.iterator(); iter.hasNext(); ) {
                    String  key = (String) iter.next();
                    Pattern p   = Pattern.compile(key);
                    Matcher m   = p.matcher(name);
                    if (m.matches()) {
                        //System.out.println("found match " + key + " for " + name);
                        String value = m.group(1);
                        GempakGridParameter match =
                            (GempakGridParameter) templateParamMap.get(key);
                        param = new GempakGridParameter(match.getNumber(),
                                name,
                                match.getDescription() + " (" + value
                                + " hour)", match.getUnit(),
                                             match.getDecimalScale());
                        paramMap.put(name, param);
                        break;
                    }
                }
            }
        }
        return param;
    }

    /**
     * Test
     *
     * @param args ignored
     *
     * @throws IOException  problem reading the table.
     */
    public static void main(String[] args) throws IOException {
        GempakParameterTable pt = new GempakParameterTable();
        pt.addParameters("resources/nj22/tables/gempak/wmogrib3.tbl");
    }



    /**
     * Read in the bytes from the given InputStream
     * and construct and return a String.
     * Closes the InputStream argument.
     *
     * @param is   InputStream to read from
     * @return  contents as a String
     *
     * @throws IOException  problem reading contents
     */
    public static String readContents(InputStream is) throws IOException {
        return new String(readBytes(is));
    }

    /**
     * Read the bytes in the given input stream.
     *
     * @param is The input stream
     *
     * @return The bytes
     *
     * @throws IOException On badness
     */
    public static byte[] readBytes(InputStream is) throws IOException {
        int    totalRead = 0;
        byte[] content   = new byte[1000000];
        while (true) {
            int howMany = is.read(content, totalRead,
                                  content.length - totalRead);
            if (howMany < 0) {
                break;
            }
            if (howMany == 0) {
                continue;
            }
            totalRead += howMany;
            if (totalRead >= content.length) {
                byte[] tmp       = content;
                int    newLength = ((content.length < 25000000)
                                    ? content.length * 2
                                    : content.length + 5000000);
                content = new byte[newLength];
                System.arraycopy(tmp, 0, content, 0, totalRead);
            }
        }
        is.close();
        byte[] results = new byte[totalRead];
        System.arraycopy(content, 0, results, 0, totalRead);
        return results;
    }

    /**
     * Get the input stream to the given resource
     *
     * @param resourceName The resource name. May be a file, url,
     *                     java resource, etc.
     *
     * @return The input stream to the resource
     */

    public static InputStream getInputStream(String resourceName) {

        InputStream s = null;

        // Try class loader to get resource
        ClassLoader cl = GempakParameterTable.class.getClassLoader();
        s = cl.getResourceAsStream(resourceName);
        if (s != null) {
            return s;
        }

        //Try the file system
        File f = new File(resourceName);
        if (f.exists()) {
            try {
                s = new FileInputStream(f);
            } catch (Exception e) {}
        }
        if (s != null) {
            return s;
        }

        //Try it as a url
        try {
            Matcher       m = Pattern.compile(" ").matcher(resourceName);
            String        encodedUrl = m.replaceAll("%20");
            URL           dataUrl    = new URL(encodedUrl);
            URLConnection connection = dataUrl.openConnection();
            s = connection.getInputStream();
        } catch (Exception exc) {}

        return s;
    }


}

