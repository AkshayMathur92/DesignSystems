import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class ConsistentHashingDemo {

    private static final class StorageNode {
        Key key;
        String name;
        TreeMap<Key, String> dataStore = new TreeMap<>();

        public StorageNode(String name) {
            this.name = name;
            this.key = new Key(name);
        }

        public void addAllData(SortedMap<Key, String> data) {
            dataStore.putAll(data);
        }

        public SortedMap<Key, String> deleteDataBeforeRange(Key nodeKey) {
            SortedMap<Key, String> dataToDelete = new TreeMap<>(dataStore.headMap(nodeKey));
            dataToDelete.keySet().stream().forEach(i -> dataStore.remove(i));
            return dataToDelete;
        }

        public void addData(Key key, String value) {
            dataStore.put(key, value);
        }

        public String getData(Key key) {
            return dataStore.get(key);
        }

        @Override
        public int hashCode() {
            return key.hashCode();
        }

        @Override
        public String toString() {
            return name;
        }

        public void printNumberLine() {
            for (Map.Entry<Key, String> entries : dataStore.entrySet()) {
                System.out.println("Key " + entries.getKey() + " with value " + entries.getValue());
            }
        }
    }

    private static final class Key implements Comparable<Key> {
        int hash;
        String originalValue;

        public static Key MIN_KEY = new Key(Integer.MIN_VALUE);

        Key(String s) {
            this.originalValue = s;
            this.hash = hashfn(originalValue);
        }

        Key(Integer i) {
            this.originalValue = "NA";
            this.hash = i;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return originalValue + "#" + hash;
        }

        @Override
        public int compareTo(Key otherKey) {
            return Integer.compare(this.hash, otherKey.hash);
        }
    }

    private static int hashfn(String text) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            return text.hashCode();
        }
        byte[] hash = md.digest(text.getBytes(StandardCharsets.UTF_8));
        // converting the HEX digest into equivalent integer value
        final StringBuilder hexString = new StringBuilder();
        for (int i = 0; i < hash.length; i++) {
            final String hex = Integer.toHexString(0xff & hash[i]);
            if (hex.length() == 1)
                hexString.append('0');
            hexString.append(hex);
        }
        return new BigInteger(hexString.toString(), 16).intValue();
    }

    private TreeMap<Key, StorageNode> nodeTreeMap = new TreeMap<>();

    public void put(String ikey, String value) {
        Key key = new Key(ikey);
        System.out.println("Adding value " + value + " for key " + key);
        StorageNode node = getForwardNode(key);
        node.addData(key, value);
    }

    public String get(String ikey) {
        Key key = new Key(ikey);
        StorageNode node = getForwardNode(key);
        String value = node.getData(key);
        System.out.println("Got value " + value + "for key" + key);
        return value;
    }

    public void addNode(String nodeName) {
        StorageNode newNode = new StorageNode(nodeName);
        System.out.println("Adding node " + newNode);
        if (nodeTreeMap.size() == 0) {
            nodeTreeMap.put(new Key(nodeName), newNode);
            return;
        }
        StorageNode dataToMoveFrom = getForwardNode(newNode.key);
        SortedMap<Key, String> dataToMove = dataToMoveFrom.deleteDataBeforeRange(newNode.key);
        newNode.addAllData(dataToMove);
        nodeTreeMap.put(newNode.key, newNode);
    }

    public void removeNode(String nodeName) {
        StorageNode nodeToRemove = nodeTreeMap.get(new Key(nodeName));
        System.out.println("Removing node " + nodeToRemove);
        if (nodeToRemove == null) {
            throw new RuntimeException("Unknown Node" + nodeName);
        }
        if (nodeTreeMap.size() == 1) {
            System.err.println("Deleting last node, all data is deleted");
            nodeTreeMap.remove(nodeToRemove.key);
            return;
        }
        StorageNode dataToMoveTo = getForwardNode(nodeToRemove.key);
        dataToMoveTo.addAllData(nodeToRemove.dataStore);
        nodeTreeMap.remove(nodeToRemove.key);
    }

    private StorageNode getForwardNode(Key key) {
        Map.Entry<Key, StorageNode> nodeFound = nodeTreeMap.higherEntry(key);
        if (nodeFound == null) {
            nodeFound = nodeTreeMap.ceilingEntry(Key.MIN_KEY);
        }
        return nodeFound.getValue();
    }

    void printNumberLine() {
        for (StorageNode node : nodeTreeMap.values()) {
            System.out.println("Key " + node.key + " associate with Node " + node.name);
            node.printNumberLine();
        }
    }

    public static void main(String... s) {
        ConsistentHashingDemo demo = new ConsistentHashingDemo();
        demo.addNode("Node 1");
        demo.printNumberLine();
        demo.put("One", "One");
        demo.printNumberLine();
        demo.put("Two", "Two");
        demo.printNumberLine();
        demo.put("Three", "Three");
        demo.printNumberLine();
        demo.addNode("Node 2");
        demo.printNumberLine();
        demo.addNode("Node 3");
        demo.printNumberLine();
        demo.get("One");
        demo.printNumberLine();
        demo.get("Two");
        demo.printNumberLine();
        demo.get("Three");
        demo.printNumberLine();
        demo.removeNode("Node 2");
        demo.printNumberLine();
        demo.get("One");
        demo.printNumberLine();
        demo.get("Two");
        demo.printNumberLine();
        demo.get("Three");
        demo.printNumberLine();
        demo.addNode("Node 4");
        demo.get("One");
        demo.get("Two");
        demo.get("Three");
        demo.printNumberLine();
        demo.removeNode("Node 1");
        demo.printNumberLine();
        demo.removeNode("Node 3");
        demo.printNumberLine();
        demo.get("One");
        demo.get("Two");
        demo.get("Three");
        demo.removeNode("Node 4");
        demo.printNumberLine();
    }
}