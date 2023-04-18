package io.arex.standalone.cli.cmd;

import io.arex.standalone.common.util.*;
import io.arex.standalone.common.serializer.Serializer;
import io.arex.standalone.cli.util.LogUtil;
import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.common.model.LocalModel;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.*;
import java.util.List;

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
    @Option(names = {"-n", "--num"}, description = "list numbers, default 10", defaultValue = "10")
    int num;
    @CommandLine.ParentCommand
    RootCommand parent;
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    int colWidth = 0;
    static final String COLUMN_OPERATION = "[index] operation name:";
    static final String COLUMN_CASE_NUM = "case num:";
    Map<String, String> operationMap = new HashMap<>();

    @Override
    public void run() {
        try {
            RootCommand.updateCmd(spec.name());
            StringBuilder options = new StringBuilder(" ");
            options.append("num=").append(num).append(Constants.CLI_SEPARATOR);
            if (StringUtil.isNotEmpty(operation)) {
                // support input index or operationName
                String operationName = operationMap.get(operation);
                if (operationName != null) {
                     operation = operationName;
                }
                options.append("operation=").append(operation).append(Constants.CLI_SEPARATOR);
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
            parent.printErr("query result is empty, please confirm if there are any recorded cases");
            return;
        }
        RootCommand.save(spec.name(), response);
        if (StringUtil.isNotEmpty(operation)) {
            RootCommand.updateApi(operation);
            parent.println("record result has been displayed in the browser");
            parent.openBrowser();
            return;
        }

        List<LocalModel> resultList = Serializer.deserialize(response, TypeUtil.forName(Constants.TYPE_LIST_LOCAL));
        if (CollectionUtil.isEmpty(resultList)) {
            return;
        }

        display(resultList);
    }

    private void display(List<LocalModel> recordList) {
        List<CommandLine.Help.Column> colHeaders = new ArrayList<>();
        List<String> colNames = new ArrayList<>();
        List<List<String>> rowList = new ArrayList<>();
        int size = recordList.size();
        int colNum;
        for (int i = 0; i < size; i++) {
            List<String> colList = new ArrayList<>();
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
                RootCommand.updateApi("");
            }
            String operationName = String.format("[%s] %s", recordList.get(i).getIndex(), recordList.get(i).getOperationName());
            colList.add(JsonUtil.breakLine(operationName, colWidth));
            colList.add(Constants.COLUMN_SEPARATOR);
            colList.add(JsonUtil.breakLine(recordList.get(i).getCaseNum(), colWidth));

            operationMap.put(recordList.get(i).getIndex(), recordList.get(i).getOperationName());
            rowList.add(colList);
        }
        drawTable(colHeaders, colNames, rowList);
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