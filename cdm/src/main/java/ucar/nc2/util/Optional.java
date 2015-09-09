/* Copyright Unidata */
package ucar.nc2.util;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Optional with an error message when empty
 */
public class Optional<T> {
  private final T value;
  private final String errMessage;

  public static<T> Optional<T> empty(String errMessage) {
    return new Optional<>(null, errMessage);
  }

  public static <T> Optional<T> of(T value) {
    value = Objects.requireNonNull(value);
    return new Optional<>(value, null);
  }

  private Optional(T value, String errMessage) {
    this.value = value;
    this.errMessage = errMessage;
  }

  public T get() {
    if (value == null) {
      throw new NoSuchElementException("No value present");
    }
    return value;
  }

  public boolean isPresent() {
    return value != null;
  }

  public String getErrorMessage() {
    return errMessage;
  }
}

