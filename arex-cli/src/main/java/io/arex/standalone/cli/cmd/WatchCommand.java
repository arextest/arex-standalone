package io.arex.standalone.cli.cmd;

import io.arex.agent.bootstrap.model.MockCategoryType;
import io.arex.agent.bootstrap.util.CollectionUtil;
import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.standalone.common.DiffMocker;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.inst.runtime.util.TypeUtil;
import io.arex.standalone.common.Constants;
import io.arex.standalone.cli.util.JsonUtil;
import io.arex.standalone.cli.util.LogUtil;
import org.jline.terminal.Terminal;
import org.jline.utils.NonBlockingReader;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.ArrayList;
import java.util.List;

/**
 * Watch Command
 * @Date: Created in 2022/4/2
 * @Modified By:
 */
@Command(name = "watch", version = "v1.0",
        header = "@|yellow [watch command]|@ @|green view replay result and differences|@",
        description = "view replay result and differences",
        mixinStandardHelpOptions = true, sortOptions = false)
public class WatchCommand implements Runnable {

    @Option(names = {"-r", "--replayId"}, arity = "1..*", description = "replay id, multiple are separated by spaces")
    String[] replayIds;

    String[] cacheIds;

    @CommandLine.ParentCommand
    RootCommand parent;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    private String space;

    @Override
    public void run() {
        if (replayIds != null && replayIds.length > 0) {
            cacheIds = replayIds;
        }
        if (cacheIds != null && cacheIds.length > 0) {
            try {
                int index = 0;
                showDiff(cacheIds[index]);

                if (cacheIds.length == 1) {
                    return;
                }

                String tip = CommandLine.Help.Ansi.AUTO.string(
                        "input @|bold,blue n|@ show next diff, @|bold,blue p|@ show previous diff, @|bold,red q|@ means quit");

                Terminal terminal = parent.reader.getTerminal();
                if (terminal.getWidth() > 0) {
                    parent.println(tip);
                    terminal.enterRawMode(); // (character editing mode) interactive mode
                    NonBlockingReader reader = terminal.reader();
                    int read;
                    while ((read = reader.read()) != 113) {
                        switch (read) {
                            case 110:
                                index ++;
                                if (index < 0) {
                                    index = 1;
                                }
                                if (index < cacheIds.length) {
                                    showDiff(cacheIds[index]);
                                } else {
                                    parent.println("already last one");
                                }
                                break;
                            case 112:
                                index --;
                                if (index >= cacheIds.length) {
                                    index = cacheIds.length - 2;
                                }
                                if (index >= 0) {
                                    showDiff(cacheIds[index]);
                                } else {
                                    parent.println("already first one");
                                }
                                break;
                            default:
                                parent.println(tip);
                        }
                    }
                } else { // compatible non-command line terminals, do not support interactive mode (such as IDE console)
                    tip = tip + ", press Enter to confirm";
                    parent.println(tip);
                    String line;
                    while ((line = parent.reader.readLine()) != null && !"q".equals(line)) {
                        switch (line) {
                            case "n":
                                index ++;
                                if (index < 0) {
                                    index = 1;
                                }
                                if (index < cacheIds.length) {
                                    showDiff(cacheIds[index]);
                                } else {
                                    parent.println("already last one");
                                }
                                break;
                            case "p":
                                index --;
                                if (index >= cacheIds.length) {
                                    index = cacheIds.length - 2;
                                }
                                if (index >= 0) {
                                    showDiff(cacheIds[index]);
                                } else {
                                    parent.println("already first one");
                                }
                                break;
                            default:
                                parent.println(tip);
                        }
                    }
                }
                parent.println("");
            } catch (Exception e) {
                parent.printErr("execute command {} fail, visit {} for more details.", spec.name(), LogUtil.getLogDir());
                LogUtil.warn(e);
            }
        } else {
            parent.println("please input replay id");
        }
    }

    private void showDiff(String replayId) {
        if (StringUtil.isEmpty(replayId)) {
            return;
        }
        parent.send(spec.name() + " " + replayId);
        String response = parent.receive(spec.name());
        if (StringUtil.isEmpty(response) || !response.contains("{")) {
            return;
        }
        List<DiffMocker> diffList = Serializer.deserialize(response, TypeUtil.forName(Constants.TYPE_LIST_DIFFMOCKER));
        if (CollectionUtil.isEmpty(diffList)) {
            return;
        }
        String operationName = getOperationBySorted(diffList);
        parent.println("\noperation: {}, diff replayId: {}", operationName, replayId);
        for (DiffMocker diffMocker : diffList) {
            drawTable(diffMocker.getRecordDiff(), diffMocker.getReplayDiff(), diffMocker.getCategoryType());
        }
    }

    private String getOperationBySorted(List<DiffMocker> diffList) {
        DiffMocker mainMocker = diffList.stream().filter(diffMocker -> diffMocker.getCategoryType().isEntryPoint()).findFirst().orElse(null);
        if (mainMocker != null) {
            diffList.remove(mainMocker);
            diffList.add(0, mainMocker);
            return mainMocker.getOperationName();
        }
        return null;
    }

    private void drawTable(String diff1, String diff2, MockCategoryType category) {
        String[] diffArray1 = diff1.split("\n");
        String[] diffArray2 = diff2.split("\n");

        int width = getColWidth();
        CommandLine.Help.TextTable textTable = CommandLine.Help.TextTable.forColumns(
                new CommandLine.Help.ColorScheme.Builder().applySystemProperties().build(),
                new CommandLine.Help.Column(width, 0, CommandLine.Help.Column.Overflow.WRAP),
                new CommandLine.Help.Column(1, 0, CommandLine.Help.Column.Overflow.WRAP),
                new CommandLine.Help.Column(width, 0, CommandLine.Help.Column.Overflow.WRAP));
        textTable.setAdjustLineBreaksForWideCJKCharacters(true);

        String space = getSpace();
        String columnRecord = space + "record" + space;
        String columnReplay = space + "replay" + space;
        textTable.addRowValues(columnRecord, "|", columnReplay);

        int diffNum = 0;
        String[] diffArray = diffArray1.length >= diffArray2.length ? diffArray1 : diffArray2;
        for (int i = 0; i < diffArray.length; i++) {
            textTable.addRowValues(JsonUtil.breakLine(i < diffArray1.length ? diffArray1[i] : "", width - 1), "|",
                    JsonUtil.breakLine(i < diffArray2.length ? diffArray2[i] : "", width - 1));
            if (diffArray[i].contains("@|bg")) {
                diffNum ++;
            }
        }

        String color = "@|bold,green ";
        if (diffNum > 0) {
            color = "@|bold,red ";
        }
        parent.println(category.getName() +": "+ CommandLine.Help.Ansi.AUTO.string(
                color + diffNum + "|@") + " differences");
        parent.println(textTable.toString());
//        parent.println("");
    }

    private int getColWidth() {
        return parent.getTerminalWidth() / 2 -1;
    }

    private String getSpace() {
        int width = getColWidth() / 2 - 6;
        StringBuilder space = new StringBuilder();
        for (int i = 0; i < width; i++) {
            space.append(" ");
        }
        return space.toString();
    }
}