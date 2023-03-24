package io.arex.standalone.cli.cmd;

import io.arex.agent.bootstrap.model.ArexMocker;
import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.model.Mocker;
import io.arex.agent.bootstrap.util.CollectionUtil;
import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.inst.runtime.util.TypeUtil;
import io.arex.standalone.cli.util.JsonUtil;
import io.arex.standalone.cli.util.LogUtil;
import io.arex.standalone.common.CommonUtils;
import io.arex.standalone.common.Constants;
import io.arex.standalone.common.RecordModel;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * List Command
 */
@Command(name = "ls", version = "v1.0",
        header = "@|yellow [ls command]|@ @|green list recorded cases|@",
        description = "list record cases",
        mixinStandardHelpOptions = true, sortOptions = false)
public class ListCommand implements Runnable {

    @Option(names = {"-o", "--operation"}, description = "input index or recorded operation name")
    String operation;

    @Option(names = {"-d", "--detail"}, description = "input index or recorded id")
    String detail;

    @Option(names = {"-n", "--num"}, description = "list numbers, default 10", defaultValue = "10")
    int num;

    @CommandLine.ParentCommand
    RootCommand parent;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    int colWidth = 0;
    static final String COLUMN_OPERATION = "[index]operation name:";
    static final String COLUMN_CASE_NUM = "case num:";
    static final String COLUMN_RECORD_ID = "[index]record id:";
    static final String COLUMN_CATEGORY = "mock category:";
    Map<String, String> operationMap = new HashMap<>();

    @Override
    public void run() {
        try {
            StringBuilder options = new StringBuilder(" ");
            options.append("num=").append(num).append(Constants.CLI_SEPARATOR);
            if (StringUtil.isNotEmpty(operation)) {
                // support input index or operationName
                String operationName = operationMap.get(operation);
                if (operationName != null) {
                     operation = operationName;
                }
                options.append("operation=").append(operation).append(Constants.CLI_SEPARATOR);
            } else if (StringUtil.isNotEmpty(detail)) {
                if (parent.recordIdMap.isEmpty()) {
                    parent.printErr("please execute command: ls -o first");
                    return;
                }
                // support input index or recordId
                String recordId = parent.recordIdMap.get(detail);
                if (recordId != null) {
                    detail = recordId;
                }
                options.append("detail=").append(detail).append(Constants.CLI_SEPARATOR);
            }
            parent.send(spec.name() + options);
            String response = parent.receive(spec.name());
            process(response);
        } catch (Throwable e) {
            parent.printErr("execute command {} fail:{}, visit {} for more details.", spec.name(), e.getMessage(), LogUtil.getLogDir());
            LogUtil.warn(e);
        }
    }

    private void process(String response) {
        if (StringUtil.isEmpty(response) || !response.contains("{")) {
            parent.printErr("query result invalid:{}", response);
            return;
        }
        List<RecordModel> resultList = Serializer.deserialize(response, TypeUtil.forName(Constants.TYPE_LIST_RECORD));
        if (CollectionUtil.isEmpty(resultList)) {
            return;
        }

        display(resultList);
    }

