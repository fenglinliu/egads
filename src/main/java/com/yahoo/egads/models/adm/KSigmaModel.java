/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// A simple thresholding model that returns an anomaly if it is above/below a certain threashold.

package com.yahoo.egads.models.adm;

import java.util.Properties;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import com.yahoo.egads.data.Anomaly.IntervalSequence;
import com.yahoo.egads.data.Anomaly.Interval;
import com.yahoo.egads.data.TimeSeries.DataSequence;
import com.yahoo.egads.utilities.AutoSensitivity;
import com.yahoo.egads.data.AnomalyErrorStorage;

import com.yahoo.egads.utilities.StatisticsUtils;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.json.JSONStringer;

@Slf4j
public class KSigmaModel extends AnomalyDetectionAbstractModel {

    // The constructor takes a set of properties
    // needed for the simple model. This includes the sensitivity.
    // 在构造函数中根据配置文件初始化为mape------》10格式的数据，
    // 如果配置文件没有配置则是一个EmptyMap
    // 这个阈值，指的是误差（例如，mape、mase等误差指标）指标的阈值
    private Map<String, Float>  threshold;
    private int maxHrsAgo;
    private long windowStart;
    // modelName.
    public String modelName = "KSigmaModel";
    public AnomalyErrorStorage anomalyErrorStorage = new AnomalyErrorStorage();
    
    public KSigmaModel(Properties config) {
        super(config);
        
        if (config.getProperty("MAX_ANOMALY_TIME_AGO") == null) {
            throw new IllegalArgumentException("MAX_ANOMALY_TIME_AGO is NULL");
        }
        this.maxHrsAgo = new Integer(config.getProperty("MAX_ANOMALY_TIME_AGO"));

        this.windowStart = new Long(config.getProperty("DETECTION_WINDOW_START_TIME"));

        this.threshold = parseMap(config.getProperty("THRESHOLD"));
            
        if (config.getProperty("THRESHOLD") != null && this.threshold.isEmpty() == true) {
            throw new IllegalArgumentException("THRESHOLD PARSE ERROR");
        } 
    }

    public void toJson(JSONStringer json_out) {

    }

    public void fromJson(JSONObject json_obj) {

    }

    public String getModelName() {
        return modelName;
    }

    @Override
    public String getType() {
        return "point_outlier";
    }

    @Override
    public void reset() {
        // At this point, reset does nothing.
    }

    @Override
    public void tune(DataSequence observedSeries, DataSequence expectedSeries,
            IntervalSequence anomalySequence) throws Exception {
        HashMap<String, ArrayList<Float>> allErrors = anomalyErrorStorage.initAnomalyErrors(observedSeries, expectedSeries);

        for (int i = 0; i < (anomalyErrorStorage.getIndexToError().keySet()).size(); i++) {
            // Add a new error metric if the error metric has not been
            // defined by the user.
            if (!threshold.containsKey(anomalyErrorStorage.getIndexToError().get(i)/*误差指标名*/)/*配置文件中 没有 设置的误差指标阈值*/) {
                Float[] fArray/*误差指标 的 值序Array列*/ = (allErrors.get(anomalyErrorStorage.getIndexToError().get(i)/*误差指标名*/)/*误差指标 的 值序List列*/).toArray(new Float[(allErrors.get(anomalyErrorStorage.getIndexToError().get(i)/*误差指标名*/)/*误差指标 的 值序List列*/).size()]);
                //threshold.put(anomalyErrorStorage.getIndexToError().get(i)/*误差指标名*/, /*返回指标的阈值*/AutoSensitivity.getKSigmaSensitivity(fArray, sDAutoSensitivity/*聚类的标准差*/));
                threshold.put(anomalyErrorStorage.getIndexToError().get(i)/*误差指标名*/, /*返回指标的阈值*/StatisticsUtils.getThreshold(fArray));

            }
        }
    }

