/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.spf4j.base;

import java.util.List;
import junit.framework.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class RTreeTest {
    
    /**
     * Test of search method, of class RTree.
     */
    @Test
    public void testSearch() {
        RTree<String> rectangles = new RTree<String>();
        rectangles.insert(new float [] {1, 1}, "Point 1,1");
        rectangles.insert(new float [] {0.5f, 0.5f}, new float [] {1, 1}, "Rectangle 0.5, 0.5, 1, 1");
        List<String> result = rectangles.search(new float [] {0, 0}, new float [] {2, 2});
        Assert.assertEquals(2, result.size());
        List<String> result2 = rectangles.search(new float [] {1.5f, 1.5f}, new float [] {2, 2});
        Assert.assertEquals(1, result2.size());
    }

}