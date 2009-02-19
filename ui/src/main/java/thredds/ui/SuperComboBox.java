// $Id: SuperComboBox.java 50 2006-07-12 16:30:06Z caron $
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

import ucar.nc2.util.NamedObject;
import thredds.viewer.ui.table.*;

import thredds.viewer.ui.event.*;
import ucar.util.prefs.ui.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.util.*;

/**
 * SuperComboBox is a complete rewrite of JComboBox;
 * it does not extend JComboBox (!)
 *
 * Items added may implement NamedObject, in which case getName() is used as the row name,
 * and getDescription() is used as the tooltip. Otherwise, o.toString() is used as the row name,
 * and there is no tooltip. The row names must be unique.
 *
 * An ActionValueEvent is thrown when the selection is changed through manipulation of the widget.
 * The selection can be obtained from actionEvent.getValue(), which returns the the selection row name.
 *
 * The caller can change the selection by calling setSelectionByName(). In this case,
 * no event is thrown.
 *
 * @author John Caron
 * @version $Id: SuperComboBox.java 50 2006-07-12 16:30:06Z caron $
 */
public class SuperComboBox extends JPanel {

  // data
  private ArrayList list = new ArrayList(); // list of TableRow objects

  // UI stuff
  private String name;
  private MyTextField text; // this is what shows all the time
  private JButton next, prev, down; // arrow button
  private JWindow pulldown; // pulldown menu
  //private Popup popup = null;
  private JTableSorted table; // inside the pulldown
  private IndependentWindow loopControl = null;  // loop control

  private ActionSourceListener actionSource;

  private boolean isNamedObject = false;
  private boolean eventOK = true;               // disallow events to prevent infinite loop
  private boolean sendExternalEvent = true;     // disallow events to prevent infinite loop
  private boolean immediateMode = false;        // used for looping

  private boolean debug = false, debugEvent = false;

  private int height = 222;
  private int width = 100;

