/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * TextHistoryPane
 * Keeps a user-settable number of lines in a JTextArea.
 * Lines are always appended to bottom, and top lines are scrolled off.
 * A popup menu allows the user to change the number of lines to keep, the font
 * size, etc.
 * @author John Caron
 */
public class TextHistoryPane extends JPanel {
  private static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TextHistoryPane.class);

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
    JScrollPane sp = new JScrollPane(ta); // , JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
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
        log.error("Problem in TextHistoryPane", e);
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
    try (StringWriter sw = new StringWriter(5000)) {
      e.printStackTrace(new PrintWriter(sw));
      setText(sw.toString());
    } catch (IOException e1) {
      e1.printStackTrace();
    }
  }

  /* public void getStream() {
    ByteArrayOutputStream bout = new ByteArrayOutputStream(20000);
  }  */

  private class MyPopupMenu extends ucar.ui.widget.PopupMenu.PopupTriggerListener implements Printable {
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

          newFont = FontUtil.getMonoFont( 10).getFont();
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
            writeToFile(ta.getText(), new File( filename)); // UTF-8 encoding

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

    private void writeToFile(String contents, File file) throws IOException {
      try (FileOutputStream fout = new FileOutputStream(file);
        OutputStreamWriter writer = new OutputStreamWriter(fout, StandardCharsets.UTF_8)) {
        writer.write(contents);
        writer.flush();
      }
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

  private static class IW extends IndependentWindow {
    private IW( String title, TextHistoryPane ta) {
      super(title, BAMutil.getImage( "thredds"), ta);
      setSize(700,700);
      setLocation(100,100);
    }
  }

}
