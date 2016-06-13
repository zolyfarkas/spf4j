
package org.spf4j.base;

import java.io.IOException;

/**
 *
 * @author zoly
 */
public interface IntAppender {

  void append(int number, Appendable appendTo) throws IOException;


  final class CommentNumberAppender implements IntAppender {

    private CommentNumberAppender() { }

    @Override
    public void append(final int number, final Appendable appendTo) throws IOException {
      appendTo.append("/* ").append(Integer.toString(number)).append(" */ ");
    }

    public static final  CommentNumberAppender INSTANCE = new CommentNumberAppender();

  }

  final class SimplePrefixNumberAppender implements IntAppender {

    private SimplePrefixNumberAppender() { }

    @Override
    public void append(final int number, final Appendable appendTo) throws IOException {
      appendTo.append(Integer.toString(number)).append(": ");
    }

    public static final  SimplePrefixNumberAppender INSTANCE = new SimplePrefixNumberAppender();

  }

}
