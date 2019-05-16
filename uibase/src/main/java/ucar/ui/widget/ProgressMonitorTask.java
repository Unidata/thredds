/*
 * Copyright (c) 1998-2018 University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */
package ucar.ui.widget;

/**
 * Subclass this for use in a ProgressMonitor. Responsibilities of your subclass:
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
 * Also can be adapted as a CancelTask implementation.
 *
 * @see ProgressMonitor
 */
public abstract class ProgressMonitorTask implements Runnable {
  protected boolean done = false;
  protected boolean success = false;
  protected boolean cancel = false;
  protected String error = null;
  protected String note = null;
  protected int progress;

  /** Here is where the work gets done. */
  public abstract void run();

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
    success = false;
  }
  /** ProgressMonitor displays this note in the ProgressMonitor. If null, ProgressMonitor will show
   * elasped seconds. */
  public String getNote() { return note; }
  /** ProgressMonitor displays this progress value in the ProgressMonitor. If <0, ProgressMonitor will show
   * elasped seconds. */
  public int getProgress() { return progress; }

  /** for compatibility with ucar.nc2.CancelTask */
  public void setError(String error) { this.error = error; }
  public void setProgress(String msg, int progress) {
    this.note = msg;
    if (progress > 0) this.progress = progress;
  }
}