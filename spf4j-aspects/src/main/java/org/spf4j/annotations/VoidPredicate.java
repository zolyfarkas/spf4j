
package org.spf4j.annotations;

import org.spf4j.base.Callables;

public final class VoidPredicate implements Callables.AdvancedRetryPredicate<Exception> {

    @Override
    public Callables.AdvancedAction apply(final Exception value) {
        throw new UnsupportedOperationException();
    }



}
