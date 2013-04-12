/*
 * Copyright 1998-2013 University Corporation for Atmospheric Research/Unidata
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
