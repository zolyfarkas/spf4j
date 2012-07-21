/**
 * (c) Zoltan Farkas 2012
 */
package com.zoltran.perf.impl;

import java.awt.Color;
import java.awt.Paint;
import java.io.Serializable;
import org.jfree.chart.HashUtilities;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.util.PublicCloneable;

/**
 * A paint scale that returns shades of gray.
 *
 * @since 1.0.4
 */
public class InverseGrayScale
        implements PaintScale, PublicCloneable, Serializable {

    /** The lower bound. */
    private double lowerBound;

    /** The upper bound. */
    private double upperBound;

    /**
     * The alpha transparency (0-255).
     *
     * @since 1.0.13
     */
    private int alpha;

    /**
     * Creates a new <code>GrayPaintScale</code> instance with default values.
     */
    public InverseGrayScale() {
        this(0.0, 1.0);
    }

    /**
     * Creates a new paint scale for values in the specified range.
     *
     * @param lowerBound  the lower bound.
     * @param upperBound  the upper bound.
     *
     * @throws IllegalArgumentException if <code>lowerBound</code> is not
     *       less than <code>upperBound</code>.
     */
    public InverseGrayScale(double lowerBound, double upperBound) {
        this(lowerBound, upperBound, 255);
    }

    /**
     * Creates a new paint scale for values in the specified range.
     *
     * @param lowerBound  the lower bound.
     * @param upperBound  the upper bound.
     * @param alpha  the alpha transparency (0-255).
     *
     * @throws IllegalArgumentException if <code>lowerBound</code> is not
     *       less than <code>upperBound</code>, or <code>alpha</code> is not in
     *       the range 0 to 255.
     *
     * @since 1.0.13
     */
    public InverseGrayScale(double lowerBound, double upperBound, int alpha) {
        if (lowerBound >= upperBound) {
            throw new IllegalArgumentException(
                    "Requires " +lowerBound+" < " + upperBound +".");
        }
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException(
                    "Requires alpha in the range 0 to 255.");

        }
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
        this.alpha = alpha;
    }

    /**
     * Returns the lower bound.
     *
     * @return The lower bound.
     *
     * @see #getUpperBound()
     */
    public double getLowerBound() {
        return this.lowerBound;
    }

    /**
     * Returns the upper bound.
     *
     * @return The upper bound.
     *
     * @see #getLowerBound()
     */
    public double getUpperBound() {
        return this.upperBound;
    }

    /**
     * Returns the alpha transparency that was specified in the constructor.
     * 
     * @return The alpha transparency (in the range 0 to 255).
     * 
     * @since 1.0.13
     */
    public int getAlpha() {
        return this.alpha;
    }

    /**
     * Returns a paint for the specified value.
     *
     * @param value  the value (must be within the range specified by the
     *         lower and upper bounds for the scale).
     *
     * @return A paint for the specified value.
     */
    public Paint getPaint(double value) {
        double v = Math.max(value, this.lowerBound);
        v = Math.min(v, this.upperBound);
        int g = 255 -  (int) ((v - this.lowerBound) / (this.upperBound
                - this.lowerBound) * 255.0);
        // FIXME:  it probably makes sense to allocate an array of 256 Colors
        // and lazily populate this array...
        if (value>0 && g>240)
            g=240;           
        return new Color(g, g, g, this.alpha);
    }

    /**
     * Tests this <code>GrayPaintScale</code> instance for equality with an
     * arbitrary object.  This method returns <code>true</code> if and only
     * if:
     * <ul>
     * <li><code>obj</code> is not <code>null</code>;</li>
     * <li><code>obj</code> is an instance of <code>GrayPaintScale</code>;</li>
     * </ul>
     *
     * @param obj  the object (<code>null</code> permitted).
     *
     * @return A boolean.
     */
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof org.jfree.chart.renderer.GrayPaintScale)) {
            return false;
        }
        InverseGrayScale that = (InverseGrayScale) obj;
        if (this.lowerBound != that.lowerBound) {
            return false;
        }
        if (this.upperBound != that.upperBound) {
            return false;
        }
        if (this.alpha != that.alpha) {
            return false;
        }
        return true;
    }

    /**
     * Returns a hash code for this instance.
     *
     * @return A hash code.
     */
    public int hashCode() {
        int hash = 7;
        hash = HashUtilities.hashCode(hash, this.lowerBound);
        hash = HashUtilities.hashCode(hash, this.upperBound);
        hash = 43 * hash + this.alpha;
        return hash;
    }

    /**
     * Returns a clone of this <code>GrayPaintScale</code> instance.
     *
     * @return A clone.
     *
     * @throws CloneNotSupportedException if there is a problem cloning this
     *     instance.
     */
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
