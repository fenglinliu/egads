package com.yahoo.egads.utilities;

import com.yahoo.egads.data.TimeSeries;
import com.yahoo.egads.models.tsmm.TimeSeriesAbstractModel;

/**
 * StatisticsUtils
 *
 * @author Flynn
 * @version 1.0
 * @description 统计工具类
 * @email liufenglin@163.com
 * @date 2019/11/28
 */
public class StatisticsUtils {

    public static TimeSeries.DataSequence countDifferent(TimeSeries.DataSequence observedSeries, TimeSeries.DataSequence expectedSeries) {
        TimeSeries.DataSequence diffDataSequence = new TimeSeries.DataSequence();
        for (int i = 0; i < observedSeries.size(); i++) {
            TimeSeries.Entry observedEntry = observedSeries.get(i);
            TimeSeries.Entry expectedEntry = expectedSeries.get(i);
            if (observedEntry.time == expectedEntry.time && Math.abs(observedEntry.value - expectedEntry.value) > TimeSeriesAbstractModel.TOLERANCE) {
                diffDataSequence.add(observedEntry);
            }
        }
        return diffDataSequence;
    }
}
