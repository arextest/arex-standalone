package io.arex.standalone.local.storage;

import io.arex.agent.bootstrap.model.ArexMocker;
import io.arex.agent.bootstrap.model.Mocker;
import io.arex.agent.bootstrap.util.CollectionUtil;
import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.foundation.config.ConfigManager;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.standalone.common.DiffMocker;
import io.arex.standalone.common.RecordModel;
import io.arex.standalone.local.util.PropertyUtil;
import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * H2 Storage Service
 */
public class H2StorageService {
    private static final Logger LOGGER = LoggerFactory.getLogger(H2StorageService.class);
    public static final H2StorageService INSTANCE = new H2StorageService();
    private static Statement stmt = null;

    public boolean start() throws Exception {
        Server webServer = null;
        Server tcpServer = null;
        Connection connection = null;
        try {
            if (StringUtil.isNotEmpty(PropertyUtil.getProperty("arex.storage.web.port"))) {
                webServer = Server.createWebServer("-webPort", PropertyUtil.getProperty("arex.storage.web.port"));
                webServer.start();
            }
            if (ConfigManager.INSTANCE.isEnableDebug()) {
                tcpServer = Server.createTcpServer("-ifNotExists", "-tcpAllowOthers");
                tcpServer.start();
            }
            JdbcConnectionPool cp = JdbcConnectionPool.create(PropertyUtil.getProperty("arex.storage.jdbc.url"),
                    PropertyUtil.getProperty("arex.storage.username"), PropertyUtil.getProperty("arex.storage.password"));
            connection = cp.getConnection();
            stmt = connection.createStatement();
            Map<String, String> schemaMap = H2SqlParser.parseSchema();
            for (String schema : schemaMap.values()) {
                stmt.execute(schema);
            }
            return true;
        } catch (Exception e) {
            if (webServer != null) {
                webServer.stop();
            }
            if (tcpServer != null) {
                tcpServer.stop();
            }
            try {
                if (connection != null) {
                    connection.close();
                }
                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException ex) {
            }
            LOGGER.warn("h2database start error", e);
        }
        return false;
    }

    public int save(Mocker mocker, String postJson) {
        String tableName = "MOCKER_INFO";
        List<Object> mockers = new ArrayList<>();
        mockers.add(mocker);
        return batchSave(mockers, tableName, postJson);
    }

    public int saveList(List<DiffMocker> mockers) {
        if (CollectionUtil.isEmpty(mockers)) {
            return 0;
        }
        String tableName = "DIFF_RESULT";
        return batchSave(new ArrayList<>(mockers), tableName, null);
    }

    public int batchSave(List<Object> mockers, String tableName, String jsonData) {
        int count = 0;
        try {
            String sql = H2SqlParser.generateInsertSql(mockers, tableName, jsonData);
            count = stmt.executeUpdate(sql);
        } catch (Throwable e) {
            LOGGER.warn("h2database batch save error: {}", e.getMessage(), e);
        }
        return count;
    }

    public Mocker query(Mocker mocker) {
        List<Mocker> result = queryList(mocker, 0);
        return CollectionUtil.isNotEmpty(result) ? result.get(0) : null;
    }

    public String queryJson(Mocker mocker) {
        String jsonData = "";
        try {
            String sql = H2SqlParser.generateSelectSql(mocker, 0);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                jsonData = URLDecoder.decode(rs.getString("jsonData"), StandardCharsets.UTF_8.name());
                if (StringUtil.isNotEmpty(jsonData)) {
                    return jsonData;
                }
            }
        } catch (Throwable e) {
            LOGGER.warn("h2database query mocker json error", e);
        }
        return jsonData;
    }

    public List<Mocker> queryList(Mocker mocker, int count) {
        List<Mocker> result = new ArrayList<>();
        try {
            String sql = H2SqlParser.generateSelectSql(mocker, count);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String jsonData = URLDecoder.decode(rs.getString("jsonData"), StandardCharsets.UTF_8.name());
                Mocker resultMocker = Serializer.deserialize(jsonData, ArexMocker.class);
                if (resultMocker == null) {
                    continue;
                }
                result.add(resultMocker);
            }
        } catch (Throwable e) {
            LOGGER.warn("h2database query mocker list error", e);
        }
        return result;
    }

    public List<DiffMocker> queryList(DiffMocker mocker) {
        List<DiffMocker> result = new ArrayList<>();
        try {
            String sql = H2SqlParser.generateSelectDiffSql(mocker);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                DiffMocker diffMocker = new DiffMocker();
                diffMocker.setReplayId(rs.getString("replayId"));
                diffMocker.setRecordId(rs.getString("recordId"));
                diffMocker.setCategoryType(mocker.getCategoryType());
                diffMocker.setRecordDiff(rs.getString("recordDiff"));
                diffMocker.setReplayDiff(rs.getString("replayDiff"));
                diffMocker.setOperationName(rs.getString("operationName"));
                result.add(diffMocker);
            }
        } catch (Throwable e) {
            LOGGER.warn("h2database query diff list error", e);
        }
        return result;
    }

    public List<RecordModel> queryRecordCount(Mocker mocker, int count) {
        List<RecordModel> result = new ArrayList<>();
        try {
            String sql = H2SqlParser.queryApiRecordCount(mocker, count);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                RecordModel recordModel = new RecordModel();
                recordModel.setIndex(rs.getString("index"));
                recordModel.setOperationName(rs.getString("operationName"));
                recordModel.setCaseNum(rs.getString("num"));
                result.add(recordModel);
            }
        } catch (Throwable e) {
            LOGGER.warn("h2database query record case error", e);
        }
        return result;
    }

    public List<RecordModel> queryApiRecordId(Mocker mocker, int count) {
        List<RecordModel> result = new ArrayList<>();
        try {
            String sql = H2SqlParser.queryApiRecordId(mocker, count);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                RecordModel recordModel = new RecordModel();
                recordModel.setRecordId(rs.getString("recordId"));
                recordModel.setMockCategoryType(rs.getString("mockCategory"));
                recordModel.setIndex(rs.getString("index"));
                result.add(recordModel);
            }
        } catch (Throwable e) {
            LOGGER.warn("h2database query record id error", e);
        }
        return result;
    }

    public List<RecordModel> queryJsonList(Mocker mocker, int count) {
        List<RecordModel> result = new ArrayList<>();
        try {
            String sql = H2SqlParser.generateSelectSql(mocker, count);
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                String jsonData = URLDecoder.decode(rs.getString("jsonData"), StandardCharsets.UTF_8.name());
                RecordModel recordModel = new RecordModel();
                recordModel.setMessage(jsonData);
                result.add(recordModel);
            }
        } catch (Throwable e) {
            LOGGER.warn("h2database query json list error", e);
        }
        return result;
    }
}