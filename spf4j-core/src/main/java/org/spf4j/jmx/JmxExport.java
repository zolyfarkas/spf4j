

package org.spf4j.jmx;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark setters and getters of attributes to export.
 * Only setters and getters are supported.
 * name of the jmx attribute exported will be extracted from sette/getter name.
 * attribute description can be added to the annotation.
 * 
 * @author zoly
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.PARAMETER })
public @interface JmxExport {
     String name() default "";
     String description() default "";
}
