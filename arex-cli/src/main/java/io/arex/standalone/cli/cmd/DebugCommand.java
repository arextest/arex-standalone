package io.arex.standalone.cli.cmd;

import io.arex.agent.bootstrap.util.StringUtil;
import io.arex.standalone.cli.util.JsonUtil;
import io.arex.standalone.cli.util.LogUtil;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Debug Command
 * @Date: Created in 2022/4/2
 * @Modified By:
 */
@Command(name = "debug", version = "v1.0",
        header = "@|yellow [debug command]|@ @|green local debugging of specific cases|@",
        description = "local debugging of specific cases", mixinStandardHelpOptions = true, sortOptions = false, hidden = true)
public class DebugCommand implements Runnable {

    @Option(names = {"-r", "--recordId"}, required = true, description = "record id, required Option")
    String recordId;

    @CommandLine.ParentCommand
    RootCommand parent;

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        try {
            parent.println("start debug...");
            parent.send(spec.name() + " " + recordId);
            String response = parent.receive(spec.name());
            if (StringUtil.isEmpty(response) || !response.contains("{")) {
                parent.printErr("query result invalid:{}", response);
                return;
            }
            parent.println("debug complete, response:");
            parent.println(JsonUtil.formatJson(response));
            parent.println("");
        } catch (Throwable e) {
            parent.printErr("execute command {} fail, visit {} for more details.", spec.name(), LogUtil.getLogDir());
            LogUtil.warn(e);
        }
    }

}
