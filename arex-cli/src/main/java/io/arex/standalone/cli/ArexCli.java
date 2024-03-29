package io.arex.standalone.cli;


import io.arex.foundation.serializer.JacksonSerializer;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.standalone.cli.cmd.RootCommand;
import io.arex.standalone.cli.util.LogUtil;
import org.fusesource.jansi.AnsiConsole;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.*;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;
import picocli.shell.jline3.PicocliCommands.PicocliCommandsFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

/**
 * Command Entrance
 * @Date: Created in 2022/4/2
 * @Modified By:
 */
public class ArexCli {

    public static void main(String... args) {
        // support window ANSI
        AnsiConsole.systemInstall();
        try {
            init();
            Supplier<Path> workDir = () -> Paths.get(System.getProperty("user.dir"));
            // set up JLine built-in commands
            Builtins builtins = new Builtins(workDir, null, null);
            builtins.rename(Builtins.Command.TTOP, "top");
            builtins.alias("zle", "widget");
            builtins.alias("bindkey", "keymap");
            // set up picocli commands
            RootCommand commands = new RootCommand();

            PicocliCommandsFactory factory = new PicocliCommandsFactory();

            CommandLine cmd = new CommandLine(commands, factory);
            PicocliCommands picocliCommands = new PicocliCommands(cmd);

            // register parameter parser
            Parser parser = new DefaultParser();
            try (Terminal terminal = TerminalBuilder.builder().jna(true).system(true).build()) {
                SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, workDir, null);
                systemRegistry.setCommandRegistries(builtins, picocliCommands);
                systemRegistry.register("help", picocliCommands);
                // build command line interaction
                LineReader reader = LineReaderBuilder.builder()
                        .terminal(terminal)
                        .completer(systemRegistry.completer())
                        .parser(parser)
                        .variable(LineReader.LIST_MAX, 50)   // max tab completion candidates
                        .build();
                builtins.setLineReader(reader);
                commands.setReader(reader);
                factory.setTerminal(terminal);
                TailTipWidgets widgets = new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
                widgets.enable();
                KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
                keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));
                // execute main command / entry
                cmd.execute(args);

                // start the shell and process input until the user quits with Ctrl-D
                String line;
                while (true) {
                    try {
                        systemRegistry.cleanUp();
                        line = reader.readLine(commands.getPrompt(), commands.getRightPrompt(), (MaskingCallback) null, null);
                        systemRegistry.execute(line);
                    } catch (UserInterruptException e) {
                        // user interrupt command ignore (Ctrl-C)
                    } catch (EndOfFileException e) {
                        // Ctrl-D
                        commands.close();
                        System.exit(1);
                        return;
                    } catch (Exception e) {
                        systemRegistry.trace(e);
                        LogUtil.warn(e);
                    }
                }
            }
        } catch (Throwable t) {
            LogUtil.warn(t);
        } finally {
            AnsiConsole.systemUninstall();
        }
    }

    private static void init() {
        // cli is an independent process that does not depend on arex-agent startup
        // so need to initialize the serializer here
        Serializer.builder(JacksonSerializer.INSTANCE).build();
    }
}
