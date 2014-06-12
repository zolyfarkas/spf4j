
package org.spf4j.annotations;

import com.google.common.base.Predicate;

public final class VoidPredicate implements Predicate<Exception> {

    @Override
    public boolean apply(final Exception input) {
        throw new UnsupportedOperationException();
    }
    
}
