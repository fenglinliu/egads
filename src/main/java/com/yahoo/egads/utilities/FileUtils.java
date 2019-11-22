/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

// A utility for creating an array of timeseries objects from the
// csv file.

package com.yahoo.egads.utilities;

import com.yahoo.egads.data.TimeSeries;
import lombok.extern.slf4j.Slf4j;

import java.util.StringTokenizer;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@Slf4j
public class FileUtils {
    
    // Creates a time-series from a file.
    public static ArrayList<TimeSeries> createTimeSeries(String csv_file, Properties config) {
        // Input file which needs to be parsed
        String fileToParse = csv_file;
        BufferedReader fileReader = null;
        // 从文件提取 raw data后的 Output，Output中的每一个TimeSeries，是由 raw data中的列构成的
        ArrayList<TimeSeries> output = new ArrayList<TimeSeries>();

        // Delimiter used in CSV file
        final String delimiter = ",";
        // 记录数据的采样时间间隔
        Long interval = null;
        // 时序数据的前一个时间
        Long prevTimestamp = null;
        Integer aggr = 1;
        // 是否填充丢失数据
        boolean fillMissing = false;
        if (config.getProperty("FILL_MISSING") != null && config.getProperty("FILL_MISSING").equals("1")) {
        	fillMissing = true;
        }
        if (config.getProperty("AGGREGATION") != null) {
            aggr = new Integer(config.getProperty("AGGREGATION"));
        }
        try {
            String line = "";
            // Create the file reader.
            log.debug("从{}处读取数据", fileToParse);
            fileReader = new BufferedReader(new FileReader(fileToParse));

            // Read the file line by line
            // 第一行数据是标题而不是数据，所以要进行特殊处理
            boolean firstLine = true;
            while ((line = fileReader.readLine()) != null) {
                log.debug("读取到的数据:{}", line);
                // Get all tokens available in line.
                String[] tokens = line.split(delimiter);
                // 时序数据的时间戳
                Long curTimestamp = null;
                
                // Check for the case where there is more than one line preceding the data
                // 跳过不是timestamp的行
                if (firstLine == true) {
                    if (!isNumeric(tokens[0]) && tokens[0].equals("timestamp") == false) {
                        continue;
                    }
                }
                if (firstLine == false && tokens.length > 1) {
                    curTimestamp = (new Double(tokens[0])).longValue();
                }
                // 获取每一行数据的第二列，及以后的列
                for (int i = 1; i < tokens.length; i++) {
                    // Assume that the first line contains the column names.
                    if (firstLine) { // 首行处理方式
                        // 初始化时序对象，设置文件名
                        TimeSeries ts = new TimeSeries();
                        ts.meta.fileName = csv_file;
                        // 输入数据的每一个属性列会被归结为一个TimeSeries
                        output.add(ts);
                        // 如果是列头，就设置属性名字
                        if (isNumeric(tokens[i]) == false) { // Just in case there's a numeric column heading
                            ts.meta.name = tokens[i];
                        } else {
                            // 如果数据列没有名字，那么系统来自定义名字
                            ts.meta.name = "metric_" + i;
                            // 如果不是列头，就对当前的ts对象设置数据
                            output.get(i - 1).append((new Double(tokens[0])).longValue(),
                                    new Float(tokens[i]));
                        }
                    } else { // 其他行处理方式
                        // A naive missing data handler. 数据缺失控制器，仅在fillMissing参数为true时，有效
                        if (interval != null && prevTimestamp != null && interval > 0 && fillMissing == true) {
                            if ((curTimestamp - prevTimestamp) != interval) { // 当前时间和之前一个时间的时间间隔不一致，则进行数据填充
                                // 计算缺失的时间段
                                int missingValues = (int) ((curTimestamp - prevTimestamp) / interval);
                                
                                Long curTimestampToFill = prevTimestamp + interval;
                                for (int j = (missingValues - 1); j > 0; j--) { // 填充缺失时间段内的数据
                                    Float valToFill =  new Float(tokens[i]); // 缺失值和现有值保持一致 或 把挨着缺失时间段的现有时间段的值向前移动来填补
                                    if (output.get(i - 1).size() >= missingValues) {
                                        valToFill = output.get(i - 1).data.get(output.get(i - 1).size() - missingValues).value;
                                    }
                                    // 填充missing数据
                                    output.get(i - 1).append(curTimestampToFill, valToFill);
                                    curTimestampToFill += interval;
                                }
                            }
                        }
                        // Infer interval.
                        if (interval == null && prevTimestamp != null) {
                            interval = curTimestamp - new Long(prevTimestamp);
                        }

                        // 对当前的ts对象设置数据
                        output.get(i - 1).append(curTimestamp,
                                new Float(tokens[i]));
                    }
                }
                if (firstLine == false) {
                    prevTimestamp = curTimestamp;
                }
                firstLine = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Handle aggregation.
        if (aggr > 1) {
            for (TimeSeries t : output) {
                t.data = t.aggregate(aggr);
                t.meta.name += "_aggr_" + aggr;
            }
        }
        return output;
    }
        
    // Checks if the string is numeric.
    public static boolean isNumeric(String str) {  
        try {  
            Double.parseDouble(str);  
        } catch (NumberFormatException nfe) {  
            return false;  
        }  
        return true;  
    }
    
    // Parses the string array property into an integer property.
    public static int[] splitInts(String str) throws IllegalArgumentException {
        StringTokenizer tokenizer = new StringTokenizer(str, ",");
        int n = tokenizer.countTokens();
        int[] list = new int[n];
        for (int i = 0; i < n; i++) {
          String token = tokenizer.nextToken();
          list[i] = Integer.parseInt(token);
        }
        return list;
      }
    
    // Initializes properties from a string (key:value, separated by ";").
    public static void initProperties(String config, Properties properties) {
    	String delims1 = ";";
    	String delims2 = ":";
 
		StringTokenizer st1 = new StringTokenizer(config, delims1);
		while (st1.hasMoreElements()) {
			String[] st2 = (st1.nextToken()).split(delims2);
			properties.setProperty(st2[0], st2[1]);
		}
    }
}
