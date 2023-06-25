package io.arex.standalone.cli.cmd;

import io.arex.standalone.cli.server.ServerListener;
import io.arex.standalone.cli.util.TelnetUtil;
import io.arex.standalone.common.util.*;
import io.arex.standalone.cli.util.LogUtil;
import io.arex.standalone.common.constant.Constants;
import org.jline.reader.LineReader;
import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model;
import picocli.CommandLine.Spec;
import picocli.shell.jline3.PicocliCommands;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.List;

import static io.arex.standalone.common.constant.Constants.APP_PORT;

/**
 * Root Command
 * @Date: Created in 2022/4/2
 * @Modified By:
 */
@Command(name = "version 1.0.0",
        description = "Arex Commander",
        footer = {"", "Press Ctrl-D to exit."},
        subcommands = {ListCommand.class, ReplayCommand.class, WatchCommand.class, DebugCommand.class,
                PicocliCommands.ClearScreen.class, HelpCommand.class})
public class RootCommand implements Runnable {

    @CommandLine.Option(names = {"-i", "--ip"}, description = "arex server ip", defaultValue = "127.0.0.1", hidden = true)
    String ip;

    @CommandLine.Option(names = {"-p", "--port"}, description = "arex server tcp port", defaultValue = "4000", hidden = true)
    int tcpPort;

    @CommandLine.Option(names = {"-h", "--httpport"}, description = "arex server http port", defaultValue = "4050", hidden = true)
    int httpPort;

    @Spec
    Model.CommandSpec spec;

    PrintWriter out;

    PrintWriter err;

    LineReader reader;

    int terminalWidth = 110;
    int terminalHeight = 50;
    static final String API = "api";
    static final String CMD = "cmd";
    static final String PORT = "port";
    static Map<String, String> cached = new HashMap<>();

    public void setReader(LineReader reader){
        out = reader.getTerminal().writer();
        err = spec.commandLine().getErr();
        this.reader = reader;
    }

    @Override
    public void run() {
        welcome();
        if (!agent()) {
            return;
        }
        if (!connect()) {
            printErr("connect fail, visit {} for more details.", LogUtil.getLogDir());
            return;
        }
        println("connect {} {}", ip, tcpPort);

        spec.commandLine().usage(out);
        start();
    }

    private void welcome() {
        out.println(CommandLine.Help.Ansi.AUTO.string("@|bold,blue     ___    ____  _______  __|@"));
        out.println(CommandLine.Help.Ansi.AUTO.string("@|bold,blue    /   |  / __ \\/ ____/ |/ /|@"));
        out.println(CommandLine.Help.Ansi.AUTO.string("@|bold,blue   / /| | / /_/ / __/  |   / |@"));
        out.println(CommandLine.Help.Ansi.AUTO.string("@|bold,blue  / ___ |/ _, _/ /___ /   |  |@"));
        out.println(CommandLine.Help.Ansi.AUTO.string("@|bold,blue /_/  |_/_/ |_/_____//_/|_|  |@"));
        out.println("");
        out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|bold,cyan Automated regression testing platform with real data|@"));
        out.println(CommandLine.Help.Ansi.AUTO.string(
                "@|bold,cyan http://arextest.com|@"));
        out.println("");
    }

    public boolean agent() {
        try {
            Map<Long, String> processMap = SystemUtils.javaPids();
            if (processMap.isEmpty()) {
                printErr("pid is null");
                return false;
            }

            println("Please choose one serial number of the process: [n], then press Enter to start Arex");
            int count = 1;
            for (String process : processMap.values()) {
                println("  [" + count + "]: " + process);
                count++;
            }

            // check choose number
            int choice = 0;
            String line;
            while ((line = reader.readLine()) != null) {
                if (NumberUtils.isNotDigits(line)) {
                    printErr("not a number");
                    continue;
                }
                choice = Integer.parseInt(line);
                if (choice <= 0 || choice > processMap.size()) {
                    printErr("number is invalid");
                    continue;
                }
                break;
            }

            long selectPid = 0;
            Iterator<Long> idIter = processMap.keySet().iterator();
            for (int i = 1; i <= choice; ++i) {
                if (i == choice) {
                    selectPid = idIter.next();
                    break;
                }
                idIter.next();
            }

            if (selectPid <= 0) {
                printErr("pid is invalid");
                return false;
            }

            long tcpPortPid = SystemUtils.findTcpListenProcess(tcpPort);
            // check tcp port is available
            if (tcpPortPid > 0 && tcpPortPid == selectPid) {
                println("The target process {} already listen port {}, skip attach.", selectPid, tcpPort);
                return true;
            }
            if (tcpPortPid > 0 && tcpPortPid != selectPid) {
                printErr("The tcp port {} is used by process {} instead of target process {}, you can specify port number, " +
                        "command line example: java -jar arex-cli.jar -p [port number]",
                        tcpPort, tcpPortPid, selectPid);
                return false;
            }

            return attach(selectPid);
        } catch (Throwable e) {
            printErr("agent fail: {}, visit {} for more details.", e.getMessage(), LogUtil.getLogDir());
            LogUtil.warn(e);
        }
        return false;
    }

