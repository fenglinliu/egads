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
import java.util.List;
import java.util.ArrayList;

import com.yahoo.egads.data.Anomaly.IntervalSequence;
import com.yahoo.egads.data.Anomaly.Interval;
import com.yahoo.egads.data.AnomalyErrorStorage;
import com.yahoo.egads.data.TimeSeries.DataSequence;
import com.yahoo.egads.utilities.DBSCANClusterer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.ml.clustering.Cluster;

import com.yahoo.egads.utilities.IdentifiedDoublePoint;

import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.json.JSONObject;
import org.json.JSONStringer;

@Slf4j
public class DBScanModel extends AnomalyDetectionAbstractModel {

    // The constructor takes a set of properties
    // needed for the simple model. This includes the sensitivity.
    // 在构造函数中根据配置文件初始化为mape------》10格式的数据，
    // 如果配置文件没有配置则是一个EmptyMap
    // 这个阈值，指的是误差（例如，mape、mase等误差指标）指标的阈值
    private Map<String, Float> threshold;
    private int maxHrsAgo;
    private long windowStart;
    // modelName.
    public String modelName = "DBScanModel";
    /**
     *         errorToIndex.put("mapee", 0);
     *         errorToIndex.put("mae", 1);
     *         errorToIndex.put("smape", 2);
     *         errorToIndex.put("mape", 3);
     *         errorToIndex.put("mase", 4);
     *         默认构造的数据
     */
    public AnomalyErrorStorage anomalyErrorStorage = new AnomalyErrorStorage();
    /**
     * 在tune调谐函数中对dbscanClusterer对象进行初始化
     * 并对minPoints和eps进行赋值
     * minPoints和eps是初始化dbscanClusterer的重要参数
     */
    private DBSCANClusterer<IdentifiedDoublePoint> dbscanClusterer = null;
    private int minPoints = 2;
    private double eps = 500;
    
