package io.arex.standalone.cli.cmd;

import io.arex.standalone.common.util.StringUtil;
import io.arex.standalone.common.constant.Constants;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

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
    @Option(names = {"-n", "--num"}, description = "watch numbers, default 10", defaultValue = "10")
    int num;
    @CommandLine.ParentCommand
    RootCommand parent;
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        RootCommand.updateCmd(spec.name());

        StringBuilder options = new StringBuilder(" ");
        options.append("num=").append(num).append(Constants.CLI_SEPARATOR);
        parent.send(spec.name() + options);
        String response = parent.receive(spec.name());
        if (StringUtil.isEmpty(response) || !response.contains("{")) {
            parent.println("query result is empty, please confirm if there are any playback differences");
            return;
        }
        RootCommand.save(spec.name(), response);
        parent.println("difference result has been displayed in the browser");
        parent.openBrowser();
    }
}