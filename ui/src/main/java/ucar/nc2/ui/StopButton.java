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

package ucar.nc2.ui;

import thredds.ui.*;
import javax.swing.*;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.EventListenerList;

import ucar.nc2.util.CancelTask;

/**
 * A UI Component for running background tasks and letting user cancel them.
 * Also can be used as a CancelTask implementation.
 * @see ProgressMonitorTask
 *
 * @author caron
 */

public class StopButton extends JButton implements CancelTask {
  static private ImageIcon[] icon = new ImageIcon[2];
  static {
    icon[0] = BAMutil.getIcon("Stop24", true);
    icon[1] = BAMutil.getIcon("Stop16", true);
  }
  static private boolean debug = false;

  private ProgressMonitorTask task;
  private javax.swing.Timer myTimer;
  private boolean busy, isCancelled;
  private int count;

  // event handling
  private EventListenerList listenerList = new EventListenerList();

  // bean for JForm Designer - let it set sizes
  public StopButton() {
   setIcon(icon[0]);
    //setMaximumSize(new java.awt.Dimension(28,28));       // kludge; consistent with BAMutil
    //setPreferredSize(new java.awt.Dimension(28,28));
    setFocusPainted(false);

    super.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (debug) System.out.println(" StopButton.EVENT");
        isCancelled = true;
      }
    });
  }

  public StopButton(String tooltip) {
    setIcon(icon[0]);
    setMaximumSize(new java.awt.Dimension(28,28));       // kludge; consistent with BAMutil
    setPreferredSize(new java.awt.Dimension(28,28));
    setToolTipText(tooltip);
    setFocusPainted(false);

    super.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (debug) System.out.println(" StopButton.EVENT");
        isCancelled = true;
      }
    });
  }

  /** Add listener: action event sent when task is done. event.getActionCommand() =
   *  <ul><li> "success"
   *  <li> "error"
   *  <li> "cancel"
   *  <li> "done" if done, but success/error/cancel not set
   *  </ul>
   */
  public void addActionListener(ActionListener l) {
    listenerList.add(java.awt.event.ActionListener.class, l);
  }
  /** Remove listener */
  public void removeActionListener(ActionListener l) {
    listenerList.remove(java.awt.event.ActionListener.class, l);
  }
  private void fireEvent(java.awt.event.ActionEvent event) {
    // Guaranteed to return a non-null array
    Object[] listeners = listenerList.getListenerList();
    // Process the listeners last to first
    for (int i = listeners.length-2; i>=0; i-=2) {
      ((java.awt.event.ActionListener)listeners[i+1]).actionPerformed(event);
    }
  }

  public boolean isCancel() {
    return isCancelled;
  }

  public void setCancel(boolean isCancelled) {
    this.isCancelled = isCancelled;
  }

  public void setError(String msg) {
    System.out.println("Got Error= "+msg);
  }

  /**
   * The given task is run in a background thread.
   * Progress is indicated once a second.
   * You cannot call this method again till the task is completed.
   * @param pmt heres where the work is done.
   * @return true task was started, false if still busy with previous task.
   */
  public boolean startProgressMonitorTask( ProgressMonitorTask pmt) {
    if (busy) return false;
    busy = true;

    this.task = pmt;
    isCancelled = false;
    count = 0;
    setIcon(icon[0]);

    // create timer, whose events happen on the awt event Thread
    ActionListener watcher = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        //System.out.println("timer event"+evt);

        if (isCancelled && !task.isCancel()) {
          task.cancel();
          if (debug) System.out.println(" task.cancel");
          return; // give it a chance to finish up
        } else {
          // indicate progress
          count++;
          setIcon( icon[count % 2]);
          if (debug) System.out.println(" stop count="+count);
        }

        // need to make sure task acknowledges the cancel; so dont shut down
        // until the task is done
        if (task.isDone()) {
          if (myTimer != null)
            myTimer.stop();
          myTimer = null;

          if (task.isError())
            javax.swing.JOptionPane.showMessageDialog(null, task.getErrorMessage());

          if (task.isSuccess())
            fireEvent( new ActionEvent(this, 0, "success"));
          else if (task.isError())
            fireEvent( new ActionEvent(this, 0, "error"));
          else if (task.isCancel())
            fireEvent( new ActionEvent(this, 0, "cancel"));
          else
            fireEvent( new ActionEvent(this, 0, "done"));

          busy = false;
        }
      }
    };
    myTimer = new javax.swing.Timer(1000, watcher); // every second
    myTimer.start();

    // do task in a seperate, non-event, thread
    Thread taskThread = new Thread(task);
    taskThread.start();

    return true;
  }

  public void clear() {
    if (myTimer != null)
      myTimer.stop();
    myTimer = null;
    busy = false;
  }

}