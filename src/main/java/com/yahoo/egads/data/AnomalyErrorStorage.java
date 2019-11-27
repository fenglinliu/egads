/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

package com.yahoo.egads.data;

import com.yahoo.egads.data.TimeSeries.DataSequence;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

public class AnomalyErrorStorage {

    // Denominator used in the MASE error metric.
    /**
     * MASE  平均绝对标度误差（平均绝对标准化误差）
     * 它给出每个误差与基线平均误差的比率。
     * 是衡量预测精度的四个主要指标之一
     * @See https://www.statisticshowto.datasciencecentral.com/mean-absolute-scaled-error/
     */
    protected float maseDenom;
    // Maps error names to error indicies.
    protected Map<String, Integer> errorToIndex;
    // Maps error index to error names.
    /**
     *         indexToError.put(0, "mapee");
     *         indexToError.put(1, "mae");
     *         indexToError.put(2, "smape");
     *         indexToError.put(3, "mape");
     *         indexToError.put(4, "mase");
     *         在本类初始化的时候，构造了五个误差统计指标
     */
    protected Map<Integer, String> indexToError;
    boolean isInit = false;

    // Getter methods.
    public Map<String, Integer> getErrorToIndex() {
        return errorToIndex;
    }
    public Map<Integer, String> getIndexToError() {
        return indexToError;
    }
    
    // Force the user to define this constructor that acts as a
    // factory method.
    public AnomalyErrorStorage() {
        // Init error indicies that are filled in computeErrorMetrics method.
        errorToIndex = new HashMap<String, Integer>();
        errorToIndex.put("mapee", 0);
        errorToIndex.put("mae", 1);
        errorToIndex.put("smape", 2);
        errorToIndex.put("mape", 3);
        errorToIndex.put("mase", 4);
        indexToError = new HashMap<Integer, String>();
        indexToError.put(0, "mapee");
        indexToError.put(1, "mae");
        indexToError.put(2, "smape");
        indexToError.put(3, "mape");
        indexToError.put(4, "mase");
    }
    
    // Initializes all anomaly errors.
    public HashMap<String, ArrayList<Float>> initAnomalyErrors(DataSequence observedSeries, DataSequence expectedSeries) {
        int n = observedSeries.size();
        
        // init MASE.
        for (int i = 1; i < n; i++) {
            maseDenom += Math.abs(observedSeries.get(i).value - observedSeries.get(i - 1).value);
        }
        maseDenom = maseDenom / (n - 1);
        HashMap<String, ArrayList<Float>> allErrors = new HashMap<String, ArrayList<Float>>();
        
        for (int i = 0; i < n; i++) {
            // 计算两个值之间的误差（不同的统计标准，会计算出不同的值，这些值以数组形式存放）；computeErrorMetrics的形参中 第一个值是预测点的value，第二值是观测点的value
            Float[] errors = computeErrorMetrics(expectedSeries.get(i).value, observedSeries.get(i).value);
            // 向allErrors中添加 对应的指标名 和 指标值
            for (int j = 0; j < errors.length; j++) {
                if (!allErrors.containsKey(indexToError.get(j))) {
                    allErrors.put(indexToError.get(j), new ArrayList<Float>());
                }
                // tmp 是 误差指标indexToError.get(j) 对应的 值序列
                ArrayList<Float> tmp = allErrors.get(indexToError.get(j));
                tmp.add(errors[j]);
                allErrors.put(indexToError.get(j), tmp);
            }            
        } // 这个for循环执行完最后形成k-v结构，k是5个误差指标名，v是observedSeries和expectedSeries之间每一个点 的 误差值 构成的序列
        isInit = true;
        return allErrors;
    }
    
    // Computes the standard error metrics including MAE（or MAD）, sMAPE, MAPE, MASE.
    // 在不同统计公式下，计算两个点的误差度量
    public Float[] computeErrorMetrics(float expected, float actual) {
        float div = expected;
        if (expected == (float) 0.0) {
          div = (float) 0.0000000001;
        }

        // 参考 https://www.statisticshowto.datasciencecentral.com/absolute-error/
        // Mean Absolute Error.
        // mae 平均绝对误差
        float mae = Math.abs(actual - expected);
        // Symmetric Mean Absolute Error.
        // 对称平均绝对误差
        float smape = (200 * Math.abs(actual - expected)) / ((Math.abs(actual) + Math.abs(expected)) == 0 ? (float) 1.0 : (float) (Math.abs(actual) + Math.abs(expected)));
        // Mean Absolute Percentage Error.
        // 绝对百分比误差
        // https://www.statisticshowto.datasciencecentral.com/mean-absolute-percentage-error-mape/
        float mape = Math.abs(actual) == 0 ? (float) 0.0 : ((100 * Math.abs(actual - expected)) / (float) Math.abs(actual));
        // Mean Absolute Scaled Error.
        // 平均绝对标度误差
        // https://www.statisticshowto.datasciencecentral.com/mean-absolute-scaled-error/
        float mase = Math.abs(maseDenom) == 0.0 ? (float) 0.0 : Math.abs(actual - expected) / Math.abs(maseDenom);
        // Mean Absolute Percentage Error (scaled by the expected value).
        float mapee = (expected == actual) ? (float) 0.0 : Math.abs((100 * ((actual / div) - 1)));
        
        // Store all errors.
        Float[] errors = new Float[5];
        errors[0] = mapee;
        errors[1] = mae;
        errors[2] = smape;
        errors[3] = mape;
        errors[4] = mase;
        return errors;
    }
}
