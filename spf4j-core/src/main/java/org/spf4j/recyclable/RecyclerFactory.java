
package org.spf4j.recyclable;

import org.spf4j.recyclable.impl.LeaseImpl;

/**
 *
 * @author zoly
 */
public final class RecyclerFactory {

    private RecyclerFactory() { }
    
    public static <T> LeaseSupplier<T> toLeaseSupplier(final RecyclingSupplier<T> rs) {
        
        return new LeaseSupplier<T>() {

            @Override
            public Lease<T> get() {
                return new LeaseImpl<>(rs);
            }
        };
        
    }
    
}
