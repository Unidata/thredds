/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util.xml;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.nc2.*;
import ucar.ma2.DataType;
import ucar.ma2.ArrayChar;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.constants.CDM;

import java.io.*;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;

/**
 * Class Description.
 *
 * @author caron
 */
public class TestUnicode {
  private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  static int[] helloGreekCode = new int[] {0xce, 0x9a, 0xce, 0xb1, 0xce, 0xbb, 0xce, 0xb7, 0xce, 0xbc, 0xe1, 0xbd, 0xb3, 0xcf, 0x81, 0xce, 0xb1};

  static final byte[] MAGIC_HEADER = new byte[]{(byte) 0xad, (byte) 0xec, (byte) 0xce, (byte) 0xda};
  static final byte[] MAGIC_DATA = new byte[]{(byte) 0xab, (byte) 0xec, (byte) 0xce, (byte) 0xba};

  @Test
  public void testMagic() throws IOException {
    String s = new String(MAGIC_HEADER);
    write(s, null);
    write(s, "UTF-8");
    s = new String(MAGIC_HEADER, "UTF-8");
    write(s, null);
    write(s, "UTF-8");
  }


  String makeString(int[] codes, boolean debug) throws UnsupportedEncodingException {
    byte[] b = new byte[codes.length];
    for (int i=0; i<codes.length; i++)
      b[i] = (byte) codes[i];
    if (debug) System.out.println(" orgBytes= "+showBytes(b));
    String s = new String(b, "UTF-8");
    if (debug) System.out.println("convBytes= "+showString( s));
    return s;
  }

  @Test
  public void testString() throws IOException {
    //int[] bi = new int[] {0xe2, 0x88, 0xae, 0x20, 0x45, 0xe2, 0x8b, 0x85, 0x64, 0x61, 0x20, 0x3d, 0x20, 0x51, 0x2c};
    //byte[] b = new byte[ helloGreek.length];
    //for (int i=0; i<helloGreek.length; i++) b[i] = (byte) helloGreek[i];

    String helloGreek = makeString(helloGreekCode, true);
    String s2 ="";// = Normalizer.normalize(helloGreek, Normalizer.Form.NFC);

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
    //writeFile(helloGreek, null, TestLocalNC. "C:/data/unicode/mathOut.html");
    //writeFile(helloGreek, "UTF-8", "C:/data/unicode/mathOut8.html");
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
    helloGreek = ""; //Normalizer.normalize(helloGreek, Normalizer.Form.NFC);
    System.out.println("normalized= "+showString(helloGreek));

    String filename = "C:/data/unicode/helloNorm.nc";
    NetcdfFileWriter ncfile = NetcdfFileWriter.createNew(filename, true);
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
    assert v.getShortName().equals(helloGreek);

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
    System.out.println(showBytes(line.getBytes(CDM.utf8Charset)));
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
      System.out.println(showBytes(line.getBytes(CDM.utf8Charset)));
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
      System.out.println(showBytes(line.getBytes(CDM.utf8Charset)));
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
    //String helloGreek = new TestUnicode("dumm").makeString(helloGreekCode, true);

    //testCharsets();
    write("hello", null);
    write("hello", "UTF-8");
    write("hello", "EUC-JP");

    //String filename = "C:/data/unicode/UTF-8-demo.html";
    //String filename = "C:/data/unicode/russian.txt";
    //testRAF(filename);
    //testUnicodeRead(filename);
    //testRead(filename);
  }

}
