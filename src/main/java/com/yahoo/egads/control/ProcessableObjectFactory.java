/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// A factory to create tasks based on the data and the config.

package com.yahoo.egads.control;

import com.yahoo.egads.data.TimeSeries;

import java.lang.reflect.Constructor;
import java.util.Properties;

import com.yahoo.egads.models.adm.*;
import com.yahoo.egads.models.tsmm.*;

public class ProcessableObjectFactory {

    public static ProcessableObject create(TimeSeries ts, Properties config) {
        // OP_TYPE 指定要进行的数据处理操作
        if (config.getProperty("OP_TYPE") == null) {
            throw new IllegalArgumentException("OP_TYPE is NULL");
        }
        if (config.getProperty("OP_TYPE").equals("DETECT_ANOMALY")) {
            // 建立模型适配器，建立模型适配器的时候会指定Period
            ModelAdapter modelAdapter = ProcessableObjectFactory.buildTSModel(ts, config);
            // 建立异常检测器（需要进行异常检测的原始数据{从ts中来}，以及 数据的 Period{从config中来}）
            AnomalyDetector anomalyDetector = ProcessableObjectFactory.buildAnomalyModel(ts, config);
            // 返回可以处理的对象的实例————————异常检测对象
            return (new DetectAnomalyProcessable(modelAdapter, anomalyDetector, config));
        } else if (config.getProperty("OP_TYPE").equals("UPDATE_MODEL")) {
            ModelAdapter ma = ProcessableObjectFactory.buildTSModel(ts, config);
            return (new UpdateModelProcessable(ma, ts.data, config));
        } else if (config.getProperty("OP_TYPE").equals("TRANSFORM_INPUT")) {
            ModelAdapter ma = ProcessableObjectFactory.buildTSModel(ts, config);
            return (new TransformInputProcessable(ma, config));
        }
        // Should not be here.
        System.err.println("Unknown OP_TYPE, returning UPDATE_MODEL ProcessableObject");
        ModelAdapter ma = ProcessableObjectFactory.buildTSModel(ts, config);
        return (new UpdateModelProcessable(ma, ts.data, config));
    }

    private static ModelAdapter buildTSModel(TimeSeries ts, Properties config) {
        ModelAdapter modelAdapter = null;
        try {
            Long period = (long) -1;
            if (config.getProperty("PERIOD") != null) { // 获取时序数据的周期，  0 - auto detect. -1 - disable.
              period = new Long(config.getProperty("PERIOD"));
            }
            if (period == 0) {
              if (ts.size() > 1) {
                // TODO 算法改进，时序数据的周期性不应该被简单的计算为，头两个时间的差值
                period = ts.data.get(1).time - ts.data.get(0).time;
              } else {
                  // TODO 算法改进，时序数据的周期性不应该被暴力指定
                period = (long) 1;
              }
            }
            // 用时序和周期性去建立模型适配器
            modelAdapter = new ModelAdapter(ts, period);
            // 获取时间预测模型
            String modelType = config.getProperty("TS_MODEL");
            // 加载时序预测模型的代码实现
            Class<?> tsModelClass = Class.forName("com.yahoo.egads.models.tsmm." + modelType);
            Constructor<?> constructor = tsModelClass.getConstructor(Properties.class);
            // 将配置文件作为构造参数，构造指定的时序预测模型
            TimeSeriesAbstractModel m = (TimeSeriesAbstractModel) constructor.newInstance(config);
            // 给模型适配器添加上模型
            modelAdapter.addModel(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 至此，模型适配器需要的：时序模型ts、周期、时序预测模型  都加入了
        return modelAdapter;
    }

    private static AnomalyDetector buildAnomalyModel(TimeSeries ts, Properties config) {
        AnomalyDetector ad = null;
        try {
            Long period = (long) -1;
            if (config.getProperty("PERIOD") != null) {
              period = new Long(config.getProperty("PERIOD"));
            }
            if (period == 0) {
              if (ts.size() > 1) {
                  // TODO 算法改进，时序数据的周期性不应该被简单的计算为，头两个时间的差值
                period = ts.data.get(1).time - ts.data.get(0).time;
              } else {
                  // TODO 算法改进，时序数据的周期性不应该被暴力指定
                period = (long) 1;
              }
            }
            ad = new AnomalyDetector(ts, period);
            String modelType = config.getProperty("AD_MODEL");

            Class<?> tsModelClass = Class.forName("com.yahoo.egads.models.adm." + modelType);
            Constructor<?> constructor = tsModelClass.getConstructor(Properties.class);
            ad.addModel((AnomalyDetectionAbstractModel) constructor.newInstance(config));
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 异常检测器，需要的时序模型ts、周期、异常检测模型 参数已经全部放入模型
        return ad;
    }
}
