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

/**
 * Subclass this for use in a ProgressMonitor. Responsibilities of your sublclass:
 * <ul> <li> You must provide the run method, which actually does the work that the ProgressMonitor
 *   is showing progress for.
 * <li> You must set the done flag for the ProgressMonitor to complete.  To avoid race conditions,
 *   do eveything else before you set the done flag.
 * <li> You should override setProgress() and return a real count that corresponds to progressMaxCount in
 *          ProgressMonitor.start().
 * <li> If possible, you should stop working if isCancel(). Usually this means checking the
 *   cancel flag in an iteration.
 * <li> You should set the success flag if everything worked ok.
 * <li> You should set an error message if there is an error. Normally you would also
 *   set done=true, and stop working. The ProgressMonitor will detect this and exit.
 * </ul>
 * The ProgressMonitor will:
 * <ul><li> Call cancel() if the user pressed cancel.
 * <li> Set progress value by counting elapsed seconds. You may overrride this by overridding getProgress()
 * and/or getNote().
 * </ul>
 *
 * @see ProgressMonitor
 */
public abstract class ProgressMonitorTask implements Runnable {
  protected boolean done = false;
  protected boolean success = false;
  protected boolean cancel = false;
  protected String error = null;
  protected String note = null;

  /** Here is where the work gets done. */
  abstract public void run();

  /** Application calls to see if task is success. */
  public boolean isSuccess() { return success; }
  /** Application calls to see if task is cancelled. */
  public boolean isCancel() { return cancel; }
  /** Applications call this to find out if there was an error. */
  public boolean isError() { return error != null; }
  /** Applications call this to get an error message. */
  public String getErrorMessage( ) { return error; }

  /** ProgressMonitor calls to see if task is done. */
  public boolean isDone() { return done; }
  /** ProgressMonitor will call this when the user cancels. */
  public void cancel() {
    cancel = true;
  }
  /** ProgressMonitor displays this note in the ProgressMonitor. If null, ProgressMonitor will show
   * elasped seconds. */
  public String getNote() { return note; }
  /** ProgressMonitor displays this progress value in the ProgressMonitor. If <0, ProgressMonitor will show
   * elasped seconds. */
  public int getProgress() { return -1; }

  /** for compatibility with ucar.nc2.CancelTask */
  public void setError(String error) { this.error = error; }
}