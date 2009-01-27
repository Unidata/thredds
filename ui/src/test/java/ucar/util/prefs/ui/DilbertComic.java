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
package ucar.util.prefs.ui;

import java.io.*;
import java.net.URL;
import javax.swing.*;

public class DilbertComic {
  public static String todaysDilbert() throws IOException {
    // open up the webpage to today's comic
    URL url = new URL("http://www.dilbert.com");
    BufferedReader webRead = new BufferedReader(
      new InputStreamReader(url.openStream()));
    String line;
    while ((line = webRead.readLine()) != null) {
      if (line.indexOf("ALT=\"Today's Dilbert Comic\"") != -1) {
        int offset = line.indexOf(
          "<IMG SRC=\"/comics/dilbert/archive/images/dilbert");
        line = line.substring(offset + 10);
        return "http://www.dilbert.com" +
               line.substring(0, line.indexOf('"'));
      }
    }
    return null;
  }
  /**
   * This would allow us to download the URL to a local file.
   * It is so easy that we do not need any explanation :-)
   */
  public static void download(URL url, File file) throws IOException {
    InputStream in = url.openStream();
    FileOutputStream out = new FileOutputStream(file);
    byte[] b = new byte[1024];
    int len;
    while((len = in.read(b)) != -1) {
      out.write(b, 0, len);
    }
    out.close();
  }
  public static void main(String[] args) throws IOException {
    System.out.println("Looking for today's dilbert comic . . .");
    String today = todaysDilbert();

    if (today == null) {
      System.out.println("Could not find today's dilbert comic!");
    } else {
      System.out.println("Found today's dilbert: " + today);
      URL url = new URL(today);

      // we could download the comic to a local file like this:
      // download(url, new File("todaydilbert.gif"));

      // Instead, we are simply going to download it as an ImageIcon
      // and show it in a JFrame.

      System.out.println("Downloading the Image . . .");
      ImageIcon im = new ImageIcon(url);
      System.out.println("Downloaded the Image");

      JFrame f = new JFrame("Today's Dilbert");
      f.getContentPane().add(new JLabel(im));
      f.pack();
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.show();
    }
  }
}
