// main entry point for egads processing node

package com.yahoo.egads;

import java.util.Properties;
import java.io.FileInputStream;
import java.io.InputStream;
import com.yahoo.egads.utilities.*;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

/*
 * Call stack.
 * 
 * Anomaly Dtection Use Case (Assuming a trained model). 
 * Egads.Main()
 *   ProcessableObjectFactory.create
 *     Create AnomalyDetector
 *       BuildADModel()
 *     Create ModelAdapter
 *       BuildTSModel()
 *   po.process
 *     TODO: write to anomaly DB.
 * 
 */
@Slf4j
public class Egads {
    public static void main(String[] args) throws Exception {
        // 指定配置文件地址
        args[0] = "src/test/resources/sample_config.ini";

        if (args.length == 0) {
            System.err.println("Usage: java Egads config.ini (input [STDIN,CSV])");
            System.exit(1);
        }

        // TODO: This config will be retrieved from ConfigDB later,
        // for now it is assumed it's a static file.
        Properties properties = new Properties();
        // args数组中，第一个元素是配置文件，第二个元素是数据文件
        String config = args[0];
        log.debug("config:{}", config);
        File file = new File(config);
        boolean isRegularFile = file.exists();
        
        if (isRegularFile) {
            // 配置文件存在，则直接读取配置文件
            InputStream is = new FileInputStream(config);
            properties.load(is);
            // 从配置文件，中指定数据文件的路径
            args[1] = properties.getProperty("FILE_PATH");
        } else {
            // 配置文件不存在则通过key:val切分来构造配置文件
        	FileUtils.initProperties(config, properties);
        }

        // Set the input type.
        InputProcessor inputProcessor = null;
        if (properties.getProperty("INPUT") == null || properties.getProperty("INPUT").equals("CSV")) {
            // 从数据文件 读入 数据并处理
            inputProcessor = new FileInputProcessor(args[1]);
        } else {
            // 从控制台 读入 数据并处理
            inputProcessor = new StdinProcessor();
        }
        
        // Process the input the we received (either STDIN or as a file).
        // 处理数据并返回结果
        inputProcessor.processInput(properties);
    }
}
