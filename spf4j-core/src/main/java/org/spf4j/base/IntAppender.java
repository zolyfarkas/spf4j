
package org.spf4j.base;

import com.google.common.annotations.GwtCompatible;
import java.io.IOException;

/**
 *
 * @author zoly
 */
@GwtCompatible
public interface IntAppender {

  void append(int number, Appendable appendTo) throws IOException;


  final class CommentNumberAppender implements IntAppender {

    public static final  CommentNumberAppender INSTANCE = new CommentNumberAppender();

    private CommentNumberAppender() { }

    @Override
    public void append(final int number, final Appendable appendTo) throws IOException {
      appendTo.append("/* ").append(Integer.toString(number)).append(" */ ");
    }

  }

  final class SimplePrefixNumberAppender implements IntAppender {

    public static final  SimplePrefixNumberAppender INSTANCE = new SimplePrefixNumberAppender();

    private SimplePrefixNumberAppender() { }

    @Override
    public void append(final int number, final Appendable appendTo) throws IOException {
      appendTo.append(Integer.toString(number)).append(": ");
    }

  }

}