   /** default : one column, with an iterator of NamedObjects
    * @param parent: parent container
    * @param name: column name
    * @param sortOK : true allow sorting, column adding and removing
    * @param iter: Iterator of objects inside the combobox.
    */
  public SuperComboBox(RootPaneContainer parent, String name, boolean sortOK, Iterator iter) {
    this.name = name;

      // create JLabel
    text = new MyTextField(" ");
    text.setToolTipText(name);

      // create arrow buttons
    JPanel seqPanel = new JPanel(new GridLayout(1,2));
    prev = makeButton(SpinIcon.TypeLeft);
    prev.setToolTipText("previous");
    seqPanel.add( prev);

    next = makeButton(SpinIcon.TypeRight);
    next.setToolTipText("next");
    seqPanel.add( next);

    //JPanel bPanel = new JPanel(new GridLayout(2,1));
    JPanel bPanel = new JPanel(new BorderLayout());
    down = makeButton(SpinIcon.TypeDown);
    down.setToolTipText("show menu");
    bPanel.add( seqPanel, BorderLayout.NORTH);
    bPanel.add( down, BorderLayout.SOUTH);

      // the jtable
    String [] colNames = new String [1];
    colNames[0] = name;

    table = new JTableSorted(colNames, list);
    table.setSortOK( sortOK);
    if (iter != null)
      setCollection( iter);

      // the pulldown menu list
    JFrame parentComponent = (parent != null) && (parent instanceof JFrame) ? (JFrame) parent : null;
    pulldown = new JWindow(parentComponent);
    pulldown.getContentPane().add( table);
    pulldown.pack();

      // put it together
    setBorder(new EtchedBorder(EtchedBorder.RAISED));
    setLayout(new BorderLayout());
    //add(seqPanel, BorderLayout.WEST);
    add(text, BorderLayout.CENTER);
    add(bPanel, BorderLayout.EAST);

    // size
    //text.setColumns(numTextCols);

    // add the listeners
    text.addMouseListener( new MyMouseAdapter() {
      public void click(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e))
          showPulldownMenu();
      }
    });

    // popup menu
    LoopControl popup = new LoopControl();
    text.addMouseListener( popup);
    loopControl = popup.getLoopControl();

    prev.addMouseListener( new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        table.incrSelected(false);
      }
    });
    next.addMouseListener( new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        table.incrSelected(true);
      }
    });
    down.addMouseListener( new MouseAdapter() {
      public void mouseClicked(MouseEvent e) {
        showPulldownMenu();
      }
    });

    table.addListSelectionListener( new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        if (debugEvent) System.out.println(" JTable event ");
        if (eventOK && !e.getValueIsAdjusting()) {
          setSelection();
          hidePulldownMenu();
        }
      }
    });
    table.getTable().addMouseListener( new MyMouseAdapter() {
      public void click(MouseEvent e) {
        //System.out.println("table.table click");
        hidePulldownMenu();
      }
    });

      // event management
    actionSource = new ActionSourceListener(name) {
      public void actionPerformed( ActionValueEvent e) {
        if (debugEvent) System.out.println(" actionSource event "+e);
        setSelectedByName( e.getValue().toString());
      }
    };
  }

    /** add ActionValueListener listener */
  public void addActionValueListener( ActionValueListener l) { actionSource.addActionValueListener(l); }
    /** remove ActionValueListener listener */
  public void removeActionValueListener( ActionValueListener l) { actionSource.removeActionValueListener(l); }

    /** better way to do event management */
  public ActionSourceListener getActionSourceListener() { return actionSource; }

       /** get the LoopControl Window associated with this list */
  public IndependentWindow getLoopControl() { return loopControl; }
       /** get the name associated with this list */
  public String getName() { return name; }

    /**
     * Set the list of things to be selected.
     * Iterator may return objects of type NamedObjects,
     * otherwise will use object.toString()
     */
  public void setCollection( Iterator iter) {
    eventOK = false;
    list = new ArrayList();

    if (iter != null) {
      FontMetrics fm = text.getFontMetrics( text.getFont());

      int width = 0;
      while (iter.hasNext()) {
        Object o = iter.next();
        TableRow row;
        if (o instanceof NamedObject)
          row = new NamedObjectRow((NamedObject) o);
        else
          row = new SimpleRow(o);

        String s = row.getValueAt(0).toString().trim();
        int slen = fm.stringWidth(s);
        width = Math.max(width, slen);

        list.add(row);
      }
      // resize
      Dimension d = text.getPreferredSize();
      d.width = width+10;
      text.setPreferredSize(d);
      text.revalidate();
    }

    if (debugEvent) System.out.println(" New collection set = ");
    table.setList(list);
    if (list.size() == 0)
      setLabel("none");
    else
      setSelectedByIndex(0);
    eventOK = true;
  }

  /** Set the displayed text. Typically its only used when the list is empty.
   */
  public void setLabel(String s) {
    text.setText( s);
  }

  /** Get the currently selected object. May be null.
   */
  public Object getSelectedObject() {
    TableRow selected = getSelectedRow();
    return (selected == null) ? null : selected.getUserObject();
  }

  /** Get the index of the currently selected object in the list.
   * If sortOK, then this may not be the original index.
   * @return index of selected object, or -1 if none selected.
   */
  public int getSelectedIndex() {
    return table.getSelectedRowIndex();
  }

  /** Set the currently selected object using its choice name.
   * /NO If not found, will be set to first element. ?NO
   * Note that no event is sent due to this call.
   * @param choiceName: name of object to match.
   * @return index of selection, or -1 if not found;
   */
  public int setSelectedByName( String choiceName) {
    for (int i=0; i<list.size(); i++) {
      TableRow row = (TableRow) list.get(i);
      String value = row.getValueAt(0).toString();
      if (choiceName.equals(value)) {
        sendExternalEvent = false;
        setSelectedByIndex(i);
        sendExternalEvent = true;
        return i;
      }
    }
    // setSelectedByIndex(-1);  // force it to 0 or none
    return -1;
  }

  /** Set the currently selected object using its index.
   * If sortOK, then this may not be the original index.
   * @param index of selected object.
   */
   public void setSelectedByIndex( int index) {
    if ((index >= 0) && (index < list.size())) {
      table.setSelected(index);
      setSelection();
    } else if ( list.size() > 0) {
      table.setSelected(0);
      setSelection();
    } else
      setLabel("none");
  }

  ////////////////////////////////////////////////////

  private TableRow getSelectedRow() {
    return table.getSelected();
  }

  private void setSelection() {
    TableRow selected = getSelectedRow();
    if (debug) System.out.println(" setSelection = "+ selected);
    if (selected != null) {
      Object selectedObject = selected.getValueAt(0);
      String selectedName = selectedObject.toString();
      text.setText( selectedName.trim());
      if (selected instanceof NamedObjectRow)
        text.setToolTipText( ((NamedObject)selected.getUserObject()).getDescription());
      repaint();

      if (sendExternalEvent) {
        if (debugEvent) System.out.println("--->SuperCombo send event "+selectedObject);
        if (immediateMode)
          actionSource.fireActionValueEvent( "redrawImmediate", selectedObject);
        else
          actionSource.fireActionValueEvent( ActionSourceListener.SELECTED, selectedObject);
      }
    }
  }

  private void showPulldownMenu() {
    if (pulldown.isShowing())
      pulldown.hide();
    else {
      Dimension d = new Dimension(getWidth(), height);
      pulldown.setSize(d);
      Point p = text.getLocationOnScreen();
      p.y += text.getHeight();
      pulldown.setLocation(p);
      pulldown.show();
    }
  }

  private void hidePulldownMenu() {
    if (pulldown.isShowing()) {
      pulldown.hide();
      //System.out.println("hidePulldownMenu");
    }
  }


  /* private void showPulldownMenu() {
    hidePulldownMenu();
    Point p = text.getLocationOnScreen();
    p.y += text.getHeight();
    PopupFactory factory = PopupFactory.getSharedInstance();
    popup = factory.getPopup(this, table, p.x, p.y);
    popup.show();
  }

  private void hidePulldownMenu() {
    if (popup != null) {
      popup.hide();
      popup = null;
    }
  } */

  private JButton makeButton(SpinIcon.Type type) {
      SpinIcon icon = new SpinIcon(type);
      JButton butt  = new JButton( icon);
      Insets i = new Insets(0,0,0,0);
      butt.setMargin( i);
      butt.setBorderPainted(false);
      butt.setFocusPainted(false);
      butt.setPreferredSize(new Dimension(icon.getIconWidth()+2, icon.getIconHeight()+2));
      return butt;
  }


  private class SimpleRow extends TableRowAbstract {
    Object o;
    SimpleRow( Object o){ this.o = o; }
    public Object getValueAt( int col) { return o; }
    public Object getUserObject() { return o; }
  }

  private class NamedObjectRow extends TableRowAbstract implements NamedObject {
    NamedObject o;
    NamedObjectRow( NamedObject o){ this.o = o; }
    public Object getValueAt( int col) { return this; }
    public Object getUserObject() { return o; }

    public String getName() { return o.getName(); }
    public String getDescription() { return o.getDescription(); }
    public String toString() { return o.getName(); }
  }

  /*private class MyCBLayoutManager extends MetalComboBoxUI.MetalComboBoxLayoutManager {

      public void layoutContainer(Container parent) {
        JComboBox cb = (JComboBox)parent;
        int width = cb.getWidth();
        int height = cb.getHeight();
        Insets insets = cb.getInsets();
        int buttonSize = height - (insets.top + insets.bottom);

        up.setBounds( width - (insets.right + buttonSize), insets.top, buttonSize, buttonSize/2);
        down.setBounds( width - (insets.right + buttonSize), 2+insets.top+buttonSize/2,
                buttonSize, buttonSize/2);

        setRectangleForCurrentValue();
      }
  }  */

  private class MyTextField extends JLabel {
    private int arrow_size = 4;
    private boolean wasDragged = false;
    private Rectangle b;
    private int nitems = 0;
    private int currentItem = 0;

    MyTextField( String name) {
      super(name);
      setOpaque(true);
      setBackground(Color.white);
      setForeground(Color.black);

      addMouseListener( new MyMouseListener());
      addMouseMotionListener( new MyMouseMotionListener());
    }

    public void paintComponent(Graphics g) {
      super.paintComponent(g);

      g.setColor( Color.black);
//      g.setColor( component.isEnabled() ? MetalLookAndFeel.getControlInfo() :
 //                                           MetalLookAndFeel.getControlShadow() );
      b = getBounds();
      nitems = list.size();  // number of items

      int posx = getItemPos();
      int line = b.height-1;
      for (int w2=arrow_size; w2>=0; w2--) {
        g.drawLine( posx-w2, line, posx+w2, line );
        line--;
      }
    }

        // return slider indicator position for currently selected item
    protected int getItemPos() {
      if (nitems < 1)
        return -arrow_size;   // dont show indicator
      else if (nitems == 1)
        return b.width/2;   // indicator in center

      int item = table.getSelectedRowIndex();  // selected item
      int eff_width = b.width - 2*arrow_size;   // effective width
      int pixel =  (item * eff_width)/(nitems-1);  // divided into n-1 intervals
      return pixel+arrow_size;
    }

    // return item selected by this pixel position
    protected int getItem( int pixel) {
      if (nitems < 2)
        return 0;

      int eff_width = b.width - 2*arrow_size;   // effective width
      double fitem = ((double) (pixel-arrow_size)*(nitems-1)) / eff_width;
      int item =  (int)(fitem+.5);
      item = Math.max( Math.min(item, nitems-1), 0);
      return item;
    }

    private class MyMouseListener extends MouseAdapter {
      public void mousePressed(MouseEvent anEvent) {
        sendExternalEvent = false;
        wasDragged = false;
        currentItem = table.getSelectedRowIndex();
      }

      public void mouseReleased(MouseEvent anEvent) {
        sendExternalEvent = true;
        if (wasDragged) {
          int item = getItem( anEvent.getX());
          if (item != currentItem) {
            setSelection();
            //if (debug) System.out.println("release select "+item);
          }
        }
        wasDragged = false;
      }
    }

    private class MyMouseMotionListener extends MouseMotionAdapter {

      public void mouseDragged(MouseEvent anEvent) {
        int item = getItem( anEvent.getX());
        table.setSelected(item);
        MyTextField.this.repaint();
        wasDragged = true;
      }
    }

  } // inner class MyTextField

  private class LoopControl extends PopupMenu.PopupTriggerListener {
    private IndependentWindow iw;

    private JPanel loopPanel;
    private JButton moreOrLess;
    private JSpinner stepSpinner;
    private AbstractButton loopButt, helpButt;
    private PrefPanel ifPanel;
    private Field.Int stepIF;
    private Field.Text startIF;

    private AbstractAction play, fastforward, stop, rewind, back, next, prev;
    private AbstractAction loopAct, helpAct;
    private boolean stopped, forward, first = true, continuous = true, less = true;
    private int step = 1, start = -1;
    private long startTime;

    LoopControl () {
      loopPanel = new JPanel();

      // create VCR buttons
      play = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          start(true);
        }
      };
      BAMutil.setActionProperties( play, "VCRPlay", "play", false, 'S', KeyEvent.VK_NUMPAD6);
      AbstractButton b = BAMutil.addActionToContainer( loopPanel, play);

      fastforward = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          setSelectedByIndex(list.size()-1);
        }
      };
      BAMutil.setActionProperties( fastforward, "VCRFastForward", "go to end", false, 'F', KeyEvent.VK_NUMPAD1);
      b = BAMutil.addActionToContainer( loopPanel, fastforward);

      next = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          int current = incr(true, false);
          if (current >= 0) {
            setSelectedByIndex(current);
            text.paintImmediately(text.getBounds());
          }
        }
      };
      BAMutil.setActionProperties( next, "VCRNextFrame", "Next frame", false, 'N', KeyEvent.VK_PAGE_DOWN);
      b = BAMutil.addActionToContainer( loopPanel, next);

      stop = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          stopped = true;
        }
      };
      BAMutil.setActionProperties( stop, "VCRStop", "stop", false, 'S', KeyEvent.VK_ESCAPE);
      b = BAMutil.addActionToContainer( loopPanel, stop);

      prev = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          int current = incr(false, false);
          if (current >= 0) {
            setSelectedByIndex(current);
            text.paintImmediately(text.getBounds());
          }
        }
      };
      BAMutil.setActionProperties( prev, "VCRPrevFrame", "Previous frame", false, 'P', KeyEvent.VK_PAGE_UP);
      b = BAMutil.addActionToContainer( loopPanel, prev);

      rewind = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          setSelectedByIndex(0);
        }
      };
      BAMutil.setActionProperties( rewind, "VCRRewind", "rewind", false, 'R', KeyEvent.VK_NUMPAD7);
      b = BAMutil.addActionToContainer( loopPanel, rewind);

      back = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          start(false);
        }
      };
      BAMutil.setActionProperties( back, "VCRBack", "play backwards", false, 'B', KeyEvent.VK_NUMPAD4);
      b = BAMutil.addActionToContainer( loopPanel, back);

      moreOrLess = new JButton( new SpinIcon( SpinIcon.TypeRight));
      moreOrLess.setBorder(BorderFactory.createEmptyBorder());
      moreOrLess.setMargin(new Insets(0,0,0,0));
      moreOrLess.addActionListener( new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          if (less) makeMore();
          else makeLess();
        }
      });
      loopPanel.add(moreOrLess);

      iw = new IndependentWindow(name+" loop", null, loopPanel);
      iw.setResizable( false);

        // these arent added to the panel right away
      loopAct = new AbstractAction() {
        public void actionPerformed(ActionEvent e) {
          Boolean state = (Boolean) getValue(BAMutil.STATE);
          continuous = state.booleanValue();
        }
      };
      BAMutil.setActionProperties( loopAct, "MovieLoop", "continuous loop", true, 'L', 0);

      helpAct = new AbstractAction() {
        public void actionPerformed(ActionEvent evt) {
          // faqo.apputil.Help.getDefaultHelp().gotoTarget("movieLoop");
        }
      };
      BAMutil.setActionProperties( helpAct, "Help", "online Help...", false, 'H', KeyEvent.VK_H);

      stepSpinner = new JSpinner();
      stepSpinner.setToolTipText("step");

      ifPanel = new PrefPanel("loopControl", null);
      stepIF = ifPanel.addIntField("step", "step", 1);
      startIF = ifPanel.addTextField("start", "start", "    ");
      ifPanel.finish(false);
    }

    private void makeMore() {
      loopButt = BAMutil.addActionToContainer( loopPanel, loopAct);
      loopAct.putValue(BAMutil.STATE, new Boolean(continuous));
      helpButt = BAMutil.addActionToContainer( loopPanel, helpAct);
      loopPanel.add( ifPanel);

      moreOrLess.setIcon( new SpinIcon( SpinIcon.TypeLeft));
      moreOrLess.setToolTipText("less");

      loopPanel.revalidate();
      iw.pack();
      less = false;
    }

    private void makeLess() {
      loopPanel.remove(loopButt);
      loopPanel.remove(helpButt);
      loopPanel.remove(ifPanel);

      moreOrLess.setIcon( new SpinIcon( SpinIcon.TypeRight));
      moreOrLess.setToolTipText("more");

      loopPanel.revalidate();
      iw.pack();
      less = true;
    }

    IndependentWindow getLoopControl() { return iw; }
    public void showPopup(MouseEvent e) {
      if (first) {
        Point pt = new Point( 0, 0);
        SwingUtilities.convertPointToScreen( pt, SuperComboBox.this);
        iw.setLocation( pt);
        first = false;
      }
      iw.show();
    }

    private void start(boolean forward) {
      this.forward = forward;
      startTime = System.currentTimeMillis();
      eventOK = false;
      immediateMode = true;
      stopped = false;

      ifPanel.accept();
      step = Math.min(stepIF.getInt(), 1);

      start = -1;
      String startName = startIF.getText();
      if ((startName != null) && (startName.length() > 0)) {
        start = setSelectedByName(startName);
      }
      if (debug) System.out.println(" start = "+start+" step = "+step);

      SwingUtilities.invokeLater( new RunLoop());  // execute on eventTread
    }


    private int incr( boolean forward, boolean continuous) {
      int current = getSelectedIndex();
      if (forward) current += step;
      else current -= step;
      if (!continuous && ((current < 0) || (current >= list.size()))) {
        return -1;
      }

      if (current >= list.size())
        current = (start >= 0) ? start : 0;
      if (current < 0)
        current = (start >= 0) ? start : list.size() - 1;
      return current;
    }

    class RunLoop implements Runnable {
      public void run() {
        loopPanel.repaint();
        if (stopped) { stop(); return; }

        int current = incr(forward, continuous);
        if (current < 0) {
          stop();
          return;
        }

          // goto next
        setSelectedByIndex(current);
        text.paintImmediately(text.getBounds());
          // set another event
        SwingUtilities.invokeLater( new RunLoop());
      }

      private void stop() {
        immediateMode = false;
        eventOK = true;
        /* if (Debug.isSet("timing/loop")) {
          long tookTime = System.currentTimeMillis() - startTime;
          System.out.println("timing/loop: "+list.size()+" " + tookTime*.001 + " seconds");
        } */
      }

    }

  }  // inner class LoopControl


  public static void main(String args[]) {

    JFrame frame = new JFrame("Test Combo Box");
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {System.exit(0);}
    });

    ArrayList a = new ArrayList(30);
    for (int i=0; i<30; i++)
      a.add("hifdsjflkjslfk "+i);
    SuperComboBox scb = new SuperComboBox(frame, "myTestdjdslkfjslkj", true, a.iterator());
    JComboBox cb = new JComboBox();
    for (int i=0; i<a.size(); i++)
      cb.addItem(a.get(i));

    JPanel main = new JPanel(new FlowLayout());
    frame.getContentPane().add(main);
    main.setPreferredSize(new Dimension(200, 200));
    main.add( scb);
    main.add( cb);

    frame.pack();
    frame.setLocation(300, 300);
    frame.setVisible(true);
  }

}


/* Change History:
   $Log: SuperComboBox.java,v $
   Revision 1.8  2005/10/11 19:43:00  caron
   release 3.3, uses nj22.11
   support range bytes, use FileCache

   Revision 1.7  2004/09/30 00:33:38  caron
   *** empty log message ***

   Revision 1.6  2004/09/24 03:26:34  caron
   merge nj22

   Revision 1.5  2004/03/05 23:43:24  caron
   1.3.1 release

   Revision 1.4  2004/02/20 05:02:53  caron
   release 1.3

   Revision 1.2  2003/01/18 19:53:43  john
   url authenticator, better logging

   Revision 1.1.1.1  2002/11/23 17:49:48  caron
   thredds reorg

   Revision 1.2  2002/04/29 22:26:58  caron
   minor

*/
