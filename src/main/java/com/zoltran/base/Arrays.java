/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.zoltran.base;

/**
 *
 * @author zoly
 */
public final class Arrays {
    private Arrays() {}
    
    public static double[] getColumnAsDoubles(long [][] data, int columnNumber) {
        double [] result = new double [data.length];
        for (int i=0; i< result.length; i++) {
            result[i] =data [i][columnNumber];
        }
        return result;
    }
    
    
    public static double[] getColumn(double [][] data, int columnNumber) {
        double [] result = new double [data.length];
        for (int i=0; i< result.length; i++) {
            result[i] =data [i][columnNumber];
        }
        return result;
    }
    
    public static double[] toDoubleArray(long [] larr) {
        double [] result = new double[larr.length];
        for (int i=0; i< larr.length;i++) {
            result[i] = larr[i];
        }
        return result;
    }
    
    public static double [] divide (double [] arr1, double [] arr2) {
        double [] result = new double [arr1.length];
        for (int i=0; i< result.length; i++) {
            result [i] = arr1[i]/arr2[i];
        }
        return result;
    }
    
    
}
