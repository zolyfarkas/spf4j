
package org.spf4j.base;

import java.util.function.Consumer;
import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 *
 * @author zoly
 */
@ParametersAreNonnullByDefault
public final class Iterables {

  private Iterables() { }

  @Nullable
  @CheckReturnValue
  public static <T> RuntimeException forAll(final Iterable<T> itterable, final Consumer<? super T> consumer)  {
    MutableHolder<RuntimeException> hex = MutableHolder.of(null);
    itterable.forEach(new Consumer<T>() {
      @Override
      public void accept(final T t) {
        try {
          consumer.accept(t);
        } catch (RuntimeException ex1) {
          RuntimeException ex = hex.getValue();
          if (ex == null) {
            hex.setValue(ex1);
          } else {
            hex.setValue(Throwables.suppress(ex1, ex));
          }
        }
      }
    });
    return hex.getValue();
  }

  public static <T> void forAll2(final Iterable<T> itterable, final Consumer<? super T> consumer)  {
    RuntimeException ex = forAll(itterable, consumer);
    if (ex != null) {
      throw ex;
    }
  }



}
