/*
 * Copyright (c) 1998-2018 John Caron and University Corporation for Atmospheric Research/Unidata
 * See LICENSE for license information.
 */

package ucar.nc2.util;

/**
 * Simple implementation of CancelTask, used in order to get status return.
 *
 * @author caron
 * @since 4/10/13
 */
public class CancelTaskImpl implements CancelTask {
  protected boolean done = false;
  protected boolean success = false;
  protected boolean cancel = false;
  protected String error = null;
  protected String note = null;
  protected int progress;

  /**
   * Application calls to see if task is success.
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Application call this to find out if there was an error.
   */
  public boolean isError() {
    return error != null;
  }

  /**
   * Application call this to get the error message, if any.
   */
  public String getErrorMessage() {
    return error;
  }

  /**
   * Application calls to see if task is done.
   */
  public boolean isDone() {
    return done;
  }

  /**
   * Application calls to see if task is done.
   */
  public void setDone(boolean done) {
    this.done = done;
  }

  /**
   * Application will call this when the user cancels.
   */
  public void cancel() {
    cancel = true;
  }

  /**
   * Application call this to get the progress message, if any.
   */
  public String getProgressMessage() {
    return note;
  }

  /**
   * Application call this to get the progress count, if any.
   */
  public int getProgress() {
    return progress;
  }

  @Override
  public boolean isCancel() {
    return cancel;
  }

  @Override
  public void setError(String error) {
    this.error = error;
  }

  @Override
  public void setProgress(String msg, int progress) {
    this.note = msg;
    if (progress > 0) this.progress = progress;
  }

  @Override
  public String toString() {
    if (cancel) return "was canceled";
    if (isError()) return "error= "+error;
    if (success) return "success";
    return "finished="+done;
  }
}
