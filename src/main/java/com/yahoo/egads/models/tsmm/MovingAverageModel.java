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
    
    public void train(TimeSeries.DataSequence data) {
        this.data = data;
        int n = data.size();
        DataPoint dataPoint = null;
        DataSet observedData = new DataSet();
        for (int i = 0; i < n; i++) {
            dataPoint = new Observation(data.get(i).value);
            dataPoint.setIndependentValue("x", i);
            observedData.add(dataPoint);
        }
        observedData.setTimeVariable("x"); 
        
        // TODO: Make window configurable.
        // TODO 代码改进，周期应该可以传进来
        forecaster = new net.sourceforge.openforecast.models.MovingAverageModel(2);
        forecaster.init(observedData);
        initForecastErrors(forecaster, data);
        
        log.info("bias: " + getBias() + "\t" + "mad: " + getMAD() + "\t" + "mape: " + getMAPE() + "\t" + "mse: " + getMSE() + "\t" + "sae: " + getSAE() + "\t" + 0 + "\t" + 0);
    }
  
    public void update(TimeSeries.DataSequence data) {

    }

    public String getModelName() {
        return modelName;
    }

    public void predict(TimeSeries.DataSequence sequence) throws Exception {
          int n = data.size();
          DataSet requiredDataPoints = new DataSet();
          DataPoint dp;

          for (int count = 0; count < n; count++) {
              dp = new Observation(0.0);
              dp.setIndependentValue("x", count);
              requiredDataPoints.add(dp);
          }
          forecaster.forecast(requiredDataPoints);

          // Output the results
          Iterator<DataPoint> it = requiredDataPoints.iterator();
          int i = 0;
          while (it.hasNext()) {
              DataPoint pnt = ((DataPoint) it.next());
              logger.info(data.get(i).time + "," + data.get(i).value + "," + pnt.getDependentValue());
              sequence.set(i, (new Entry(data.get(i).time, (float) pnt.getDependentValue())));
              i++;
          }
    }

    public void toJson(JSONStringer json_out) {

    }

    public void fromJson(JSONObject json_obj) {

    }
}
