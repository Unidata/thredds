// $Id: TextHistoryPane.java 50 2006-07-12 16:30:06Z caron $
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
package thredds.ui;

import ucar.nc2.util.IO;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.*;
import java.util.StringTokenizer;
import javax.swing.*;

/**
 * TextHistoryPane
 * Keeps a user-settable number of lines in a JTextArea.
 * Lines are always appended to bottom, and top lines are scrolled off.
 * A popup menu allows the user to change the number of lines to keep, the font
 * size, etc.
 * @author John Caron
 * @version $Id: TextHistoryPane.java 50 2006-07-12 16:30:06Z caron $
 */
public class TextHistoryPane extends JPanel {
  protected JTextArea ta;
  private FontUtil.StandardFont fontu;
  private int nlines, removeIncr, count = 0;
  private int ptSize;

  protected FileManager fileChooser;

  /** default constructor. */
  public TextHistoryPane() {
    this( false, 5000, 50, true, true, 14);
  }

  /** constructor allows editing. */
  public TextHistoryPane(boolean editable) {
    this( editable, 5000, 50, true, true, 14);
  }

  /** constructor
     @param nlines  number of lines of text to keep history of
     @param removeIncr  remove this number of lines at a time
     @param popupOK enable popup menu
     @param ptSize font point size
   */
  public TextHistoryPane(boolean editable, int nlines, int removeIncr, boolean popupOK,  boolean lineWrap, int ptSize) {
    super(new BorderLayout());
    this.nlines = nlines-1;
    this.removeIncr = Math.min(nlines-1, removeIncr-1); // cant be bigger than nlines

    ta = new JTextArea();
    ta.setLineWrap(lineWrap);
    ta.setEditable(editable);
    fontu = FontUtil.getMonoFont( ptSize);
    ta.setFont(fontu.getFont());

    if (popupOK)
      ta.addMouseListener( new MyPopupMenu());

    //JScrollPane sp = new JScrollPane(ta, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    JScrollPane sp = new JScrollPane(ta);
    add(sp, BorderLayout.CENTER);

    javax.swing.filechooser.FileFilter[] filters = new javax.swing.filechooser.FileFilter[2];
    filters[0] = new FileManager.HDF5ExtFilter();
    filters[1] = new FileManager.NetcdfExtFilter();
    fileChooser = new FileManager(null, null, null, null);
  }

  /** Append this line to the bottom of the JTextArea.
   * A newline is added and JTextArea is scrolled to bottom;
   * remove lines at top if needed.
   * @param line append this line. Ok to have multiple lines (ie embedded newlines)
   *   but not too many.
   */
  public void appendLine( String line) {

    if (count >= nlines) {
      try {
        int remove = Math.max(removeIncr, count - nlines); // nlines may have changed
        int offset = ta.getLineEndOffset( remove);
        ta.replaceRange( "", 0, offset);
      } catch (Exception e) {
        System.out.println("BUG in TextHistoryPane");  // shouldnt happen
      }
      count = nlines - removeIncr;
    }
    ta.append(line);
    ta.append("\n");
    count++;

    // scroll to end
    ta.setCaretPosition(ta.getText().length());
  }

  public void clear() { ta.setText(null); }
  public String getText() { return ta.getText(); }
  public void gotoTop() { ta.setCaretPosition(0); }
  public void setText(String text) { ta.setText(text); }

  public void setTextFromStackTrace(Throwable e) {
    ByteArrayOutputStream bos = new ByteArrayOutputStream(10000);
    e.printStackTrace(new PrintStream(bos));
    setText(bos.toString());
  }

  /* public void getStream() {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(20000);
  }  */

  private class MyPopupMenu extends PopupMenu.PopupTriggerListener implements Printable {
    private JPopupMenu popup = new JPopupMenu("Options");
    private JTextField nlinesFld = new JTextField();
    private JTextField ptSizeFld = new JTextField();
    private AbstractAction incrFontAction, decrFontAction;

    private StringTokenizer token;
    private Font newFont;
    private int incrY;

    MyPopupMenu () {
      super();

        // the popup menu
      JPanel nlPan = new JPanel();
      nlPan.add(new JLabel("Number of lines to keep:"));
      nlPan.add(nlinesFld);
      popup.add(nlPan);

      incrFontAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          fontu.incrFontSize();
          ta.setFont(fontu.getFont());
       }
      };
      BAMutil.setActionProperties( incrFontAction, "FontIncr", "Increase Font Size", false, '+', -1);
      BAMutil.addActionToPopupMenu( popup, incrFontAction);

