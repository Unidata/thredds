/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package ucar.nc2.ui.util;

import ucar.nc2.util.IO;

import javax.swing.text.*;
import javax.swing.text.html.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class URLExtracter {
  private HTMLEditorKit.Parser parser;
  private ArrayList urlList;
  private URL baseURL;

  private boolean wantURLS = false;
  private String title;
  private boolean isTitle;

  private StringBuffer textBuffer;
  private boolean wantText = false;
  private boolean debug = false,  debugIMG = false;
  private boolean dump = true;

  public URLExtracter() {
    ParserGetter kit = new ParserGetter();
    parser = kit.getParser();
  }

  public ArrayList extract(String url) throws IOException {
    if (debug) System.out.println(" URLextract from "+url);
    urlList = new ArrayList();

    baseURL = new URL(url);
    InputStream in = baseURL.openStream();
    /* if (dump) {
      System.out.println(" dump");
      IO.copy( in, System.out);
      // return urlList;
  } */

    // InputStreamReader r = new InputStreamReader(filterTag(in));
    InputStreamReader r = new InputStreamReader(in);

    //InputStreamReader r = new InputStreamReader(in);
    HTMLEditorKit.ParserCallback callback = new CallerBacker();

    wantURLS = true;
    wantText = false;
    parser.parse(r, callback, false);

    return urlList;
  }

  /* URLDoc factory(URLDocSet docset, int uid, String url) { // throws IOException {
    URLDoc doc = new URLDoc(docset, uid, url);
    if (debug) System.out.println(" URLfactory="+url);

    try {
      baseURL = new URL(url);
      InputStream in = baseURL.openStream();
      InputStreamReader r = new InputStreamReader(filterTag(in));
      HTMLEditorKit.ParserCallback callback = new CallerBacker();

      title = null;
      wantURLS = false;
      wantText = true;
      parser.parse(r, callback, false);

      if (title != null)
        doc.setSubject( title);
      return doc;

    } catch (IOException e) {
      System.out.println("URLextracter factory failed "+e);
      return null;
    }
  } */

  String getTextContent(String url) throws IOException {
    if (debug) System.out.println(" URL.getTextContent="+url);

    baseURL = new URL(url);
    InputStream in = baseURL.openStream();
    InputStreamReader r = new InputStreamReader(filterTag(in));
    HTMLEditorKit.ParserCallback callback = new CallerBacker();

    textBuffer = new StringBuffer(3000);
    wantURLS = false;
    wantText = true;
    parser.parse(r, callback, false);

    return textBuffer.toString();
  }



    // workaround for HTMLEditorKit.Parser, cant deal with "content-encoding"
  private InputStream filterTag(InputStream in) throws IOException {
    DataInputStream dins = new DataInputStream( in);
    ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);

    DataInputStream din =  new DataInputStream(new BufferedInputStream(in));
    while (din.available() > 0) {
      String line = din.readLine();
      String lline = line.toLowerCase();
      if (0 <= lline.indexOf("<meta "))  // skip meta tags
        continue;
      //System.out.println("--"+line);
      bos.write( line.getBytes());
    }
    din.close();

    if (dump) {
      System.out.println(" dumpFilter= "+ bos.toString());
    }

    return new ByteArrayInputStream( bos.toByteArray());
  }


  private class CallerBacker extends HTMLEditorKit.ParserCallback {

    private boolean wantTag( HTML.Tag tag) {
      return (tag == HTML.Tag.H1 || tag == HTML.Tag.H2
       || tag == HTML.Tag.H3 || tag == HTML.Tag.H4
       || tag == HTML.Tag.H5 || tag == HTML.Tag.H6);
    }

    public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
      if (debug) System.out.println(" handleStartTag="+tag);
      isTitle = (tag == HTML.Tag.TITLE);

      if (wantURLS && tag == HTML.Tag.A)
        extractHREF( attributes);
      if (tag == HTML.Tag.IMG)
        extractIMG( attributes);
    }

    public void handleEndTag(HTML.Tag tag, int position) {
      isTitle = false;
    }

    public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
      if (debug) System.out.println(" handleSimpleTag="+tag);
      isTitle = false; // (tag == HTML.Tag.TITLE); // ??

      //System.out.println(" "+tag);
      if (wantURLS && tag == HTML.Tag.A)
        extractHREF( attributes);

      if (tag == HTML.Tag.IMG)
        extractIMG( attributes);
    }

    private void extractHREF(AttributeSet attributes) {
      Enumeration e = attributes.getAttributeNames();
      while (e.hasMoreElements()) {
        Object name = e.nextElement();
        String value = (String) attributes.getAttribute(name);
        if (debug) System.out.println(" HREF att: name= <"+name+ ">"+" value= <"+value+ ">");
        try {
          if (name == HTML.Attribute.HREF) {
            URL u = new URL(baseURL, value);
            String urlName = u.toString();
            if (urlList != null)
              urlList.add( u.toString());
            if (debug) System.out.println(" extracted URL= <"+urlName+ ">");
          }
        } catch (MalformedURLException ex) {
          System.err.println(ex);
          System.err.println(baseURL);
          System.err.println(value);
          // ex.printStackTrace();
        }
      } // while
    } // extractHREF

    private void extractIMG(AttributeSet attributes) {
      String src = null;
      int w=0, h=0;

      Enumeration e = attributes.getAttributeNames();
      while (e.hasMoreElements()) {
        Object name = e.nextElement();
        String value = (String) attributes.getAttribute(name);
        if (debugIMG) System.out.println(" extractIMG "+name+" value= "+value);

       // if ((name == HTML.Attribute.SRC) && value.endsWith("jpg")) {
        if ((name == HTML.Attribute.SRC) && (0 < value.indexOf("jpg"))) {
          src = value;
          if (debugIMG) System.out.println(" IMG SRC= <"+src+ ">");
        }

        if (name == HTML.Attribute.WIDTH) {
          try {
            w = Integer.parseInt( value);
            if (debugIMG) System.out.println(" IMG WIDTH= <"+w+ ">");
          } catch (NumberFormatException ne) {}
        }
        if (name == HTML.Attribute.HEIGHT) {
          try {
            h = Integer.parseInt( value);
            if (debugIMG) System.out.println(" IMG HEIGHT= <"+h+ ">");
          } catch (NumberFormatException ne) {}
        }
      } // while

      if ((src != null) && (w * h > 30000)) {
        try {
          URL u = new URL(baseURL, src);
          String urlName = u.toString();
          urlList.add( u.toString());
          if (debugIMG) System.out.println(" extracted IMG URL= <"+urlName+ ">");
        } catch (MalformedURLException ex) {
          System.err.println(ex);
        }
      }

    } // extractIMG

    public void handleText(char[] text, int position) {
      if (debug) System.out.println(" handleText="+new String(text));
     /*  if (isTitle) title = new String( text);

      if (wantText) {
        textBuffer.append( text);
        textBuffer.append( ' ');
      } */
    }

    public void handleError(String errorMsg, int pos) {
      System.out.println(" error="+errorMsg+" at "+pos);
    }

  } // Callerbacker

  private class ParserGetter extends HTMLEditorKit {
    // purely to make this method public
    public HTMLEditorKit.Parser getParser(){
      return super.getParser();
    }
  }


  void extractJPG(String urlString, int depth) {
    System.out.println("extractJPG from " + urlString);

    try {
      ArrayList urls = extract(urlString);
      for (int i = 0; i < urls.size(); i++) {
        String url = (String) urls.get(i);
        System.out.println(" " + url);

        if (url.endsWith("jpg")) {
          int h = url.hashCode();
          String hs = (h < 0) ? "M" + Integer.toString( -h) : Integer.toString(h);
          String filename = dirpath + "/" + hs + ".jpg";
          System.out.println("---Extract= " +
                             IO.readURLtoFile(url, new File(filename)));
        }

        if (depth < recursionDepth) {
          if (url.endsWith("html") || url.endsWith("htm")) {
            extractJPG(url, depth + 1);
          }
        }

      }

    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("failed to download "+urlString+"\n"+e.getMessage());
    }

  }

  static String startURL = "";
  static String dirpath = "c:/tmp/test/";

  static int recursionDepth = 3;

  public static void main(String[] args) {

    File dir = new File(dirpath);
    if (!dir.exists())
      dir.mkdirs();

    URLExtracter ue = new URLExtracter();
    ue.extractJPG(startURL, 1);
  }
}