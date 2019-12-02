/* ===========================================================
 * JFreeChart : a free chart library for the Java(tm) platform
 * ===========================================================
 *
 * (C) Copyright 2000-2014, by Object Refinery Limited and Contributors.
 *
 * Project Info:  http://www.jfree.org/jfreechart/index.html
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * [Oracle and Java are registered trademarks of Oracle and/or its affiliates. 
 * Other names may be trademarks of their respective owners.]
 *
 *
 */

package com.yahoo.egads.utilities;

import lombok.extern.slf4j.Slf4j;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.ApplicationFrame;
import com.yahoo.egads.data.TimeSeries.DataSequence;
import com.yahoo.egads.data.Anomaly;
import java.util.ArrayList;
import com.yahoo.egads.data.Anomaly.IntervalSequence;
import com.yahoo.egads.data.Anomaly.Interval;
import java.awt.Color;
import java.util.HashMap;
import com.yahoo.egads.data.AnomalyErrorStorage;
import java.util.Properties;
import javax.swing.JFrame;

// Draws the time-series.
@Slf4j
public class GUIUtils extends ApplicationFrame {
	private static final long serialVersionUID = 1L;
	// Denominator used in the MASE error metric.
    float maseDenom = 0;
    private AnomalyErrorStorage anomalyErrorStorage = new AnomalyErrorStorage();
    private Properties config;

    private GUIUtils(String title, DataSequence orig, DataSequence predicted, ArrayList<Anomaly> anomalyList, Properties config) {
         super(title);
         this.config = config;
         // 绘制图案
         final JFreeChart chart = createCombinedChart(orig, predicted, anomalyList);
         final ChartPanel panel = new ChartPanel(chart, true, true, true, false, true);
         panel.setPreferredSize(new java.awt.Dimension(1440, 900));
         setContentPane(panel);
    }
    
    /**
     * Creates a combined chart.
     * @param tsOne 原始数据
     * @param tsTwo 预测数据
     * @param anomalyList 异常值
     * @return The combined chart.
     */
    private JFreeChart createCombinedChart(DataSequence tsOne, DataSequence tsTwo, ArrayList<Anomaly> anomalyList) {

        // create subplot 1.
        final XYDataset data1 = createDataset(tsOne, "观测数据");
        final XYItemRenderer renderer1 = new StandardXYItemRenderer();
        final NumberAxis rangeAxis1 = new NumberAxis("观测数据值");
        XYPlot subplot1 = new XYPlot(data1, null, rangeAxis1, renderer1);
        subplot1.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
        
        // plot anomalies on subplot 1.
        addAnomalies(subplot1, anomalyList);
        
        // create subplot 2.
        final XYDataset data2 = createDataset(tsTwo, "预测数据");
        final XYItemRenderer renderer2 = new StandardXYItemRenderer();
        final NumberAxis rangeAxis2 = new NumberAxis("预测数据值");
        rangeAxis2.setAutoRangeIncludesZero(false);
        final XYPlot subplot2 = new XYPlot(data2, null, rangeAxis2, renderer2);
        subplot2.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);

        // parent plot.
        final CombinedDomainXYPlot plot = new CombinedDomainXYPlot(new NumberAxis("时间"));
        plot.setGap(10.0);
        
        // add the subplots.
        plot.add(subplot1, 1);
        plot.add(subplot2, 1);
        
        // Add anomaly score time-series. 计算异常得分
//        addAnomalyTS(plot, tsOne, tsTwo);
        
        plot.setOrientation(PlotOrientation.VERTICAL);

