/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

package com.yahoo.egads.models.adm;

import java.util.Properties;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.json.JSONStringer;
import java.util.Map;
import java.util.HashMap;

import com.yahoo.egads.data.JsonEncoder;

@Slf4j
public abstract class AnomalyDetectionAbstractModel implements AnomalyDetectionModel {

    /**
     * 聚类的标准差，默认是3
     */
    protected float sDAutoSensitivity = 3;
    /**
     * 数据集中异常的期望百分比，比如，期待有百分之一的异常，默认值为百分之5
     */
    protected float amntAutoSensitivity = (float) 0.05;
    /**
     * 输出方式，如控制台，数据，GUI等
     */
    protected String outputDest = "";
    /**
     * 异常检测的模型名字
     */
	protected String modelName;

    public String getModelName() {
		return modelName;
	}

	public String getModelType() {
    	return "Anomaly";
    }
    
    @Override
    public void toJson(JSONStringer json_out) throws Exception {
        JsonEncoder.toJson(this, json_out);
    }

    @Override
    public void fromJson(JSONObject json_obj) throws Exception {
        JsonEncoder.fromJson(this, json_obj);
    }

    /**
     * 数组格式 转String ，用 : 隔开
     * @param input
     * @return
     */
    protected String arrayF2S (Float[] input) {
    	String ret = new String();
    	if (input.length == 0) {
    		return "";
    	}
    	if (input[0] == null) {
    		ret = "无";
    	} else {
    		ret = input[0].toString();
    	}
    	for (int ix = 1; ix < input.length; ix++) {
            if (input[ix] == null) {
                ret += ":无";
            } else {
    		    ret += ":" + input[ix].toString();
            }
    	}
    	return ret;
    }
    
    // Parses the THRESHOLD config into a map.
    protected Map<String, Float> parseMap(String s) {
        if (s == null) {
            return new HashMap<String, Float>();
        }
        // 配置文件格式是这样的：mape#10,mase#15
        String[] pairs = s.split(",");
        Map<String, Float> myMap = new HashMap<String, Float>();
        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            String[] keyValue = pair.split("#");
            myMap.put(keyValue[0], Float.valueOf(keyValue[1]));
        }
        // Map的组成：配置文件中的mape#10，切割为mape------》10
        return myMap;
    }

    // Force the user to define this constructor that acts as a
    // factory method.
    public AnomalyDetectionAbstractModel(Properties config) {
        // Set the assumed amount of anomaly in your data.
        if (config.getProperty("AUTO_SENSITIVITY_ANOMALY_PCNT") != null) {
            // 数据集中异常的期望百分比
            this.amntAutoSensitivity = new Float(config.getProperty("AUTO_SENSITIVITY_ANOMALY_PCNT"));
        }
        // Set the standard deviation for auto sensitivity.
        if (config.getProperty("AUTO_SENSITIVITY_SD") != null) {
            // 聚类的标准差
            this.sDAutoSensitivity = new Float(config.getProperty("AUTO_SENSITIVITY_SD"));
        }
      	this.outputDest = config.getProperty("OUTPUT");
    }

    @Override
    public boolean isDetectionWindowPoint(int maxHrsAgo, long windowStart, long anomalyTime, long startTime) {
        long unixTime = System.currentTimeMillis() / 1000L;
        // consider 'windowStart' if it is greater than or equal to first timestamp
        if (windowStart >= startTime) {
            return (anomalyTime - windowStart) > 0;
        } else {
            // use detection window as max hours specified
            return ((unixTime - anomalyTime) / 3600) < maxHrsAgo;
        }
    }
}
