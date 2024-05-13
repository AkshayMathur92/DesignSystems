import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class CacheRunner {
    public static void main(String[] args) throws Exception {
        List<String> keys = IntStream.range(0, 25).boxed().map(String::valueOf)
                .collect(Collectors.toList());

        for (int i = 0; i < 5; i++) {
            Cache cache = new Cache();
            List<CacheCommand> cacheCommands = prepareRandomCommands(keys);
            execute(i, cacheCommands, cache);
        }

    }

    public static List<CacheCommand> prepareRandomCommands(List<String> keys) {
        List<String> listWithDuplicates = keys.stream().flatMap(s -> Stream.of(s, s, s, s, s, s, s, s, s, s, s))
                .collect(Collectors.toList());
        // shuffle
        Collections.shuffle(listWithDuplicates);
        return CacheCommand.createCommandList(listWithDuplicates);
    }

    public static void execute(int runId, List<CacheCommand> commands, Cache cache) {
        Object globalValue = new Object();
        int hits = 0;
        int miss = 0;
        int totalGetCalls = 0;
        Instant start = Instant.now();
        for (CacheCommand command : commands) {
            if (command.action == 0) {
                // add
                cache.add(command.key, globalValue);
            } else if (command.action == 1) {
                // get
                totalGetCalls++;
                Object found = cache.get(command.key);
                if (found != null) {
                    hits++;
                } else {
                    miss++;
                }
            }
        }
        Instant end = Instant.now();
        long timeTaken = Duration.between(start, end).toNanos();
        int totalcommands = commands.size();
        System.out.println(
                "Test " + runId + " Total get calls: " + totalGetCalls + " Hits:" + hits + " Miss " + miss
                        + " HitRatio:"
                        + (float) hits / totalGetCalls);
        System.out.println("Throughput: " + (float) totalcommands / timeTaken + " commands per nanoseconds");
    }

    public static class CacheCommand {
        int action;
        String key;
        Object value;

        CacheCommand(int action, String key) {
            this.action = action;
            this.key = key;
        }

        public static List<CacheCommand> createCommandList(List<String> keys) {
            Random random = new Random();
            List<CacheCommand> commands = new ArrayList<>();
            for (String key : keys) {
                // if added call get action 2
                commands.add(new CacheCommand(random.nextInt(2), key));
            }
            return commands;
        }
    }
}
