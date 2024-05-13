import java.lang.ref.WeakReference;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentHashMap;

public final class Cache {
    private static int INT_MAX_COUNT = 5;
    private ConcurrentHashMap<String, RecencyNode> data;
    private PriorityQueue<RecencyNode> recencyNodes;

    private static class RecencyNode implements Comparable<RecencyNode> {
        String key;
        WeakReference<Object> value;
        int priority;

        public RecencyNode(String key, WeakReference<Object> value, int priority) {
            this.key = key;
            this.value = value;
            this.priority = priority;
        }

        @Override
        public int compareTo(RecencyNode other) {
            return Integer.compare(priority, other.priority);
        }

        @Override
        public boolean equals(Object o) {
            RecencyNode node = (RecencyNode) o;
            return this.key.equals(node.key);
        }

        public RecencyNode incr() {
            return new RecencyNode(key, value, priority + 1);
        }
    }

    public Cache() {
        data = new ConcurrentHashMap<>(INT_MAX_COUNT);
        recencyNodes = new PriorityQueue<>();
    }

    public void add(String key, Object value) {
        if (!data.contains(key)) {
            var recencyNode = new RecencyNode(key, new WeakReference<Object>(value), /* priority= */ 0);
            recencyNodes.remove(recencyNode);
            recencyNodes.add(recencyNode);
            data.put(key, recencyNode);
            if (data.size() > INT_MAX_COUNT) {
                evict();
            }
        }
    }

    public Object get(String key) {
        RecencyNode node = data.get(key);
        if (node == null) {
            return null;
        }
        recencyNodes.remove(node);
        recencyNodes.add(node.incr());
        return node.value.get();
    }

    public void invalidateKey(String key) {
        if (data.contains(key)) {
            var nodeRef = data.get(key);
            recencyNodes.remove(nodeRef);
        }
    }

    private void evict() {
        RecencyNode nodeToRemove = recencyNodes.poll();
        data.remove(nodeToRemove.key);
        recencyNodes.remove(nodeToRemove);
    }
}