package io.arex.standalone.local.storage;

import io.arex.agent.bootstrap.model.Mocker;
import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.foundation.util.IOUtils;
import io.arex.standalone.local.model.DiffMocker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class H2SqlParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(H2SqlParser.class);

    private static Map<String, String> schemaMap = new HashMap<>();

    public static Map<String, String> parseSchema() {
        try {
            String schemaSql = IOUtils.toString(
                    H2StorageService.class.getClassLoader().getResourceAsStream("db/h2/schema.txt"));
            String[] schemaArray = schemaSql.split("--");
            for (String schemas : schemaArray) {
                if (StringUtils.isBlank(schemas)) {
                    continue;
                }
                String[] sqlArray = schemas.split("\n");
                String tableName = "";
                StringBuilder schema = new StringBuilder();
                for (String sql : sqlArray) {
                    if (StringUtils.isBlank(sql)) {
                        continue;
                    }
                    if (sql.startsWith("CREATE TABLE")) {
                        tableName = StringUtils.substringBetween(sql, "EXISTS ", "(");
                    }
                    schema.append(sql);
                    if (sql.equals(");")) {
                        break;
                    } else {
                        schema.append("\n");
                    }
                }
                schemaMap.put(tableName, schema.toString());
            }
        } catch (Throwable e) {
            LOGGER.warn("h2database parseSchema error", e);
        }
        return schemaMap;
    }

    public static String generateInsertSql(List<Object> entitys, String tableName, String jsonData) {
        StringBuilder sqlBuilder = new StringBuilder("INSERT INTO ").append(tableName).append(" VALUES");
        try {
            for (Object entity : entitys) {
                sqlBuilder.append("(DEFAULT,CURRENT_TIMESTAMP,");
                if (entity instanceof Mocker) {
                    Mocker mocker = (Mocker)entity;
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getRecordId())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getReplayId())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getCategoryType().getName())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getAppId())).append("',");
                    sqlBuilder.append("'").append(jsonData == null ? "" : URLEncoder.encode(jsonData, StandardCharsets.UTF_8.name())).append("',");
                    sqlBuilder.append(mocker.getCreationTime()).append(",");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getOperationName())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getTargetRequest().getBody())).append("'");
                } else if (entity instanceof DiffMocker) {
                    DiffMocker mocker = (DiffMocker)entity;
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getRecordId())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getReplayId())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getCategoryType())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getRecordDiff())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getReplayDiff())).append("',");
                    sqlBuilder.append("'").append(StringUtil.defaultString(mocker.getOperationName())).append("'");
                }
                sqlBuilder.append("),");
            }
        } catch (Throwable e) {
            LOGGER.warn("h2database generateInsertSql error", e);
        }
        return sqlBuilder.substring(0, sqlBuilder.length()-1);
    }

    public static String generateSelectSql(Mocker mocker, int count) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM MOCKER_INFO");
        sqlBuilder.append(" WHERE 1 = 1");
        if (StringUtils.isNotBlank(mocker.getRecordId())) {
            sqlBuilder.append(" AND RECORDID = '").append(mocker.getRecordId()).append("'");
        }
        if (StringUtils.isNotBlank(mocker.getReplayId())) {
            sqlBuilder.append(" AND REPLAYID = '").append(mocker.getReplayId()).append("'");
        } else {
            sqlBuilder.append(" AND REPLAYID = ''");
        }
        if (mocker.getCategoryType() != null) {
            sqlBuilder.append(" AND CATEGORYTYPE = '").append(mocker.getCategoryType().getName()).append("'");
        }
        if (StringUtils.isNotBlank(mocker.getOperationName())) {
            sqlBuilder.append(" AND OPERATIONNAME = '").append(mocker.getOperationName()).append("'");
        }
        if (mocker.getTargetRequest() != null && StringUtils.isNotBlank(mocker.getTargetRequest().getBody())) {
            sqlBuilder.append(" AND REQUEST = '").append(mocker.getTargetRequest().getBody()).append("'");
        }
        sqlBuilder.append(" ORDER BY CREATIONTIME DESC");
        if (count > 0) {
            sqlBuilder.append(" LIMIT ").append(count);
        }
        return sqlBuilder.toString();
    }

    public static String generateSelectDiffSql(DiffMocker mocker, int count) {
        StringBuilder sqlBuilder = new StringBuilder("SELECT * FROM DIFF_RESULT WHERE 1 = 1");
        if (mocker.getCategoryType() != null) {
            sqlBuilder.append(" AND CATEGORYTYPE = '").append(mocker.getCategoryType()).append("'");
        }
        if (StringUtils.isNotBlank(mocker.getRecordId())) {
            sqlBuilder.append(" AND RECORDID = '").append(mocker.getRecordId()).append("'");
        }
        if (StringUtils.isNotBlank(mocker.getReplayId())) {
            sqlBuilder.append(" AND REPLAYID = '").append(mocker.getReplayId()).append("'");
        }
        if (count > 0) {
            sqlBuilder.append(" LIMIT ").append(count);
        }
        return sqlBuilder.toString();
    }

    public static String queryApiRecordCount(Mocker mocker, int count) {
        StringBuilder sqlBuilder = new StringBuilder("select rownum() as index, * from (");
        sqlBuilder.append(" SELECT OPERATIONNAME, count(1) as NUM FROM MOCKER_INFO WHERE REPLAYID = ''");
        if (StringUtils.isNotBlank(mocker.getAppId())) {
            sqlBuilder.append(" AND APPID = '").append(mocker.getAppId()).append("'");
        }
        sqlBuilder.append(" AND CATEGORYTYPE = 'Servlet'");
        sqlBuilder.append(" GROUP BY OPERATIONNAME");
        sqlBuilder.append(" ORDER BY NUM DESC )");
        if (count > 0) {
            sqlBuilder.append(" LIMIT ").append(count);
        }
        return sqlBuilder.toString();
    }

    public static String queryApiRecordId(Mocker mocker, int count) {
        StringBuilder sqlBuilder = new StringBuilder("select rownum() as index, * from (");
        sqlBuilder.append(" select recordId, group_concat(distinct categoryType) as mockCategory ");
        sqlBuilder.append(" from mocker_info where replayId = '' and recordId in (");
        sqlBuilder.append(" select recordId from mocker_info where replayId = ''");
        if (StringUtils.isNotBlank(mocker.getAppId())) {
            sqlBuilder.append(" AND appId = '").append(mocker.getAppId()).append("'");
        }
        sqlBuilder.append(" and categoryType = 'Servlet' ");
        if (StringUtils.isNotBlank(mocker.getOperationName())) {
            sqlBuilder.append(" AND operationName = '").append(mocker.getOperationName()).append("'");
        }
        sqlBuilder.append(" ) and operationName != 'java.lang.System.currentTimeMillis'");
        sqlBuilder.append(" group by recordId order by recordId desc)");
        if (count > 0) {
            sqlBuilder.append(" limit ").append(count);
        }
        return sqlBuilder.toString();
    }
}