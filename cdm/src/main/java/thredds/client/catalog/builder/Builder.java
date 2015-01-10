/*
 * Copyright 1998-2015 University Corporation for Atmospheric Research/Unidata
 *
 *   Portions of this software were developed by the Unidata Program at the
 *   University Corporation for Atmospheric Research.
 *
 *   Access and use of this software shall impose the following obligations
 *   and understandings on the user. The user is granted the right, without
 *   any fee or cost, to use, copy, modify, alter, enhance and distribute
 *   this software, and any derivative works thereof, and its supporting
 *   documentation for any purpose whatsoever, provided that this entire
 *   notice appears in all copies of the software, derivative works and
 *   supporting documentation.  Further, UCAR requests that the user credit
 *   UCAR/Unidata in any publications that result from the use of this
 *   software or in any product that includes this software. The names UCAR
 *   and/or Unidata, however, may not be used in any advertising or publicity
 *   to endorse or promote any products or commercial entity unless specific
 *   written permission is obtained from UCAR/Unidata. The user also
 *   understands that UCAR/Unidata is not obligated to provide the user with
 *   any support, consulting, training or assistance of any kind with regard
 *   to the use, operation and performance of this software nor to provide
 *   the user with any updates, revisions, new versions or "bug fixes."
 *
 *   THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 *   IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *   WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 *   INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 *   FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 *   NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 *   WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */
package thredds.client.catalog.builder;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder superclass handling "issues"
 *
 * @author caron
 * @since 1/9/2015
 */
public class Builder {

  public enum Severity {FATAL, ERROR, WARNING}

  public static class Issue {
    private final Severity severity;
    private final String message;
    private final Builder builder;
    private final Exception cause;

    public Issue(Severity severity, String message, Builder builder, Exception cause) {
      if (severity == null || message == null || builder == null)
        throw new IllegalArgumentException("Null severity level, message, and/or builder.");
      this.severity = severity;
      this.message = message;
      this.builder = builder;
      this.cause = cause;
    }

    public Severity getSeverity() {
      return this.severity;
    }

    public String getMessage() {
      return this.message;
    }

    public Builder getBuilder() {
      return this.builder;
    }

    public Exception getCause() {
      return this.cause;
    }
  }


  protected List<Issue> issues;
  private int numFatalIssues = 0;
  private int numErrorIssues = 0;
  private int numWarningIssues = 0;

  public void addIssue(Severity severity, String message, Builder builder, Exception cause) {
    this.issues.add(new Issue(severity, message, builder, cause));
    trackSeverity(severity);
  }

  public void addIssue(Issue issue) {
    if (issue == null) return;
    if (issues == null) issues = new ArrayList<>();
    this.issues.add(issue);
    trackSeverity(issue.getSeverity());
  }

  public void addAllIssues(List<Issue> issues) {
    if (issues == null) return;
    if (issues.size() == 0) return;
    if (this.issues == null) this.issues = new ArrayList<>();
    this.issues.addAll(issues);
    for (Issue curIssue : issues) {
      trackSeverity(curIssue.getSeverity());
    }
  }

  public List<Issue> getIssues() {
    return this.issues;
  }

  public boolean isValid() {
    if (this.numFatalIssues > 0 || this.numErrorIssues > 0)
      return false;
    return true;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Issue bfi : this.issues) {
      sb.append(bfi.getMessage()).append("\n");
    }
    return sb.toString();
  }

  private void trackSeverity(Severity severity) {
    if (severity.equals(Severity.FATAL))
      this.numFatalIssues++;
    else if (severity.equals(Severity.ERROR))
      this.numErrorIssues++;
    else if (severity.equals(Severity.WARNING))
      this.numWarningIssues++;
  }
}
