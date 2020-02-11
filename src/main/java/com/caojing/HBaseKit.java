package com.caojing;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.*;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * HBase工具
 */
@Slf4j
public class HBaseKit {

    public enum DB {

        /**
         * 连接1
         */
        DB_1(
            "ds1:2181,ds2:2181,ds3:2181"
        );

        private Connection connection;

        DB(String quorum) {
            org.apache.hadoop.conf.Configuration conf = HBaseConfiguration.create();
            conf.set(HConstants.ZOOKEEPER_QUORUM, quorum);
            try {
                this.connection = ConnectionFactory.createConnection(conf);
            } catch (IOException e) {
                log.error("", e);
            }
        }
    }

    /**
     * 通过rowKey查询
     */
    public static Map<String, String> findOne(String tableName, String rowKey) throws IOException {
        Map<String, String> resultMap;
        try (Table table = DB.DB_1.connection.getTable(TableName.valueOf(tableName))) {
            if (table == null) {
                return null;
            }
            Get get = new Get(rowKey.getBytes());
            Result result = table.get(get);
            resultMap = new HashMap<>(16);
            for (Cell cell : result.rawCells()) {
                resultMap.put(Bytes.toString(CellUtil.cloneQualifier(cell)), Bytes.toString(CellUtil.cloneValue(cell)));
            }
        }
        return resultMap;
    }

    /**
     * 根据rowKey删除指定的行
     */
    public static void deleteRow(String tableName, String rowKey) throws IOException {
        try (Table table = DB.DB_1.connection.getTable(TableName.valueOf(tableName))) {
            Delete delete = new Delete(Bytes.toBytes(rowKey));
            table.delete(delete);
        }
    }

}
