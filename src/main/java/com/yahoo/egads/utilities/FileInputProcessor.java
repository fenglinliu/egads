/*
 * Copyright 2015, Yahoo Inc.
 * Copyrights licensed under the GPL License.
 * See the accompanying LICENSE file for terms.
 */

package com.yahoo.egads.utilities;

// Class that implements EGADS file input processing.

import com.yahoo.egads.control.ProcessableObject;
import com.yahoo.egads.control.ProcessableObjectFactory;
import java.util.Properties;
import com.yahoo.egads.data.TimeSeries;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public class FileInputProcessor implements InputProcessor {
    
    private String file = null;
    
    public FileInputProcessor(String file) {
        this.file = file;
    }

    /**
     * 从文件中读入数据，并返回结果
     *
     * @param p
     * @throws Exception
     */
    public void processInput(Properties p) throws Exception {
        // 建立时序模型(time, value)，多个属性会有多个时序模型放入到List中
        ArrayList<TimeSeries> metrics = com.yahoo.egads.utilities.FileUtils
                .createTimeSeries(this.file, p);
        // 循环处理每一个时间序列
        for (TimeSeries ts : metrics) {
            ProcessableObject po = ProcessableObjectFactory.create(ts, p);
            po.process();
        }
    }
}
