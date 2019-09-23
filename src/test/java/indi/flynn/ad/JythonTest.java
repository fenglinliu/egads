package indi.flynn.ad;

import org.python.util.PythonInterpreter;

import java.util.HashMap;
import java.util.Map;

/**
 * JythonTest
 *
 * @author Flynn
 * @version 1.0
 * @description Jython使用demo
 * @email liufenglin@163.com
 * @date 2019/9/22
 */
public class JythonTest {
    public static void main(String[] args) {
//        PythonInterpreter pyInterp = new PythonInterpreter();
//        for (int i = 0; i < map.size(); i++) {
//            map.get(i).get
//        }
        PythonInterpreter interpreter = new PythonInterpreter();
        interpreter.exec("print('hello')");
        interpreter.exec("print('Hello Python World!')");

    }
}
