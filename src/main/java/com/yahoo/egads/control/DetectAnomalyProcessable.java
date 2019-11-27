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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Properties;

@Slf4j
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
        // 训练应该是为了构建预测模型
        modelAdapter.train();

        // Finding the expected values
        // 获取时序数据（整个原始时序数据，从数据的起始时间到数据的截至时间）的预测值
        // 一个模型对应一段数据，多个模型就会有多个数据，统一放到List中
        // 这个List里面存放的是各种算法模型产生的预测值，每个算法模型产生的预测值，对应list的一个对象，一个对象是一个List<Entry>结构
        ArrayList<TimeSeries.DataSequence> list = modelAdapter.forecast(
            modelAdapter.metric.startTime(), modelAdapter.metric.lastTime());
        String tdTmep = list.get(0).toString();
        log.info("------------list.get(0):{}", list.get(0).toString());
        // For each model's prediction in the ModelAdapter
        for (TimeSeries.DataSequence dataSequence : list) {
            // Reseting the anomaly detectors
            anomalyDetector.reset();

            // Unsupervised tuning of the anomaly detectors 异常探测器的无监督调谐
            anomalyDetector.tune(dataSequence, null);
            log.info("调谐后的 》》》》》  list.get(0) 没发生变化？:{}", dataSequence.toString().equals(tdTmep));
            if (!dataSequence.toString().equals(tdTmep)) {
                log.info(">>>>>tdTmep: {}", tdTmep);
                log.info(">>>>>dataSequence.toString: {}", dataSequence.toString());
            }
            // Detecting anomalies for each anomaly detection model in anomaly detector 异常检测
            // anomalyDetector.metric是原始数据   dataSequence 经典模型 的 预测数据
            anomalyList = anomalyDetector.detect(anomalyDetector.metric, dataSequence);

            /******************************************************
             * 以下代码的作用只是进行输出展示，可以输出到数据库
             * ，也可以输出到控制台和GUI界面
             */

            // Writing the anomalies to AnomalyDB
            // OUTPUT  有 控制台输出 和 GUI输出 两种 ， ANOMALY_DB是输出到异常检测数据库
            if (config.getProperty("OUTPUT") != null && config.getProperty("OUTPUT").equals("ANOMALY_DB")) {
                for (Anomaly anomaly : anomalyList) {
                    // TODO: Batch Anomaly Process.
                }
            } else if (config.getProperty("OUTPUT") != null && config.getProperty("OUTPUT").equals("GUI")) {
                // 页面绘图函数, modelAdapter.metric.data - 原始数据， dataSequence - 预测数据， anomalyList - 一个属性的异常点， config - 配置文件
                GUIUtils.plotResults(modelAdapter.metric.data, dataSequence, anomalyList, config);
                printAnomalyList();
            } else if (config.getProperty("OUTPUT") != null && config.getProperty("OUTPUT").equals("PLOT")) {
                printAnomalyList();
            } else { // 如果没有设置输出，默认输出到控制台
                printAnomalyList();
            }
        }
    }

    public ArrayList<Anomaly> result() throws Exception {
        return getAnomalyList();
    }

    private void printAnomalyList() {
        if (this.anomalyList == null || this.anomalyList.size() == 0) {
            log.info("异常检测结果:{}", "没有异常");
        } else {
            for (Anomaly anomaly : anomalyList) {
                log.info(StringUtils.isBlank(anomaly.toPerlString()) ? "没有异常" : "存在异常 " + anomaly.intervals.size() + "\n" + anomaly.toPerlString());
            }
        }
    }
}
