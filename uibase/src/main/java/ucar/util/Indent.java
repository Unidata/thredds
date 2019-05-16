package ucar.util;

/**
 * Maintains indentation level for printing nested structures.
 */
public class Indent {
  private int nspaces = 0;

  private int level = 0;
  private StringBuilder blanks;
  private String indent = "";

  // nspaces = how many spaces each level adds.
  // max 100 levels
  public Indent(int nspaces) {
    this.nspaces = nspaces;
    blanks = new StringBuilder();
    for (int i = 0; i < 100 * nspaces; i++)
      blanks.append(" ");
  }

  public Indent incr() {
    level++;
    setIndentLevel(level);
    return this;
  }

  public Indent decr() {
    level--;
    setIndentLevel(level);
    return this;
  }

  public int level() {
    return level;
  }

  public String toString() {
    return indent;
  }

  public void setIndentLevel(int level) {
    this.level = level;
    if (level * nspaces >= blanks.length())
      System.out.printf("HEY setIndentLevel!%n");
    int end = Math.min(level * nspaces, blanks.length());
    indent = blanks.substring(0, end);
  }
}