    // Returns true this point is identified as a potential anomaly.
    public boolean isAnomalyADV(Float[] errors, Map<String, Float> threshold/*系统阈值的误差阈值， 5个值*/) {
        // Cycle through all available thresholds and return
        // true if any of them matches.
        int trueVote = 0;
        int falseVote = 0;
        for (Map.Entry<String, Float> entry/*每一个entry的k是误差指标名，v是误差指标的阈值*/ : threshold.entrySet()) {
            // disable mapee and mape.
            if (anomalyErrorStorage.getErrorToIndex().containsKey(entry.getKey())/*引用的误差指标一定要是提前设定了的*/ == true &&
                Math.abs(errors[anomalyErrorStorage.getErrorToIndex().get(entry.getKey())])/*计算出的误差阈值*/ >= Math.abs(entry.getValue())/*系统阈值的误差阈值*/) {
                trueVote++;
            } else {
                falseVote++;
            }
        }
        return (trueVote > falseVote) ? true : false;
    }

    // Returns true this point is identified as a potential anomaly.
    public boolean isAnomaly(Float[] errors, Map<String, Float> threshold/*系统阈值的误差阈值， 5个值*/) {

        // Cycle through all available thresholds and return
        // true if any of them matches.
        for (Map.Entry<String, Float> entry/*每一个entry的k是误差指标名，v是误差指标的阈值*/ : threshold.entrySet()) {
            // disable mapee and mape.
            if (anomalyErrorStorage.getErrorToIndex().containsKey(entry.getKey())/*引用的误差指标一定要是提前设定了的*/ == true &&
                    Math.abs(errors[anomalyErrorStorage.getErrorToIndex().get(entry.getKey())])/*计算出的误差阈值*/ >= Math.abs(entry.getValue())/*系统阈值的误差阈值*/) {
                // 只要有一个指标超过了，就判定为异常
                return true;
            }
        }
        return false;
    }

    @Override
    public IntervalSequence detect(DataSequence observedSeries,
            DataSequence expectedSeries) throws Exception {
        
        // At detection time, the anomaly thresholds shouldn't all be 0.
        // 读阈值进行累加
        Float threshSum = (float) 0.0;
        for (Map.Entry<String, Float> entry : this.threshold.entrySet()) {
            threshSum += Math.abs(entry.getValue());
        }
        
        // Get an array of thresholds.
        // 存储所有的阈值
        Float[] thresholdErrors = new Float[anomalyErrorStorage.getErrorToIndex().size()];
        for (Map.Entry<String, Float> entry : this.threshold.entrySet()) {
            thresholdErrors[anomalyErrorStorage.getErrorToIndex().get(entry.getKey())] = Math.abs(entry.getValue());
        }
        
        IntervalSequence output = new IntervalSequence();
        int n = observedSeries.size();
        
        for (int i = 0; i < n; i++) {
            // 计算统计的误差数据，5个值
            Float[] errors = anomalyErrorStorage.computeErrorMetrics(expectedSeries.get(i).value, observedSeries.get(i).value);
            log.info("TS:" + observedSeries.get(i).time + ",E:" + arrayF2S(errors) + ",TE:" + arrayF2S(thresholdErrors) + ",OV:" + observedSeries.get(i).value + ",EV:" + expectedSeries.get(i).value);
            if (observedSeries.get(i).value != expectedSeries.get(i).value/*观测值和预期值不一样的不一定是异常*/ &&
                threshSum > (float) 0.0 &&
                isAnomaly(errors, threshold/*系统阈值的误差阈值， 5个值*/)/*最关键的阈值检测代码*/ == true &&
                (isDetectionWindowPoint(maxHrsAgo, windowStart, observedSeries.get(i).time, observedSeries.get(0).time) ||
                (maxHrsAgo == 0 && i == (n - 1)))) {
                output.add(new Interval(observedSeries.get(i).time,
                		                i,
                                        errors,
                                        thresholdErrors,
                                        observedSeries.get(i).value,
                                        expectedSeries.get(i).value));
            }
        }
        return output;
    }
}
