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
// Sample code from John that parses HTML and looks for URLs


package thredds.util;

import javax.swing.text.AttributeSet;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Enumeration;

public class DodsURLExtractor {
  private HTMLEditorKit.Parser parser;
  private ArrayList urlList;
  private URL baseURL;

  private boolean wantURLS = false;
  private String title;
  private boolean isTitle;

  private StringBuffer textBuffer;
  private boolean wantText = false;
  private boolean debug = false;

  /** Constructor */
  public DodsURLExtractor() {
    ParserGetter kit = new ParserGetter();
    parser = kit.getParser();
  }

  /** Extract all A-HREF contained URLS from the given URL and return in List */
  public ArrayList extract(String url) throws IOException {
    if (debug) System.out.println(" URLextract="+url);

    baseURL = new URL(url);
    InputStream in = baseURL.openStream();
    InputStreamReader r = new InputStreamReader(filterTag(in));
    HTMLEditorKit.ParserCallback callback = new CallerBacker();

    urlList = new ArrayList();
    wantURLS = true;
    wantText = false;
    parser.parse(r, callback, false);

    return urlList;
  }

  /** Extract text content from the given URL and return in String */
  public String getTextContent(String url) throws IOException {
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
    BufferedReader buffIn = new BufferedReader(new InputStreamReader(in));
    ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);

    String line = buffIn.readLine();
    while ( line != null ) {
      String lline = line.toLowerCase();
      if ( 0 <= lline.indexOf( "<meta " ) )  // skip meta tags
        continue;
      //System.out.println("--"+line);
      bos.write( line.getBytes() );
      line = buffIn.readLine();
    }
    buffIn.close();

    return new ByteArrayInputStream( bos.toByteArray());
  }


  private class CallerBacker extends HTMLEditorKit.ParserCallback {

    private boolean wantTag( HTML.Tag tag) {
      return (tag == HTML.Tag.H1 || tag == HTML.Tag.H2
       || tag == HTML.Tag.H3 || tag == HTML.Tag.H4
       || tag == HTML.Tag.H5 || tag == HTML.Tag.H6);
    }

    public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes,
int position) {
      isTitle = (tag == HTML.Tag.TITLE);

      //System.out.println(" "+tag);
      if (wantURLS && tag == HTML.Tag.A)
        extractHREF( attributes);
    }

    public void handleEndTag(HTML.Tag tag, int position) {
      isTitle = false;
    }

    public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes,
int position) {
      isTitle = false; // (tag == HTML.Tag.TITLE); // ??

      //System.out.println(" "+tag);
      if (wantURLS && tag == HTML.Tag.A)
        extractHREF( attributes);
    }

    private void extractHREF(AttributeSet attributes) {
      Enumeration e = attributes.getAttributeNames();
      while (e.hasMoreElements()) {
        Object name = e.nextElement();
        String value = (String) attributes.getAttribute(name);
        //System.out.println(" name= <"+name+ ">"+" value= <"+value+ ">");
        try {
          if (name == HTML.Attribute.HREF) {
            URL u = baseURL.toURI().resolve( value ).toURL();
            String urlName = u.toString();
            if (urlList != null)
              urlList.add( u.toString());
            if (debug) System.out.println(" extracted URL= <"+urlName+ ">");
          }
        } catch (MalformedURLException ex) {
          System.err.println(ex);
          System.err.println(baseURL);
          System.err.println(value);
          ex.printStackTrace();
        }
        catch( URISyntaxException ex)
        {
          System.err.println( ex );
          System.err.println( baseURL );
          System.err.println( value );
          ex.printStackTrace();
        }
      } // while
    } // extractHREF

    public void handleText(char[] text, int position) {
      if (isTitle) title = new String( text);

      if (wantText) {
        textBuffer.append( text);
        textBuffer.append( ' ');
      }
    }

  } // Callerbacker

  private class ParserGetter extends HTMLEditorKit {
    // purely to make this method public
    public HTMLEditorKit.Parser getParser(){
      return super.getParser();
    }
  }
}