package com.caojing;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;


/**
 * 多数据源Jdbc工具
 *
 * @author CaoJing
 * @date 2019/10/20 16:18
 */
@Slf4j
public final class JdbcUtil {

    /**
     * 多数据源
     */
    public enum DataSource {

        /**
         * 推客dev数据源
         */
        PUSHER_DEV(
            "jdbc:mysql://rm-2ze2433x22ce09k6f.mysql.rds.aliyuncs.com/pusher_dev",
            "pusher_dev",
            "2VtfYJ36SeMeZckq"
        ),

        /**
         * 推客test数据源
         */
        PUSHER_TEST(
            "jdbc:mysql://rm-2ze2433x22ce09k6f.mysql.rds.aliyuncs.com/pusher_test",
            "pusher_test",
            "DgCOM2GJNzWLGZJN"
        );

        private DruidDataSource dataSource;

        DataSource(String url, String username, String password) {
            this.dataSource = initialDataSource(url, username, password);
        }

        public DruidPooledConnection getConnection() throws SQLException {
            return this.dataSource.getConnection();
        }
    }

    /**
     * 根据环境变量获取DataSource
     */
    public static DataSource getByEnv(String env) {
        if ("dev".equals(env)) {
            return DataSource.PUSHER_DEV;
        } else if ("test".equals(env)) {
            return DataSource.PUSHER_TEST;
        }
        return DataSource.PUSHER_DEV;
    }

    /**
     * 初始化数据库连接池
     */
    private static synchronized DruidDataSource initialDataSource(String url, String username, String password) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(5);
        dataSource.setMaxWait(60000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1 FROM DUAL");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(true);
        dataSource.setTestOnReturn(true);
        return dataSource;
    }

    /**
     * 执行查询
     *
     * @param connection 数据库连接
     * @param sql        要执行的sql语句
     * @param bindArgs   绑定的参数
     * @return List<Map < String, Object>>结果集对象
     * @throws Exception 异常
     */
    public static List<Map<String, Object>> executeQuery(Connection connection, String sql, Object[] bindArgs) throws Exception {

        List<Map<String, Object>> datas;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(sql);
            if (bindArgs != null) {
                // 设置sql占位符中的值
                for (int i = 0; i < bindArgs.length; i++) {
                    preparedStatement.setObject(i + 1, bindArgs[i]);
                }
            }
            log.info(getExecSql(sql, bindArgs));
            // 执行sql语句，获取结果集
            resultSet = preparedStatement.executeQuery();
            datas = getDatas(resultSet);

        } catch (Exception e) {
            log.error("查询异常", e);
            throw e;
        } finally {
            if (resultSet != null) {
                resultSet.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return datas;
    }

    /**
     * 插入
     *
     * @param connection 数据库连接
     * @param valueMap   插入数据表中key为列名和value为列对应的值的Map对象
     * @param tableName  要插入的数据库的表名
     * @return 影响的行数
     * @throws Exception 异常
     */
    public static int insertSelective(Connection connection, String tableName, Map<String, Object> valueMap) throws Exception {
        // 获取数据库插入的Map的键值对的值
        Iterator<String> iterator = valueMap.keySet().iterator();
        // 要插入的字段sql，其实就是用key拼起来的
        StringBuilder columnSql = new StringBuilder();
        // 要插入的字段值，其实就是
        StringBuilder unknownMarkSql = new StringBuilder();
        Object[] bindArgs = new Object[valueMap.size()];
        int i = 0;
        while (iterator.hasNext()) {
            String key = iterator.next();
            columnSql.append(i == 0 ? "" : ",");
            columnSql.append(key);

            unknownMarkSql.append(i == 0 ? "" : ",");
            unknownMarkSql.append("?");
            bindArgs[i] = valueMap.get(key);
            i++;
        }
        // 开始拼插入的sql语句
        String sql = "INSERT INTO " + tableName + " (" + columnSql + " )  VALUES (" + unknownMarkSql + " )";
        return executeUpdate(connection, sql, bindArgs);
    }

    /**
     * 批量插入
     *
     * @param connection 数据库连接
     * @param datas      插入数据表中key为列名和value为列对应的值的Map对象的List集合
     * @param tableName  要插入的数据库的表名
     * @return 影响的行数
     * @throws Exception 异常
     */
    public static int batchInsert(Connection connection, String tableName, List<Map<String, Object>> datas) throws Exception {
        // 影响的行数
        int affectRowCount;
        PreparedStatement preparedStatement = null;
        try {
            Map<String, Object> valueMap = datas.get(0);
            // 获取数据库插入的Map的键值对的值
            Iterator<String> iterator = valueMap.keySet().iterator();
            // 要插入的字段sql，其实就是用key拼起来的
            StringBuilder columnSql = new StringBuilder();
            // 要插入的字段值，其实就是？
            StringBuilder unknownMarkSql = new StringBuilder();
            Object[] keys = new Object[valueMap.size()];
            int i = 0;
            while (iterator.hasNext()) {
                String key = iterator.next();
                keys[i] = key;
                columnSql.append(i == 0 ? "" : ",");
                columnSql.append(key);

                unknownMarkSql.append(i == 0 ? "" : ",");
                unknownMarkSql.append("?");
                i++;
            }
            // 开始拼插入的sql语句
            String sql = "INSERT INTO " + tableName + " (" + columnSql + " )  VALUES (" + unknownMarkSql + " )";

            // 执行SQL预编译
            preparedStatement = connection.prepareStatement(sql);
            // 设置不自动提交，以便于在出现异常的时候数据库回滚
            connection.setAutoCommit(false);

            log.info(sql);

            for (Map<String, Object> data : datas) {
                for (int k = 0; k < keys.length; k++) {
                    preparedStatement.setObject(k + 1, data.get(keys[k]));
                }
                preparedStatement.addBatch();
            }
            int[] arr = preparedStatement.executeBatch();
            connection.commit();
            affectRowCount = arr.length;
            log.info("插入" + affectRowCount + "行");
        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
            }
            log.error("插入异常", e);
            throw e;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return affectRowCount;
    }

    /**
     * 将结果集对象封装成List<Map<String, Object>> 对象
     *
     * @param resultSet 结果多想
     * @return 结果的封装
     * @throws SQLException 异常
     */
    private static List<Map<String, Object>> getDatas(ResultSet resultSet) throws SQLException {
        List<Map<String, Object>> datas = new ArrayList<>();
        // 获取结果集的数据结构对象
        ResultSetMetaData metaData = resultSet.getMetaData();
        while (resultSet.next()) {
            Map<String, Object> rowMap = new HashMap<>(metaData.getColumnCount());
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                rowMap.put(metaData.getColumnName(i), resultSet.getObject(i));
            }
            datas.add(rowMap);
        }

        log.info("查询到" + datas.size() + "行数据");

        for (int i = 0; i < datas.size(); i++) {
            Map<String, Object> map = datas.get(i);
            log.info("第" + (i + 1) + "行：" + map);
        }
        return datas;
    }

