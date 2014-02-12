/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.spf4j.zel.vm;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author zoly
 */
public class TestZelMultiThreaded {
    
    /**
     * GO equivalent:
     *
     * // Concurrent computation of pi.
     *
     * func main() { fmt.Println(pi(5000)) }
     *
     * // pi launches n goroutines to compute an // approximation of pi.
     * func pi(n int) float64 {
     * ch := make(chan float64)
     * for k := 0; k <= n; k++
     * { go term(ch, float64(k)) }
     * f := 0.0
     * for k := 0; k <= n; k++ { f += <-ch }
     * return f
     * }
     *
     * func term(ch chan float64, k float64) { ch <- 4 * math.Pow(-1, k) / (2*k + 1) }
     *
     * @throws CompileException
     * @throws ZExecutionException
     * @throws InterruptedException
     */
    @Test
    public void testParallelPi() throws CompileException, ZExecutionException, InterruptedException {
        String pi = "pi = func (x) {"
                + "term = func (k) {4 * (-1 ** k) / (2d * k + 1)};"
                + "for i = 0; i < x; i = i + 1 { parts[i] = term(i) };"
                + "for result = 0, i = 0; i < x; i = i + 1 { result = result + parts[i] }"
                + "return result};"
                + "pi(x)";
        Program prog = Program.compile(pi, "x");
        System.out.println(prog);
        long startTime = System.currentTimeMillis();
        Number result = (Number) prog.execute(1000000);
        long endTime = System.currentTimeMillis();
        System.out.println("zel pi = " + result + " in " + (endTime - startTime) + "ms");
        // pi is 3.141592653589793
        Assert.assertEquals(3.141592653589793, result.doubleValue(), 0.0001);
        
        startTime = System.currentTimeMillis();
        double dresult = pi(100000);
        endTime = System.currentTimeMillis();
        System.out.println("java pi = " + dresult + " in " + (endTime - startTime) + "ms");
    }
    
     private static  double term(final int k) {
        return 4 * Math.pow(-1, k) / (2d * k + 1);
     }
    
     private static double pi(final int nr) {
         double result = 0;
         for (int i = 0; i < nr; i++) {
             result += term(i);
         }
         return result;
     }


    
}
