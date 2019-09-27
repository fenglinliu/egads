package indi.flynn.ad;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.factory.Nd4j;

/**
 * ND4JTest
 *
 * @author Flynn
 * @version 1.0
 * @description ND4J is a scientific computing library for the JVM.
 *  as same as Numpy
 *  <p>
 *      https://deeplearning4j.org/docs/latest/nd4j-quickstart
 *      https://docs.scipy.org/doc/numpy/user/quickstart.html
 *  <p/>
 * @email liufenglin@163.com
 * @date 2019/9/23
 */
public class ND4JTest {

    public static void main(String[] args) {
        INDArray x = Nd4j.zeros(3,4);
        // The number of axes (dimensions) of the array.
        int dimensions = x.rank();

// The dimensions of the array. The size in each dimension.
        int[] shape = x.shape();

// The total number of elements.
        long length = x.length();

        double arr_2d[][]={{1.0,2.0,3.0},{4.0,5.0,6.0},{7.0,8.0,9.0}};

        Nd4j.arange(3);

    }
}