        // return a new chart containing the overlaid plot.
        return new JFreeChart("异常检测",
                              JFreeChart.DEFAULT_TITLE_FONT,
                              plot,
                              true);
    }
    
    /**
     * Compute the time-series of anomalies.
     */
    public void addAnomalyTS(CombinedDomainXYPlot plot, DataSequence observedSeries, DataSequence expectedSeries) {
        // Compute the time-series of errors.
        HashMap<String, ArrayList<Float>> allErrors = anomalyErrorStorage.initAnomalyErrors(observedSeries, expectedSeries);
        Float sDAutoSensitivity = (float) 0.0;
        Float amntAutoSensitivity = (float) 0.0;
        // Denotes the expected % of anomalies
        if (config.getProperty("AUTO_SENSITIVITY_ANOMALY_PCNT") != null) {
          amntAutoSensitivity = new Float(config.getProperty("AUTO_SENSITIVITY_ANOMALY_PCNT"));
        }
        // Refers to the cluster standard deviation.
        if (config.getProperty("AUTO_SENSITIVITY_SD") != null) {
          sDAutoSensitivity = new Float(config.getProperty("AUTO_SENSITIVITY_SD"));
        }

        String errorDebug = "";
        for (int i = 0; i < (anomalyErrorStorage.getIndexToError().keySet()).size(); i++) {
            Float[] fArray = (allErrors.get(anomalyErrorStorage.getIndexToError().get(i))).toArray(new Float[(allErrors.get(anomalyErrorStorage.getIndexToError().get(i))).size()]);
            XYDataset data1 = createDataset(fArray, anomalyErrorStorage.getIndexToError().get(i));
            XYItemRenderer renderer1 = new StandardXYItemRenderer();
            NumberAxis rangeAxis1 = new NumberAxis(anomalyErrorStorage.getIndexToError().get(i));
            XYPlot subplot1 = new XYPlot(data1, null, rangeAxis1, renderer1);
            // Get threshold.
            Float d = AutoSensitivity.getLowDensitySensitivity(fArray, sDAutoSensitivity, amntAutoSensitivity);
            subplot1.addRangeMarker(new ValueMarker(d));
            subplot1.setRangeAxisLocation(AxisLocation.BOTTOM_OR_LEFT);
            plot.add(subplot1, 1);
            errorDebug += anomalyErrorStorage.getIndexToError().get(i) + ": " + d + " ";
        }
        log.info(errorDebug);
    }
    
    /**
     * Add anomalies to the plot.
     */
    public void addAnomalies(XYPlot plot, ArrayList<Anomaly> anomalyList) {
        for (Anomaly a : anomalyList) {
            IntervalSequence is = a.intervals;
            for (Interval i : is) {
            	ValueMarker marker = new ValueMarker(i.index);
                marker.setPaint(Color.black);
                plot.addDomainMarker(marker);
            }
        }
    }
    
    /**
     * Creates a float[] time-series
     */
    public XYDataset createDataset(Float[] ds, String label) {
         XYSeries observations = new XYSeries(label);
         int n = ds.length;
         for (int i = 0; i < n; i++) {
             observations.add(i, ds[i]);
         }        
         XYSeriesCollection collection = new XYSeriesCollection();
         collection.addSeries(observations);
         return collection;
    }
    
    /**
     * Creates a dataset.
     * @return the dataset.
     */
    public XYDataset createDataset(DataSequence ds, String label) {
        XYSeries observations = new XYSeries(label);
        int n = ds.size();
        for (int i = 0; i < n; i++) {
            observations.add(i, ds.get(i).value);
        }        
        XYSeriesCollection collection = new XYSeriesCollection();
        collection.addSeries(observations);
        return collection;
    }

    /**
     * @param orig 原始数据
     * @param predicted 预测数据
     * @param anomalyList 一个属性对应的异常点，多个属性则构成List集合
     * @param config 配置文件
     */
    public static void plotResults(DataSequence orig, DataSequence predicted, ArrayList<Anomaly> anomalyList, Properties config) {
        GUIUtils gui = new GUIUtils("GUI", orig, predicted, anomalyList, config);
        gui.pack();
        gui.setVisible(true);
        JFrame frame = new JFrame("GUI");
//        JOptionPane.showMessageDialog(frame, "Click OK to continue");
//        gui.setVisible(false);
    }
}
