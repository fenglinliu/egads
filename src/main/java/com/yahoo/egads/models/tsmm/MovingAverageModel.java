/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// Olympic scoring model considers the average of the last k weeks
// (dropping the b highest and lowest values) as the current prediction.

package com.yahoo.egads.models.tsmm;

import com.yahoo.egads.data.*;
import com.yahoo.egads.data.TimeSeries.Entry;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.json.JSONStringer;
import java.util.Properties;
import net.sourceforge.openforecast.DataSet;
import net.sourceforge.openforecast.ForecastingModel;
import net.sourceforge.openforecast.DataPoint;
import net.sourceforge.openforecast.Observation;
import java.util.*;

// A moving average forecast model is based on an artificially constructed time series in which the value for a
// given time period is replaced by the mean of that value and the values for some number of preceding and succeeding time periods.
@Slf4j
public class MovingAverageModel extends TimeSeriesAbstractModel {
    // methods ////////////////////////////////////////////////

    // The model that will be used for forecasting.
    // 这个参数在train函数中初始化，设置的预测模型就是自己，MovingAverageModel
    // 不过只是名字一样而已，真正的类是，开源实现的类
    private ForecastingModel forecaster;
    
    // Stores the historical values.
    private TimeSeries.DataSequence data;

    public MovingAverageModel(Properties config) {
        super(config);
        modelName = "MovingAverageModel";
    }

    public void reset() {
        // At this point, reset does nothing.
    }
    
    public void train(TimeSeries.DataSequence originalData) {
        this.data = originalData;
        int n = originalData.size();
        DataPoint dataPoint = null;
        // 观测值
        DataSet observedData = new DataSet();
        for (int i = 0; i < n; i++) {
            dataPoint = new Observation(originalData.get(i).value);
            dataPoint.setIndependentValue("x", i);
            observedData.add(dataPoint);
        }
        observedData.setTimeVariable("x"); 
        
        // TODO: Make window configurable.
        // TODO 代码改进，周期应该可以传进来
        forecaster = new net.sourceforge.openforecast.models.MovingAverageModel(originalData.size());
        forecaster.init(observedData);
        initForecastErrors(forecaster, originalData);
        
        log.info("bias: " + getBias() + "\t" + "mad: " + getMAD() + "\t" + "mape: " + getMAPE() + "\t" + "mse: " + getMSE() + "\t" + "sae: " + getSAE() + "\t" + 0 + "\t" + 0);
    }
  
    public void update(TimeSeries.DataSequence data) {

    }

    public String getModelName() {
        return modelName;
    }

    public void predict(TimeSeries.DataSequence sequence) throws Exception {
          int n = data.size();

          // requiredDataPoints存储由历史数据构造出的 DataPoint
          DataSet requiredDataPoints = new DataSet();
          DataPoint dp;

          for (int count = 0; count < n; count++) {
              /**
               * Observation 是 DataPoint的实现
               * 属性有：
               * dependentValue  因变量
               * independentValues 自变量，k-v类型，k是自变量的名字
               * */
              // 按照历史数据的大小来 初始化同样大小的数据点，初始的时候设置因变量是0.0
              dp = new Observation(0.0);
              // 初始的时候设置自变量名为x， 自变量值为是数据点的索引
              dp.setIndependentValue("x", count);
              requiredDataPoints.add(dp);
          }
          // FIXME 重写预测数据生成代码
          forecaster.forecast(requiredDataPoints);

          // Output the results
          Iterator<DataPoint> it = requiredDataPoints.iterator();
          int i = 0;
          while (it.hasNext()) {
              DataPoint pnt = ((DataPoint) it.next());
              log.info(">>>>>预测 >>  " + "time: " + data.get(i).time + "," + "value: " + data.get(i).value + "," + "predict val: " + pnt.getDependentValue());
              sequence.set(i, (new Entry(data.get(i).time, (float) pnt.getDependentValue())));
              i++;
          }
    }

    public void toJson(JSONStringer json_out) {

    }

    public void fromJson(JSONObject json_obj) {

    }
}
