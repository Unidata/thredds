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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.event.EventListenerList;

/**
 * This wraps a javax.swing.ProgressMonitor, which allows tasks to be canceled.
 * This class adds extra behavior to javax.swing.ProgressMonitor:
 * <ol>
 * <li> Pass in the ProgressMonitorTask you want to monitor.
 * <li> Throws an actionEvent (on the AWT event thread) when the task is done.
 * <li> If an error, pops up an error message.
 * <li> Get status: success/failed/cancel when task is done.
 * </ol>
 *
 * The ProgressMonitorTask is run in a background thread while a
 * javax.swing.ProgressMonitor dialog box shows progress. The task is checked every second
 * to see if its done or canceled.
 *
 * <pre>
 *  Example:

    AddDatasetTask task = new AddDatasetTask(datasets);
    ProgressMonitor pm = new ProgressMonitor(task);
    pm.addActionListener( new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals("success")) {
          doGoodStuff();
        }
      }
    });
    pm.start( this, "Add Datasets", datasets.size());

   class AddDatasetTask extends ProgressMonitorTask {
     private List datasets;

     OpenDatasetTask(List datasets) { this.datasets = datasets; }

     public void run() {
       Iterator iter = datasets.iterator();
       while (iter.hasNext()) {
         AddeDataset ads = (AddeDataset) iter.next();
         this.note = ads.filenameReletive();
         try {
           ads.addImageData( currentSM.serverInfo(), results, false);
         } catch (IOException ioe) {
         error = ioe.getMessage();
         break;
       }
       if (cancel) break;
       this.progress++;
     }
     success = !cancel && !isError();
     done = true;    // do last!
    }
  }
 * </pre>
 *
 *
 * @see ProgressMonitorTask
 * @author jcaron
 * @version 1.0
 */

public class ProgressMonitor {
  private javax.swing.ProgressMonitor pm;
  private ProgressMonitorTask task;
  private javax.swing.Timer timer;
  private Thread taskThread;
  private int millisToPopup = 1000;
  private int millisToDecideToPopup = 1000;
  private int secs = 0;

  // event handling
  private EventListenerList listenerList = new EventListenerList();

  public ProgressMonitor(ProgressMonitorTask task) {
    this( task, 1000, 1000);
  }

  public ProgressMonitor(ProgressMonitorTask task, int millisToPopup, int millisToDecideToPopup) {
    this.task = task;
    this.millisToPopup = millisToPopup;
    this.millisToDecideToPopup = millisToDecideToPopup;
  }

  public ProgressMonitorTask getTask() { return task; }

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

  /**
   * Call this from awt event thread.
   * The task is run in a background thread.
   * @param top: put ProgressMonitor on top of this component (may be null)
   * @param taskName: display name of task
   * @param progressMaxCount: maximum number of Progress indicator
   */
  public synchronized void start(java.awt.Component top, String taskName, int progressMaxCount) {
    // create ProgressMonitor
    pm = new javax.swing.ProgressMonitor(top, taskName, "", 0, progressMaxCount);
    pm.setMillisToDecideToPopup(millisToDecideToPopup);
    pm.setMillisToPopup(millisToPopup);

    // do task in a seperate, non-event, thread
    taskThread = new Thread( task);
    taskThread.start();

    // create timer, whose events happen on the awt event Thread
    ActionListener watcher = new ActionListener() {
      public void actionPerformed(ActionEvent evt) {
        secs++;
        if (pm.isCanceled()) {
          task.cancel();
        } else {
          // indicate progress
          String note = task.getNote();
          pm.setNote(note == null ? secs+" secs" : note);
          int progress = task.getProgress();
          pm.setProgress(progress < 0 ? secs : progress);
        }

        // need to make sure task acknowledges the cancel; so dont shut down
        // until the task is done
        if (task.isDone()) {
          timer.stop();
          pm.close();
          // Toolkit.getDefaultToolkit().beep();

          if (task.isError()) {
           javax.swing.JOptionPane.showMessageDialog(null, task.getErrorMessage());
          }

          if (task.isSuccess())
            fireEvent( new ActionEvent(this, 0, "success"));
          else if (task.isError())
            fireEvent( new ActionEvent(this, 0, "error"));
          else if (task.isCancel())
            fireEvent( new ActionEvent(this, 0, "cancel"));
          else
            fireEvent( new ActionEvent(this, 0, "done"));
        }
      }
    };

    timer = new javax.swing.Timer(1000, watcher); // every second
    timer.start();
  }

}