package com.yahoo.egads.utilities;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.yahoo.egads.data.TimeSeries;
import com.yahoo.egads.models.tsmm.TimeSeriesAbstractModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.ml.clustering.Cluster;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * StatisticsUtils
 *
 * @author Flynn
 * @version 1.0
 * @description 统计工具类
 * @email liufenglin@163.com
 * @date 2019/11/28
 */
@Slf4j
public class StatisticsUtils {
    public static final int SWITCH = 0;

    // ec2_cpu_utilization_24ae8d_DEAL
    //BK.add("2014-02-26 13:45:00");
    //        BK.add("2014-02-27 06:25:00");
    //        BK.add("2014-02-27 08:55:00");
    //        BK.add("2014-02-28 01:35:00");
    //        BK.add("2014-02-26 22:05:00");
    //        BK.add("2014-02-27 17:15:00");

    public static final Set BK = Sets.newHashSet();

    static {
        BK.add("2014-02-26 13:45:00");
        BK.add("2014-02-27 06:25:00");
        BK.add("2014-02-27 08:55:00");
        BK.add("2014-02-28 01:35:00");
        BK.add("2014-02-26 22:05:00");
        BK.add("2014-02-27 17:15:00");
    }

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

    public static float getThreshold(Float[] array) {
        if (SWITCH == 0) { // kSigma模型
            float r = AutoSensitivity.getKSigmaSensitivity(array, 3/*聚类的标准差*/);
            return r;
        } else if (SWITCH == 1) { // DBSCAN模型
            // 将Array转为数据点
            List<IdentifiedDoublePoint> points = Lists.newArrayList();
            for (int i = 0; i < array.length; i++) {
                double[] temp = new double[1];
                temp[0] = array[i];
                IdentifiedDoublePoint point = new IdentifiedDoublePoint(temp, i);
                points.add(point);
            }
            DBSCANClusterer dbscanClusterer = new DBSCANClusterer(2/*聚类的领域半径*/, 2/*聚类的最小点*/);
            long start = System.currentTimeMillis();
            List<Cluster<IdentifiedDoublePoint>> clusters = dbscanClusterer.cluster_getClusters_MY(points, 0);
            Cluster<IdentifiedDoublePoint> anomalyCluster = (Cluster) dbscanClusterer.cluster_getClusters_MY(points, 1).get(0);
            log.info("DBSCAN聚类{}个点，cost:{}ms", points.size(), (System.currentTimeMillis() - start));

            List<Float> newArr = Lists.newArrayList();
            for (int i = 0; i < array.length; i++) {
                boolean isSame = false;
                for (int j = 0; j < anomalyCluster.getPoints().size(); j++) {
                    IdentifiedDoublePoint temP = anomalyCluster.getPoints().get(j);
                    double val = temP.getPoint()[0];
                    if (val == array[i]) {
                        isSame = true;
                        break;
                    }
                }
                if (!isSame) {
                    newArr.add(array[i]);
                }
            }
           Float[] nnnn = newArr.toArray(new Float[0]);
//            return Float.valueOf(String.valueOf(clusters.get(clusters.size() - 1).getPoints().get(0).getPoint()[0]));
            float r = AutoSensitivity.getKSigmaSensitivity(nnnn, 3/*聚类的标准差*/);
            return r;
        }
        return 0;
    }
}