      decrFontAction = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          fontu.decrFontSize();
          ta.setFont(fontu.getFont());
       }
      };
      BAMutil.setActionProperties( decrFontAction, "FontDecr", "Decrease Font Size", false, '-', -1);
      BAMutil.addActionToPopupMenu( popup, decrFontAction);

      JMenuItem printButt= new JMenuItem("Print");
      popup.add(printButt);

      JMenuItem writeButt= new JMenuItem("Write to File");
      popup.add(writeButt);

      JMenuItem dissButt= new JMenuItem("Dismiss");
      popup.add(dissButt);

      JMenuItem clearButt= new JMenuItem("Clear");
      popup.add(clearButt);

        // listen to changes to the JTextField
      nlinesFld.addActionListener( new AbstractAction() {
        public void actionPerformed( ActionEvent e) {
          int numLines = Integer.parseInt(nlinesFld.getText());
          // System.out.println( "numLines = "+numLines);
          TextHistoryPane.this.nlines = Math.max(numLines-1, removeIncr);
          popup.setVisible(false);
        }
      });

        // print
      printButt.addActionListener( new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          PrinterJob printJob = PrinterJob.getPrinterJob();
          PageFormat pf = printJob.defaultPage();

          newFont = thredds.ui.FontUtil.getMonoFont( 10).getFont();
          FontMetrics fontMetrics = Toolkit.getDefaultToolkit().getFontMetrics( newFont);
          incrY = fontMetrics.getAscent() + fontMetrics.getDescent();

          printJob.setPrintable(MyPopupMenu.this, pf);
          if (printJob.printDialog()) {
            try {
              //if (Debug.isSet("print.job")) System.out.println("call printJob.print");
              printJob.print();
              //if (Debug.isSet("print.job")) System.out.println(" printJob done");
            } catch (Exception PrintException) {
              PrintException.printStackTrace();
            } finally {
              popup.setVisible(false);
            }
          }
        }
      });

      writeButt.addActionListener( new AbstractAction() {
        public void actionPerformed( ActionEvent e) {
          String filename = fileChooser.chooseFilename();
          if (filename == null) return;

          try {
            IO.writeToFile(ta.getText(), new File( filename)); // UTF-8 encoding

            JOptionPane.showMessageDialog(null, "Text written to"+filename);
          } catch (IOException ioe) {
            //System.out.println(" write TextArea to file = "+filename+" "+ioe);
            JOptionPane.showConfirmDialog(null, "Error writting to" + filename+" "+ioe.getMessage());
          }
          popup.setVisible(false);
        }
      });

      dissButt.addActionListener( new AbstractAction() {
        public void actionPerformed( ActionEvent e) {
          popup.setVisible(false);
        }
      });

      clearButt.addActionListener( new AbstractAction() {
        public void actionPerformed( ActionEvent e) {
          clear();
          popup.setVisible(false);
        }
      });

    }

    public void showPopup(MouseEvent e) {
      nlinesFld.setText(""+(TextHistoryPane.this.nlines+1));
      ptSizeFld.setText(""+ptSize);
      popup.show(ta, e.getX(), e.getY());
    }

    public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
      if (pi == 0)
        token = new StringTokenizer(ta.getText(), "\r\n");

      if (!token.hasMoreTokens())
        return Printable.NO_SUCH_PAGE;

      Graphics2D g2d = (Graphics2D) g;
      g2d.setPaint(Color.black);
      g2d.setFont(newFont);

      double xbeg = pf.getImageableX();
      double ywidth = pf.getImageableHeight() + pf.getImageableY();
      double y = pf.getImageableY() + incrY;
      while (token.hasMoreTokens() && (y < ywidth)) {
        String toke = token.nextToken();
        g2d.drawString( toke, (int) xbeg, (int) y);
        y += incrY;
      }
      return Printable.PAGE_EXISTS;
    }
  }

  public IndependentWindow makeIndependentWindow(String title) {
    return new IW( title, this);
  }

  private class IW extends IndependentWindow {
    private IW( String title, TextHistoryPane ta) {
      super(title, BAMutil.getImage( "thredds"), ta);
      setSize(700,700);
      setLocation(100,100);
    }
  }

}

/* Change History:
   $Log: TextHistoryPane.java,v $
   Revision 1.9  2005/07/27 23:29:14  caron
   minor

   Revision 1.8  2004/12/14 15:41:01  caron
   *** empty log message ***

   Revision 1.7  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.6  2004/09/24 03:26:35  caron
   merge nj22

   Revision 1.5  2004/05/11 23:30:36  caron
   release 2.0a

   Revision 1.4  2004/03/05 23:43:25  caron
   1.3.1 release

   Revision 1.3  2004/02/20 05:02:53  caron
   release 1.3

   Revision 1.2  2003/05/29 23:02:46  john
   add right-click menu

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.2  2002/04/29 22:26:58  caron
   minor

   Revision 1.1.1.1  2002/02/26 17:24:51  caron
   import sources
*/

