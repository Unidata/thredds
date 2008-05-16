/*
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

package ucar.nc2.util.xml;

import ucar.unidata.util.Format;
import ucar.nc2.NetcdfFileWriteable;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.Attribute;
import ucar.ma2.DataType;
import ucar.ma2.ArrayChar;
import ucar.ma2.InvalidRangeException;
import ucar.ma2.Array;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.awt.*;
import java.text.Normalizer;

import junit.framework.TestCase;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestUnicode extends TestCase {

  public TestUnicode(String name) {
    super(name);
  }

  public void testCharsets() {
    Map<String,Charset> map = Charset.availableCharsets();
    for (String key : map.keySet()) {
      Charset cs = map.get(key);
      System.out.println(" "+cs);
    }
    System.out.println("default= "+Charset.defaultCharset());

    System.out.println("\nFont names:");
    GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
    for (String s : env.getAvailableFontFamilyNames()) {
      System.out.println(" "+s);
    }

    int c1 = 0x1f73;

    System.out.println("\nFonts:");
    for (Font f: env.getAllFonts()) {
      f.canDisplay(c1);
      System.out.println(f.canDisplay(c1)+" "+f.getFontName());
    }
  }

  static int[] helloGreekCode = new int[] {0xce, 0x9a, 0xce, 0xb1, 0xce, 0xbb, 0xce, 0xb7, 0xce, 0xbc, 0xe1, 0xbd, 0xb3, 0xcf, 0x81, 0xce, 0xb1};

  String makeString(int[] codes, boolean debug) throws UnsupportedEncodingException {
    byte[] b = new byte[codes.length];
    for (int i=0; i<codes.length; i++)
      b[i] = (byte) codes[i];
    if (debug) System.out.println(" orgBytes= "+showBytes(b));
    String s = new String(b, "UTF-8");
    if (debug) System.out.println("convBytes= "+showString( s));
    return s;
  }

  public void testString() throws IOException {
    //int[] bi = new int[] {0xe2, 0x88, 0xae, 0x20, 0x45, 0xe2, 0x8b, 0x85, 0x64, 0x61, 0x20, 0x3d, 0x20, 0x51, 0x2c};
    //byte[] b = new byte[ helloGreek.length];
    //for (int i=0; i<helloGreek.length; i++) b[i] = (byte) helloGreek[i];

    String helloGreek = makeString(helloGreekCode, true);
    String s2 = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);

    //String lineb = new String( b, "UTF-8");
    System.out.println(" helloGreek= "+helloGreek);
    System.out.println("char values= "+showString(helloGreek));
    System.out.println("      UTF-8= "+showBytes(helloGreek.getBytes("UTF-8")));
    System.out.println("     UTF-16= "+showBytes(helloGreek.getBytes("UTF-16")));
    System.out.println("    default= "+showBytes(helloGreek.getBytes()));
    System.out.println("norm values= "+showString(s2));
    System.out.println("      UTF-8= "+showBytes(s2.getBytes("UTF-8")));
    System.out.println("     UTF-16= "+showBytes(s2.getBytes("UTF-16")));
    System.out.println("    default= "+showBytes(s2.getBytes()));

    write(helloGreek, null);
    write(helloGreek, "UTF-8");
    //writeFile(lineb, null, "C:/data/unicode/mathOut.txt");
    //writeFile(lineb, "UTF-8", "C:/data/unicode/mathOut8.txt");
    writeFile(helloGreek, null, "C:/data/unicode/mathOut.html");
    writeFile(helloGreek, "UTF-8", "C:/data/unicode/mathOut8.html");
        /*
    System.out.println("\nRussian Char=  ?");
    char[] cline = new char[] {'?','?','?','?','?','?','?','?','?','?','?','?','?','?','?','?','?'};
    for (char c : cline) {
      int i = (int) c;
      System.out.print(c+" "+i);
    }
    System.out.println();

    String line = new String(cline, 0, cline.length);
    //String line = "? E?da = Q,";
    System.out.println(line);
    System.out.println("char values= "+showString(line));
    System.out.println("      UTF-8= "+showBytes(line.getBytes("UTF-8")));
    System.out.println("     UTF-16= "+showBytes(line.getBytes("UTF-16")));
    System.out.println("    default= "+showBytes(line.getBytes()));
    write(line, null);
    write(line, "UTF-8"); */

  }

  public void makeNetCDF() throws IOException, InvalidRangeException {
    String helloGreek = makeString(helloGreekCode, true);
    helloGreek = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.println("normalized= "+showString(helloGreek));

    String filename = "C:/data/unicode/helloNorm.nc";
    NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew(filename);
    ucar.nc2.Dimension dim = ncfile.addDimension(helloGreek, 20);
    ncfile.addVariable(helloGreek, DataType.CHAR, helloGreek);
    ncfile.addVariableAttribute(helloGreek, "units", helloGreek);
    ncfile.create();
    ArrayChar.D1 data = new ArrayChar.D1(dim.getLength());
    data.setString(helloGreek);
    ncfile.write(helloGreek, data);
    ncfile.close();

    NetcdfFile nc = NetcdfFile.open(filename);
    Variable v = ncfile.findVariable(helloGreek);
    assert v != null;
    assert v.getName().equals(helloGreek);

    Attribute att = v.findAttribute("units");
    assert att != null;
    assert att.isString();
    assert(helloGreek.equals(att.getStringValue()));
    nc.close();    
  }

  // read a string = (nelems, byte array), then skip to 4 byte boundary
  static void testRAF(String filename) throws IOException {
    ucar.unidata.io.RandomAccessFile raf = new ucar.unidata.io.RandomAccessFile(filename, "r");
    int nelems = 37;
    byte[] b = new byte[nelems];
    raf.read(b);
    String line = new String(b, "UTF-8"); // all strings are considered to be UTF-8 unicode.

    System.out.println(line);
    System.out.println(showBytes(b));
    System.out.println(showBytes(line.getBytes()));
  }

  static void write(String s, String charset) throws IOException {
    Charset cs =  (charset == null) ? Charset.defaultCharset() : Charset.forName(charset);
    OutputStreamWriter outw = new OutputStreamWriter(System.out, cs);
    outw.write("OutputWriter ("+cs+")=(");
    outw.write(s);
    outw.write(")\n");
    outw.flush();
  }

  static void writeFile(String s, String charset, String filename) throws IOException {
    FileOutputStream fout = new FileOutputStream(filename);
    Charset cs =  (charset == null) ? Charset.defaultCharset() : Charset.forName(charset);
    OutputStreamWriter outw = new OutputStreamWriter(fout, cs);

    outw.write( "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
            "    \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n" +
            "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n" +
            "    <body>\n" +
            "<pre>");

    outw.write("OutputWriter ("+cs+")=(");
    outw.write(s);
    outw.write(")</pre>\n" +
            "\n" +
            "</body>\n" +
            "</html>");

    outw.flush();
    fout.close();
  }

  static void testRead(String filename) throws IOException {
    DataInputStream in = new DataInputStream( new BufferedInputStream(new FileInputStream(filename)));

    int count = 0;
    String line;
    while (null != (line = in.readLine())) {
      System.out.println("Line "+count++);
      System.out.println(line);
      System.out.println(showBytes(line.getBytes()));
    }
    in.close();
  }

  static void testUnicodeRead(String filename) throws IOException {
    BufferedReader in = new BufferedReader( new InputStreamReader(new FileInputStream(filename), "UTF-8"));

    int count = 0;
    String line;
    while (null != (line = in.readLine())) {
      System.out.println("ULine "+count++);
      System.out.println(line);
      System.out.println(showBytes(line.getBytes()));
    }
    in.close();
  }

  static public String showBytes(byte[] buff) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < buff.length; i++) {
      byte b = buff[i];
      int ub = (b < 0) ? b + 256 : b;
      if (i > 0) sbuff.append(" ");
      sbuff.append( Integer.toHexString(ub));
    }
    return sbuff.toString();
  }

  static public String showString(String s) {
    StringBuffer sbuff = new StringBuffer();
    for (int i = 0; i < s.length(); i++) {
      int c = (int) s.charAt(i);
      if (i > 0) sbuff.append(" ");
      sbuff.append( Integer.toHexString(c));
    }
    return sbuff.toString();
  }


  public static void main(String args[]) throws IOException {
    String helloGreek = new TestUnicode("dumm").makeString(helloGreekCode, true);

    //String filename = "C:/data/unicode/UTF-8-demo.html";
    //String filename = "C:/data/unicode/russian.txt";
    //testRAF(filename);
    //testUnicodeRead(filename);
    //testRead(filename);
  }

}
