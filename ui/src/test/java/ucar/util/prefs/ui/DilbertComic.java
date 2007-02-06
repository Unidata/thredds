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