    /**
     * 可以执行新增，修改，删除
     *
     * @param connection 数据库连接
     * @param sql        sql语句
     * @param bindArgs   绑定参数
     * @return 影响的行数
     * @throws SQLException SQL异常
     */
    private static int executeUpdate(Connection connection, String sql, Object[] bindArgs) throws Exception {
        // 影响的行数
        int affectRowCount;
        PreparedStatement preparedStatement = null;
        try {
            // 执行SQL预编译
            preparedStatement = connection.prepareStatement(sql);
            // 设置不自动提交，以便于在出现异常的时候数据库回滚
            connection.setAutoCommit(false);

            log.info(getExecSql(sql, bindArgs));

            if (bindArgs != null) {
                // 绑定参数设置sql占位符中的值
                for (int i = 0; i < bindArgs.length; i++) {
                    preparedStatement.setObject(i + 1, bindArgs[i]);
                }
            }

            // 执行sql
            affectRowCount = preparedStatement.executeUpdate();
            connection.commit();
            String operate;
            if (sql.toUpperCase().contains("DELETE FROM")) {
                operate = "删除";
            } else if (sql.toUpperCase().contains("INSERT INTO")) {
                operate = "新增";
            } else {
                operate = "修改";
            }
            log.info(operate + affectRowCount + "行");
        } catch (Exception e) {
            if (connection != null) {
                connection.rollback();
            }
            log.error("数据库执行异常", e);
            throw e;
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (connection != null) {
                connection.close();
            }
        }
        return affectRowCount;
    }

    /**
     * 绑定参数得到最终执行sql
     *
     * @param sql      SQL statement
     * @param bindArgs Binding parameters
     * @return 最终执行sql
     */
    private static String getExecSql(String sql, Object[] bindArgs) {
        StringBuilder sb = new StringBuilder(sql);
        if (bindArgs != null && bindArgs.length > 0) {
            int index = 0;
            for (Object bindArg : bindArgs) {
                index = sb.indexOf("?", index);
                sb.replace(index, index + 1, String.valueOf(bindArg));
            }
        }
        return sb.toString();
    }
}
