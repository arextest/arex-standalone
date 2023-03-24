package io.arex.standalone.cli.cmd;

import io.arex.standalone.common.util.CollectionUtil;
import io.arex.standalone.common.util.StringUtil;
import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.common.model.DiffModel;
import io.arex.standalone.common.serializer.Serializer;
import io.arex.standalone.common.util.TypeUtil;
import io.arex.standalone.cli.util.LogUtil;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Replay Command
 * @Date: Created in 2022/4/2
 * @Modified By:
 */
@Command(name = "replay", version = "v1.0",
        header = "@|yellow [replay command]|@ @|green replay recorded data and view differences|@",
        description = "replay recorded data and view differences",
        mixinStandardHelpOptions = true, sortOptions = false)
public class ReplayCommand implements Runnable {
    @Option(names = {"-n", "--num"}, description = "replay numbers, default 10", defaultValue = "10")
    int num;
    @Option(names = {"-r", "--recordId"}, description = "input index or record id, single replay available for local debug")
    String recordIdOrIndex;

    @ParentCommand
    RootCommand parent;

    @Spec
    Model.CommandSpec spec;

    @Override
    public void run() {
        try {
            if (StringUtil.isNotEmpty(recordIdOrIndex)) {
                String recordId = parent.recordIdMap.get(recordIdOrIndex);
                if (recordId != null) {
                    recordIdOrIndex = recordId;
                }
                // call the debug command
                CommandLine cmd = spec.parent().subcommands().get("debug");
                cmd.execute("-r", recordIdOrIndex);
                return;
            }

            long startNanoTime = System.nanoTime();
            parent.println("start replay...");

            StringBuilder options = new StringBuilder(" ");
            options.append("num=").append(num).append(Constants.CLI_SEPARATOR);
            if (StringUtil.isNotEmpty(parent.currentApi)) {
                options.append("operation=").append(parent.currentApi).append(Constants.CLI_SEPARATOR);
            }

            parent.send(spec.name() + options);
            String response = parent.receive(spec.name());

            parent.println("replay complete, elapsed mills: "
                    + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime));
            parent.println("start compute difference...");

            String result = "@|bold,green no differences|@";
            String[] replayIds = null;
            if (StringUtil.isNotEmpty(response) && response.contains("{")) {
                List<DiffModel> diffList = Serializer.deserialize(response, TypeUtil.forName(Constants.TYPE_LIST_DIFF));
                if (CollectionUtil.isNotEmpty(diffList)) {
                    int totalDiffCount = 0;
                    replayIds = new String[diffList.size() + 1];
                    replayIds[0] = "-r";
                    for (int i = 0; i < diffList.size(); i++) {
                        totalDiffCount += diffList.get(i).getDiffCount();
                        replayIds[i + 1] = diffList.get(i).getReplayId();
                    }
                    result = "@|bold,red there are " + totalDiffCount + " differences in total|@";
                }
            }
            parent.println("comparison result: " + Help.Ansi.AUTO.string(result));

            // If there is any differences call the watch command
            if (replayIds != null && replayIds.length > 1) {
                CommandLine cmd = spec.parent().subcommands().get("watch");
                cmd.execute(replayIds);
            }
        } catch (Throwable e) {
            parent.printErr("execute command {} fail, visit {} for more details.", spec.name(), LogUtil.getLogDir());
            LogUtil.warn(e);
        }
    }
}
