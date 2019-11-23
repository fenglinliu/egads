/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// A template for doing Anomaly Detection.

package com.yahoo.egads.control;

import java.util.ArrayList;

import com.yahoo.egads.data.Anomaly;
import com.yahoo.egads.data.TimeSeries;
import com.yahoo.egads.utilities.GUIUtils;
import java.util.Properties;

public class DetectAnomalyProcessable implements ProcessableObject {
    /**
     * 模型适配器，模型适配器需要：时序模型ts、周期、时序预测模型
     */
    private ModelAdapter modelAdapter;
    /**
     * 异常检测器，异常检测器需要：时序模型ts、周期、异常检测模型
     */
    private AnomalyDetector anomalyDetector;
    /**
     * 配置文件
     */
    private Properties config;
    private ArrayList<Anomaly> anomalyList;

    public ArrayList<Anomaly> getAnomalyList() {
        return anomalyList;
    }


    DetectAnomalyProcessable(ModelAdapter modelAdapter, AnomalyDetector anomalyDetector, Properties config) {
        this.modelAdapter = modelAdapter;
        this.anomalyDetector = anomalyDetector;
        this.config = config;
        anomalyList = new ArrayList<>();
    }

    /**
     * 异常检测的实际处理操作
     *
     * @throws Exception
     */
    public void process() throws Exception {

        // Resetting the models
        modelAdapter.reset();

        // Training the model with the whole metric
        // 训练应该是为了得到预测值
        modelAdapter.train();

        // Finding the expected values
        ArrayList<TimeSeries.DataSequence> list = modelAdapter.forecast(
            modelAdapter.metric.startTime(), modelAdapter.metric.lastTime());

        // For each model's prediction in the ModelAdapter
        for (TimeSeries.DataSequence ds : list) {
            // Reseting the anomaly detectors
            anomalyDetector.reset();

            // Unsupervised tuning of the anomaly detectors
            anomalyDetector.tune(ds, null);

            // Detecting anomalies for each anomaly detection model in anomaly detector
            anomalyList = anomalyDetector.detect(anomalyDetector.metric, ds);

            // Writing the anomalies to AnomalyDB
            if (config.getProperty("OUTPUT") != null && config.getProperty("OUTPUT").equals("ANOMALY_DB")) {
                for (Anomaly anomaly : anomalyList) {
                    // TODO: Batch Anomaly Process.
                }
            } else if (config.getProperty("OUTPUT") != null && config.getProperty("OUTPUT").equals("GUI")) {
                GUIUtils.plotResults(modelAdapter.metric.data, ds, anomalyList, config);
            } else if (config.getProperty("OUTPUT") != null && config.getProperty("OUTPUT").equals("PLOT")) {
                for (Anomaly anomaly : anomalyList) {
                    System.out.print(anomaly.toPlotString());
                }
            } else {
                for (Anomaly anomaly : anomalyList) {
                    System.out.print(anomaly.toPerlString());
                }
            }
        }
    }

    public ArrayList<Anomaly> result() throws Exception {
        return getAnomalyList();
    }
}
