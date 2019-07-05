package org.neo4j.spatial.benchmarks;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.IterationParams;
import org.openjdk.jmh.profile.ExternalProfiler;
import org.openjdk.jmh.profile.InternalProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.String.format;

public class JfrProfiler implements InternalProfiler, ExternalProfiler {

    private boolean DUMP_ON_EXIT = true;

    static long getPid() {
        final String DELIM = "@";

        String name = ManagementFactory.getRuntimeMXBean().getName();

        if (name != null) {
            int idx = name.indexOf(DELIM);

            if (idx != -1) {
                String str = name.substring(0, name.indexOf(DELIM));
                try {
                    return Long.valueOf(str);
                } catch (NumberFormatException nfe) {
                    throw new IllegalStateException("Process PID is not a number: " + str);
                }
            }
        }
        throw new IllegalStateException("Unsupported PID format: " + name);
    }

    @Override
    public Collection<String> addJVMInvokeOptions(BenchmarkParams params) {
        return Collections.emptyList();
    }

    @Override
    public Collection<String> addJVMOptions(BenchmarkParams params) {
        List<String> args = Arrays.asList(
                "-XX:+UnlockCommercialFeatures",
                "-XX:+UnlockDiagnosticVMOptions",
                "-XX:+FlightRecorder",
                "-XX:+DebugNonSafepoints",
                "-XX:+PreserveFramePointer",
                "-XX:FlightRecorderOptions=stackdepth=256");
        return args;
    }

    @Override
    public void beforeTrial(BenchmarkParams benchmarkParams) {

    }

    @Override
    public Collection<? extends Result> afterTrial(BenchmarkResult br, long pid, File stdOut, File stdErr) {
        return Collections.emptyList();
    }

    @Override
    public boolean allowPrintOut() {
        return false;
    }

    @Override
    public boolean allowPrintErr() {
        return false;
    }

    @Override
    public void beforeIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams) {
        startJfr(benchmarkParams.getBenchmark());
    }

    @Override
    public Collection<? extends Result> afterIteration(BenchmarkParams benchmarkParams, IterationParams iterationParams, IterationResult result) {
        stopJfr(benchmarkParams.getBenchmark());
        return Collections.emptyList();
    }

    @Override
    public String getDescription() {
        return null;
    }

    private void startJfr(String name) {
        try {

            // JFR profiler log -- used as redirect for the process that starts the JFR recording
            Path jfrLog = Paths.get("jfr.log");

            // -----------------------------------------------------------------------------------------------
            // ------------------------------------- start JFR profiler --------------------------------------
            // -----------------------------------------------------------------------------------------------
            // NOTE: sometimes interesting things occur after benchmark, e.g., db shutdown takes long time. dumponexit setting helps investigate those cases.
            String[] startJfrCommand = !DUMP_ON_EXIT ? new String[6] : new String[7];
            startJfrCommand[0] = Paths.get("/opt/java/jdk1.8.0_211/").resolve("bin").resolve("jcmd").toAbsolutePath().toString();
            startJfrCommand[1] = Long.toString(getPid());
            startJfrCommand[2] = "JFR.start";
            startJfrCommand[3] = "settings=profile";
            startJfrCommand[4] = "name=" + name;
            startJfrCommand[5] = "dumponexit=" + DUMP_ON_EXIT;
            if (DUMP_ON_EXIT) {
                startJfrCommand[6] = "filename='DUMPED'";
            }

            Process startJfr = new ProcessBuilder(startJfrCommand)
                    .redirectOutput(jfrLog.toFile())
                    .redirectError(jfrLog.toFile())
                    .start();

            int resultCode = startJfr.waitFor();
            String jfrLogContents = new String(Files.readAllBytes(jfrLog));
            if (resultCode != 0 || jfrLogContents.contains("Could not start recording")) {
                throw new RuntimeException(
                        "Bad things happened when starting JFR recording, result code = " + resultCode + "\n" +
                                "See: " + jfrLog.toAbsolutePath());
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error trying to start JFR profiler", e);
        }
    }

    private void stopJfr(String name) {
        try {
            Path jfrProfilerOutput = Paths.get("benchmarks/results/" + name + ".jfr");

            // JFR profiler log -- used as redirect for the process that starts the JFR recording
            Path jfrLog = Paths.get("jfr.log");

            // -----------------------------------------------------------------------------------------------
            // ------------------------------- dump profiler recording to file -------------------------------
            // -----------------------------------------------------------------------------------------------

            String[] dumpJfrCommand = {
                    Paths.get("/opt/java/jdk1.8.0_211/").resolve("bin").resolve("jcmd").toAbsolutePath().toString(),
                    Long.toString(getPid()),
                    "JFR.dump",
                    format("name=%s", name),
                    format("filename='%s'", jfrProfilerOutput.toAbsolutePath())};

            Process dumpJfr = new ProcessBuilder(dumpJfrCommand)
                    .redirectOutput(jfrLog.toFile())
                    .redirectError(jfrLog.toFile())
                    .start();
            int resultCode = dumpJfr.waitFor();
            if (resultCode != 0) {
                throw new RuntimeException(
                        "Bad things happened when dropping JFR recording\n" +
                                "See: " + jfrLog.toAbsolutePath());
            }

            if (!Files.exists(jfrProfilerOutput)) {
                throw new RuntimeException(
                        "A bad thing happened. No JFR profiler recording was created.\n" +
                                "Expected but did not find: " + jfrProfilerOutput.toAbsolutePath());
            }

            // -----------------------------------------------------------------------------------------------
            // -------------------------------------- stop JFR profiler --------------------------------------
            // -----------------------------------------------------------------------------------------------

            String[] stopJfrCommand = {
                    "jcmd",
                    Long.toString(getPid()),
                    "JFR.stop",
                    format("name=%s", name)};

            Process stopJfr = new ProcessBuilder(stopJfrCommand)
                    .redirectOutput(jfrLog.toFile())
                    .redirectError(jfrLog.toFile())
                    .start();
            resultCode = stopJfr.waitFor();
            if (resultCode != 0) {
                throw new RuntimeException(
                        "Bad things happened when stopping JFR profiler\n" +
                                "See: " + jfrLog.toAbsolutePath());
            }
        } catch (Exception e) {
            throw new RuntimeException("Error trying to stop JFR profiler", e);
        }
    }
}