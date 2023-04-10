package io.arex.standalone.cli.cmd;

import io.arex.standalone.common.util.StringUtil;
import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.cli.util.LogUtil;
import picocli.CommandLine;
import picocli.CommandLine.*;

import java.util.concurrent.TimeUnit;

import static io.arex.standalone.common.constant.Constants.APP_PORT;

/**
 * Replay Command
 */
@Command(name = "replay", version = "v1.0",
        header = "@|yellow [replay command]|@ @|green replay recorded data and view differences|@",
        description = "replay recorded data and view differences",
        mixinStandardHelpOptions = true, sortOptions = false)
public class ReplayCommand implements Runnable {
    @Option(names = {"-n", "--num"}, description = "replay numbers, default 10", defaultValue = "10")
    int num;
    @CommandLine.Option(names = {"-p", "--port"}, description = "your own local application http port number", defaultValue = APP_PORT, hidden = true)
    int port;
    @ParentCommand
    RootCommand parent;
    @Spec
    Model.CommandSpec spec;

    @Override
    public void run() {
        try {
            RootCommand.updateCmd(spec.name());
            RootCommand.updateAppPort(port);
            long startNanoTime = System.nanoTime();
            parent.println("start replay...");

            StringBuilder options = new StringBuilder(" ");
            options.append("num=").append(num).append(Constants.CLI_SEPARATOR);
            options.append("port=").append(port).append(Constants.CLI_SEPARATOR);
            if (StringUtil.isNotEmpty(RootCommand.currentApi())) {
                options.append("operation=").append(RootCommand.currentApi()).append(Constants.CLI_SEPARATOR);
            }

            parent.send(spec.name() + options);
            String response = parent.receive(spec.name());

            parent.println("replay complete, elapsed mills: "
                    + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanoTime));
            if (StringUtil.isEmpty(response) || !response.contains("{")) {
                parent.println(Help.Ansi.AUTO.string("@|bold,green no differences|@"));
                return;
            }
            RootCommand.save(spec.name(), response);
            parent.println("difference result has been displayed in the browser");
            parent.openBrowser();
        } catch (Throwable e) {
            parent.printErr("execute command {} fail, visit {} for more details.", spec.name(), LogUtil.getLogDir());
            LogUtil.warn(e);
        }
    }
}
