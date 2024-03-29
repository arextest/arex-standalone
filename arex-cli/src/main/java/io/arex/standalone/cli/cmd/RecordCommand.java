package io.arex.standalone.cli.cmd;

import io.arex.standalone.cli.util.LogUtil;
import picocli.CommandLine.*;

/**
 * Record Command
 * @Date: Created in 2022/4/2
 * @Modified By:
 */
@Command(name = "record", version = "v1.0",
        header = "@|yellow [record command]|@ @|green turn on record or set record rate|@",
        description = "turn on record, set recording frequency",
        mixinStandardHelpOptions = true, sortOptions = false)
public class RecordCommand implements Runnable {

    @Option(names = {"-r", "--rate"}, description = "set record rate, default value 1, record once every 60 seconds")
    int rate;

    @Option(names = {"-c", "--close"}, description = "shut down record")
    boolean close;

    @ParentCommand
    RootCommand parent;

    @Spec
    Model.CommandSpec spec;

    @Override
    public void run() {
        try {
            StringBuilder options = new StringBuilder(" ");
            if (close) {
                options.append("-c");
            }
            if (rate > 0) {
                options.append("-r=").append(rate);
            }
            parent.send(spec.name() + options);
            parent.println(parent.receive(spec.name()));
        } catch (Throwable e) {
            parent.printErr("execute command {} fail:{}, visit {} for more details.", spec.name(), e.getMessage(), LogUtil.getLogDir());
            LogUtil.warn(e);
        }
    }
}