    public DBScanModel(Properties config) {
        // 父类中初始三个参数：聚类的标准差、数据集中异常的期望百分比、输出方式
        super(config);
       
        if (config.getProperty("MAX_ANOMALY_TIME_AGO") == null) {
            throw new IllegalArgumentException("MAX_ANOMALY_TIME_AGO is NULL");
        }
        // 异常时间戳的最大限制值
        this.maxHrsAgo = new Integer(config.getProperty("MAX_ANOMALY_TIME_AGO"));
        // 指定timeseries中的检测窗口开始时间
        this.windowStart = new Long(config.getProperty("DETECTION_WINDOW_START_TIME"));

        // 设定的阈值主要是针对 误差指标 的阈值，所以是k-v结构，k是误差指标名，v是误差指标的阈值
        // 如果在没有在配置文件中设置，则是一个空map
        this.threshold = parseMap(config.getProperty("THRESHOLD"));
        // 如果配置文件中设定了阈值，但是解析出来的阈值Map为空，则抛出阈值解析错误异常
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

    /**
     * 调谐的目的是为了给本类的三个参数赋值：eps， minPoints，dbscanClusterer
     *
     * @param observedSeries
     * @param expectedSeries
     * @param anomalySequence
     * @throws Exception
     */
    @Override
    public void tune(DataSequence observedSeries,
                     DataSequence expectedSeries,
                     IntervalSequence anomalySequence) throws Exception {
        // Compute the time-series of errors.
        HashMap<String, ArrayList<Float>> allErrors = anomalyErrorStorage.initAnomalyErrors(observedSeries, expectedSeries);
        List<IdentifiedDoublePoint> points = new ArrayList<IdentifiedDoublePoint>();
        EuclideanDistance ed = new EuclideanDistance();
        int n = observedSeries.size();
        
        for (int i = 0; i < n; i++) {
            double[] d = new double[(anomalyErrorStorage.getIndexToError().keySet()).size()];
           
            for (int e = 0; e < (anomalyErrorStorage.getIndexToError().keySet()).size(); e++) {
                 d[e] = allErrors.get(anomalyErrorStorage.getIndexToError().get(e)).get(i);
            }
            points.add(new IdentifiedDoublePoint(d, i));
        }
        
        double sum = 0.0;
        double count = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                sum += ed.compute(points.get(i).getPoint(), points.get(j).getPoint());
                count++;
            }
        }
        // 调谐的目的
        eps = ((double) this.sDAutoSensitivity) * (sum / count);
        // 调谐目的
        minPoints = ((int) Math.ceil(((double) this.amntAutoSensitivity) * ((double) n)));
        // 调谐目的
        dbscanClusterer = new DBSCANClusterer<IdentifiedDoublePoint>(eps, minPoints);
    }
  
    @Override
    public IntervalSequence detect(DataSequence observedSeries,
                                   DataSequence expectedSeries) throws Exception {
        
        IntervalSequence output = new IntervalSequence();
        int n = observedSeries.size();
        // Get an array of thresholds.
        // anomalyErrorStorage（误差的阈值）在本类中初始化了，初始化的构造方法中存了5个元素
        // 如果配置文件中没指定误差阈值，则thresholdErrors是一个Empty Array
        Float[] thresholdErrors = new Float[anomalyErrorStorage.getErrorToIndex().size()];
        for (Map.Entry<String, Float> entry : this.threshold.entrySet()) {
            thresholdErrors[anomalyErrorStorage.getErrorToIndex().get(entry.getKey()/*误差指标的名字*/)] = Math.abs(entry.getValue()/*误差指标的值*/);
        }
        
        // Compute the time-series of errors. 计算时序数据中的异常
        // 形成k-v结构，k是5个误差指标名，v是observedSeries和expectedSeries之间每一个点 的 误差值 构成的序列
        HashMap<String, ArrayList<Float>> allErrors = anomalyErrorStorage.initAnomalyErrors(observedSeries, expectedSeries);
        List<IdentifiedDoublePoint> points = new ArrayList<IdentifiedDoublePoint>();
        // 遍历观测数据
        for (int i = 0; i < n; i++) {
            // 按照误差统计指标的顺序存放，第i个点时的误差数据值
            double[] d = new double[(anomalyErrorStorage.getIndexToError().keySet()).size()];
           
            for (int e = 0; e < (anomalyErrorStorage.getIndexToError().keySet()).size(); e++) {
                 d[e] = allErrors.get(anomalyErrorStorage.getIndexToError().get(e)/*获取误差统计指标的名字*/).get(i)/*获取误差统计指标在第i个点时的值*/;
            }
            // 把第i个点（观测点和预测值点） 和 对应的五大误差统计指标值d[] 统一存储为一个Point，并放到points序列中
            points.add(new IdentifiedDoublePoint(d, i));
        }
        // 对所有点的误差统计度量进行聚类，将异常的cluster返回回来，对于DBSCAN，anomalousClusters的大小为1
        List<Cluster<IdentifiedDoublePoint>> anomalousClusters = dbscanClusterer.cluster(points);


        for(Cluster<IdentifiedDoublePoint> tempAnomalousCluster: anomalousClusters) {
            // 去除异常聚类中的每个数据点（一个数据点的ID是他的索引，d[]是他的统计误差数据的集合）
            for (IdentifiedDoublePoint tempAnomalousPoint : tempAnomalousCluster.getPoints()) {
                // 获取异常点所在的位置索引
            	int i = tempAnomalousPoint.getId();
            	// 根据异常点的预测值和观测值再计算一次误差指标
                Float[] errors = anomalyErrorStorage.computeErrorMetrics(expectedSeries.get(tempAnomalousPoint.getId()).value/*获取该异常点的预测值*/,
                        observedSeries.get(tempAnomalousPoint.getId()).value/*获取该异常点的观测值*/);
                log.info("异常点 {}的观测值：TS:" + observedSeries.get(i).time + ", 5种误差度量值 E:" + arrayF2S(errors) + "误差指标的 阈值 ,TE:" + arrayF2S(thresholdErrors)
                        + ",OV:" + observedSeries.get(i).value + ",EV:" + expectedSeries.get(i).value, i);
                if (observedSeries.get(tempAnomalousPoint.getId()).value != expectedSeries.get(tempAnomalousPoint.getId()).value &&
                    (isDetectionWindowPoint(maxHrsAgo, windowStart, observedSeries.get(tempAnomalousPoint.getId()).time, observedSeries.get(0).time) ||
                    (maxHrsAgo == 0 && tempAnomalousPoint.getId() == (n - 1)))) {
                    output.add(new Interval(observedSeries.get(tempAnomalousPoint.getId()).time,
                    		                tempAnomalousPoint.getId(),
                                            errors,
                                            thresholdErrors,
                                            observedSeries.get(tempAnomalousPoint.getId()).value,
                                            expectedSeries.get(tempAnomalousPoint.getId()).value));
                }
            }
        }

        return output;
    }
}
