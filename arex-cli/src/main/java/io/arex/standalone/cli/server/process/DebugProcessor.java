package io.arex.standalone.cli.server.process;

import io.arex.standalone.cli.cmd.RootCommand;
import io.arex.standalone.cli.server.Request;
import io.arex.standalone.cli.util.TelnetUtil;
import io.arex.standalone.common.constant.Constants;
import io.arex.standalone.common.util.StringUtil;

public class DebugProcessor extends AbstractProcessor {
    @Override
    public String process(Request request) throws Exception {
        String recordId = getRecordId(request);
        if (StringUtil.isEmpty(recordId)) {
            return fail("recordId is empty");
        }
        int port = RootCommand.currentAppPort();
        StringBuilder options = new StringBuilder(" ");
        options.append("recordId=").append(recordId).append(Constants.CLI_SEPARATOR);
        options.append("port=").append(port).append(Constants.CLI_SEPARATOR);
        TelnetUtil.send("debug" + options);
        return TelnetUtil.receive("debug");
    }
}