    private void display(List<RecordModel> recordList) {
        List<CommandLine.Help.Column> colHeaders = new ArrayList<>();
        List<String> colNames = new ArrayList<>();
        List<List<String>> rowList = new ArrayList<>();
        int size = recordList.size();
        int colNum;
        for (int i = 0; i < size; i++) {
            List<String> colList = new ArrayList<>();
            if (StringUtil.isNotEmpty(operation)) {
                // | [index]recordId | mockCategory |
                colNum = 2;
                setColWidth(colNum);
                // 1st row
                if (i == 0) {
                    colHeaders = columnHeader(colNum);

                    colNames.add(COLUMN_RECORD_ID + getSpace(COLUMN_RECORD_ID));
                    colNames.add(Constants.COLUMN_SEPARATOR);
                    colNames.add(COLUMN_CATEGORY + getSpace(COLUMN_CATEGORY));
                    parent.recordIdMap.clear();
                    parent.currentApi = operation;
                }
                String recordId = String.format("[%s]%s", recordList.get(i).getIndex(), recordList.get(i).getRecordId());
                colList.add(JsonUtil.breakLine(recordId, colWidth));
                colList.add(Constants.COLUMN_SEPARATOR);
                colList.add(JsonUtil.breakLine(recordList.get(i).getMockCategoryType(), colWidth));

                parent.recordIdMap.put(recordList.get(i).getIndex(), recordList.get(i).getRecordId());
                rowList.add(colList);
            } else if (StringUtil.isNotEmpty(detail)) {
                // | message |
                colNum = 1;
                setColWidth(colNum);
                // 1st row
                if (i == 0) {
                    colHeaders = columnHeader(colNum);
                }
                Mocker mocker = Serializer.deserialize(recordList.get(i).getMessage(), ArexMocker.class);
                if (mocker != null) {
                    String json = getRecordJson(mocker);
                    if (StringUtil.isNotEmpty(json)) {
                        String category = CommandLine.Help.Ansi.AUTO.string("@|bold,green "
                                + mocker.getCategoryType().getName() + ":|@");
                        rowList.add(Collections.singletonList(category));
                        colList.add(new String(json.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
                        rowList.add(colList);
                    }
                }
            } else {
                // | [index]operation | case num |
                colNum = 2;
                setColWidth(colNum);
                // 1st row
                if (i == 0) {
                    colHeaders = columnHeader(colNum);

                    colNames.add(COLUMN_OPERATION + getSpace(COLUMN_OPERATION));
                    colNames.add(Constants.COLUMN_SEPARATOR);
                    colNames.add(COLUMN_CASE_NUM + getSpace(COLUMN_CASE_NUM));
                    operationMap.clear();
                    parent.currentApi = null;
                }
                String operationName = String.format("[%s]%s", recordList.get(i).getIndex(), recordList.get(i).getOperationName());
                colList.add(JsonUtil.breakLine(operationName, colWidth));
                colList.add(Constants.COLUMN_SEPARATOR);
                colList.add(JsonUtil.breakLine(recordList.get(i).getCaseNum(), colWidth));

                operationMap.put(recordList.get(i).getIndex(), recordList.get(i).getOperationName());
                rowList.add(colList);
            }
        }
        if (StringUtil.isNotEmpty(operation)) {
            parent.println(operation);
        }
        drawTable(colHeaders, colNames, rowList);
    }

    private String getRecordJson(Mocker mocker) {
        StringBuilder builder = new StringBuilder();
        builder.append("{");
        Mocker.Target targetRequest = mocker.getTargetRequest();
        MockCategoryType category = mocker.getCategoryType();
        if (MockCategoryType.DYNAMIC_CLASS.getName().equals(category.getName())) {
            return null;
        }
        String response = mocker.getTargetResponse().getBody();
        if (category.isEntryPoint()) {
            builder.append("operation: ").append(mocker.getOperationName()).append(",");
            builder.append("request: ").append(JsonUtil.cleanFormat(CommonUtils.decode(targetRequest.getBody()))).append(",");
            builder.append("response: ").append(response);
        }
        if (MockCategoryType.DATABASE.getName().equals(category.getName())) {
//            compareMap.put("dbname", targetRequest.attributeAsString("dbName"));
            builder.append("sql: ").append("\"").append(targetRequest.getBody()).append("\"").append(",");
            builder.append("result: ").append(response);
        }
        if (MockCategoryType.HTTP_CLIENT.getName().equals(category.getName())) {
            builder.append("operation: ").append(mocker.getOperationName()).append(",");
            builder.append("request: ").append(CommonUtils.decode(targetRequest.getBody()));
        }
        if (MockCategoryType.REDIS.getName().equals(category.getName())) {
            builder.append("clusterName: ").append(targetRequest.attributeAsString("clusterName")).append(",");
            builder.append("key: ").append(targetRequest.getBody()).append(",");
            builder.append("result: ").append(response);
        }
        builder.append("}");
        return JsonUtil.formatJson(builder.toString());
    }

    private List<CommandLine.Help.Column> columnHeader(int size) {
        List<CommandLine.Help.Column> columns = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            columns.add(new CommandLine.Help.Column(colWidth, 0, CommandLine.Help.Column.Overflow.WRAP));
            if (i < size - 1) {
                columns.add(new CommandLine.Help.Column(2, 0, CommandLine.Help.Column.Overflow.WRAP));
            }
        }
        return columns;
    }

    private void drawTable(List<CommandLine.Help.Column> columns, List<String> columnNameList, List<List<String>> rowDataList) {
        if (rowDataList.isEmpty()) {
            return;
        }

        CommandLine.Help.TextTable textTable = CommandLine.Help.TextTable.forColumns(
                new CommandLine.Help.ColorScheme.Builder().applySystemProperties().build(),
                columns.toArray(new CommandLine.Help.Column[]{}));
        textTable.setAdjustLineBreaksForWideCJKCharacters(true);
        textTable.addRowValues(columnNameList.toArray(new String[0]));
        for (List<String> colList : rowDataList) {
            textTable.addRowValues(colList.toArray(new String[0]));
        }
        parent.println(textTable.toString());
    }

    private void setColWidth(int columns) {
        colWidth = parent.getTerminalWidth() / columns - 2;
    }

    private String getSpace(String str) {
        int width = colWidth / 2 - str.length();
        StringBuilder space = new StringBuilder();
        for (int i = 0; i < width; i++) {
            space.append(" ");
        }
        return space.toString();
    }
}