    private boolean attach(long pid) throws Exception {
        String javaHome = SystemUtils.findJavaHome();
        String javaBinDir = SystemUtils.javaBinDir(javaHome);
        if (StringUtil.isEmpty(javaBinDir)) {
            printErr("Can not find java/java.exe executable file under java home: {}", javaHome);
            return false;
        }

        String toolsJarDir = SystemUtils.getToolsJarDir();
        if (SystemUtils.lessThanJava9() && StringUtil.isEmpty(toolsJarDir)) {
            printErr("Can not find tools.jar under java home: {}", javaHome);
            return false;
        }

        List<String> command = new ArrayList<>();
        command.add(javaBinDir);

        if (StringUtil.isNotEmpty(toolsJarDir)) {
            command.add("-Xbootclasspath/a:" + toolsJarDir);
        }

        command.add("-jar");
        String attachJarPath = SystemUtils.findModuleJarDir("", "arex-attacher");
        if (StringUtil.isEmpty(attachJarPath)) {
            printErr("{} jar not exist, please confirm whether it is in the same level directory", "arex-attacher");
            return false;
        }
        command.add(attachJarPath);
        command.add(""+pid);
        String agentJarPath = SystemUtils.findModuleJarDir("", "arex-agent");
        if (StringUtil.isEmpty(agentJarPath)) {
            printErr("{} jar not exist, please confirm whether it is in the same level directory", "arex-agent-jar");
            return false;
        }
        command.add(agentJarPath);

        command.add("arex.storage.mode=local;arex.enable.debug=true;arex.server.tcp.port=" + tcpPort);

        ProcessBuilder pb = new ProcessBuilder(command);
        Process proc = pb.start();
        println("starting AREX, please wait...");
        InputStream inputStream = proc.getInputStream();
        InputStream errorStream = proc.getErrorStream();
        IOUtils.copy(inputStream, System.out);
        IOUtils.copy(errorStream, System.err);

        proc.waitFor();

        int exitValue = proc.exitValue();
        if (exitValue != 0) {
            printErr("Attach fail, pid: {}", pid);
            return false;
        }

        println("Attach process {} success", pid);
        return true;
    }

    private boolean connect() {
        return TelnetUtil.connect(ip, tcpPort, getTerminalWidth(), getTerminalHeight());
    }

    public void send(String command) {
        TelnetUtil.send(command);
    }

    public String receive(String command) {
        return TelnetUtil.receive(command);
    }

    public void close() {
        TelnetUtil.close();
    }

    public int getTerminalWidth() {
        return reader.getTerminal().getWidth() > 0 ? reader.getTerminal().getWidth() : terminalWidth;
    }

    public int getTerminalHeight() {
        return reader.getTerminal().getHeight() > 0 ? reader.getTerminal().getHeight() : terminalHeight;
    }

    public void println(String from, Object... arguments) {
        if (StringUtil.isEmpty(from)) {
            out.println();
            return;
        }
        out.println(LogUtil.format(from, arguments));
    }

    public void printErr(String from, Object... arguments) {
        err.println(LogUtil.format(from, arguments));
    }

    public static void updateApi(String currentApi) {
        save(API, currentApi);
    }

    public static String currentApi() {
        return query(API);
    }

    public String getPrompt() {
        AttributedStringBuilder builder = new AttributedStringBuilder()
                .style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
                .append(Constants.CLI_PROMPT);
        if (StringUtil.isNotEmpty(currentApi())) {
            return builder.style(AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN))
                    .append(currentApi())
                    .append("> ")
                    .toAnsi();
        }
        return builder.append(" ").toAnsi();
    }

    public String getRightPrompt() {
        return null;
    }

    private void start() {
        new ServerListener(httpPort).start();
    }

    public static void save(String key, String val) {
        cached.put(key, val);
    }

    public static String query(String key) {
        return cached.get(key);
    }

    public static String query(String key, String def) {
        return cached.getOrDefault(key, def);
    }

    public static void updateCmd(String cmd) {
        save(CMD, cmd);
    }

    public static String currentCmd() {
        return query(CMD);
    }

    public static void updateAppPort(int port) {
        save(PORT, String.valueOf(port));
    }

    public static int currentAppPort() {
        return Integer.parseInt(query(PORT, APP_PORT));
    }

    public void openBrowser() {
        CommonUtils.EXECUTOR.submit(() -> {
            try {
                // to avoid screen switches too fast for users to see command line prompts
                Thread.sleep(1000);
                Desktop desktop = Desktop.getDesktop();
                if (Desktop.isDesktopSupported() && desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI("http://localhost:" + httpPort));
                }
            } catch (Throwable e) {
                printErr("open browser fail:{}, visit {} for more details.", e, LogUtil.getLogDir());
                LogUtil.warn(e);
            }
        });
    }
}
