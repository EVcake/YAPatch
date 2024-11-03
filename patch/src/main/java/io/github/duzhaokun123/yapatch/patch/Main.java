package io.github.duzhaokun123.yapatch.patch;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.duzhaokun123.yapatch.patch.utils.Logger;
import io.github.duzhaokun123.yapatch.patch.utils.StdLogger;

public class Main {
    public static class Patch {
        @Parameter(description = "apk")
        protected String apk = null;

        @Parameter(names = {"-h", "--help"}, help = true, order = 0, description = "Print this message")
        protected boolean help = false;

        @Parameter(names = {"-o", "--output"}, description = "Output directory")
        protected String outputPath = ".";

        @Parameter(names = {"-k", "--keystore"}, arity = 4, description = "Set custom signature keystore. Followed by 4 arguments: keystore path, keystore password, keystore alias, keystore alias password")
        protected List<String> keystoreArgs = Arrays.asList(null, "123456", "key0", "123456");

        @Parameter(names = {"-m", "--modules"}, description = "Xposed modules package name to load")
        protected List<String> modules = new ArrayList<>();

        @Parameter(names = {"-l", "--sigbypasslv"}, description = "Signature bypass level. 0 (disable), 1 (pm). default 0")
        protected int sigbypassLevel = 0;

        @Parameter(names = {"-s", "--split"}, description = "Split apks")
        protected List<String> splitApks = new ArrayList<>();

        @Parameter(names = {"-v", "--version"}, description = "Print version")
        protected boolean version = false;

        private final JCommander jCommander;
        protected final Logger logger;

        public Patch(Logger logger, String... args) {
            this.logger = logger;
            jCommander =JCommander.newBuilder()
                    .addObject(this)
                    .build();
            try {
                jCommander.parse(args);
            } catch (Exception e) {
                logger.error(e.getMessage());
                help = true;
            }

            if (version) return;

            if (apk == null) {
                logger.error("No apk specified");
                help = true;
            }
        }

        public void run() {
            logger.info("Running patch");
        }
    }

    public static void main(String[] args) {
        Patch patch = new PatchKt(StdLogger.INSTANCE, args);

        if (patch.version) {
            System.out.println(Versions.INSTANCE);
            return;
        }

        if (patch.help) {
            patch.jCommander.usage();
            return;
        }

        try {
            patch.run();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
}
