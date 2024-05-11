package consistenthashing;

import java.util.SortedMap;
import java.util.TreeMap;

public final class NodeCluster {
    private ConsistentHashing ring = new ConsistentHashing();
    private TreeMap<ConsistentHashing.Key, StorageNode> nodes = new TreeMap<>();

    public void putObject(String ikey, String value) {
        StorageNode candidateNode = nodes.get(ring.findNextKey(ikey));
        System.out.println("Adding value " + value + " for key " + ikey + " to Node " + candidateNode);
        candidateNode.addData(ikey, value);
    }

    public String getObject(String ikey) {
        StorageNode candidateNode = nodes.get(ring.findNextKey(ikey));
        String value = candidateNode.getData(ikey);
        System.out.println("Got value " + value + " for key " + ikey + " from Node " + candidateNode);
        return value;
    }

    public void addNode(String nodeName) {
        StorageNode newNode = new StorageNode(nodeName);
        ConsistentHashing.Key key = ConsistentHashing.Key.of(nodeName);
        System.out.println("Adding node " + newNode);
        nodes.put(key, newNode);
        ring.addKey(nodeName);
        if (nodes.size() == 1) {
            return;
        }
        StorageNode dataToMoveFrom = nodes.get(ring.findNextKey(nodeName));
        SortedMap<String, String> dataToMove = dataToMoveFrom.deleteDataBeforeRange(key.getOriginalString());
        newNode.addAllData(dataToMove);
    }

    public void removeNode(String nodeName) {
        ConsistentHashing.Key key = ConsistentHashing.Key.of(nodeName);
        StorageNode nodeToRemove = nodes.get(key);
        System.out.println("Removing node " + nodeToRemove);
        if (nodeToRemove == null) {
            throw new RuntimeException("Unknown Node" + nodeName);
        }
        if (nodes.size() == 1) {
            System.err.println("Deleting last node, all data is deleted");
            nodes.remove(key);
            return;
        }
        StorageNode dataToMoveTo = nodes.get(ring.findNexKey(key));
        dataToMoveTo.addAllData(nodeToRemove.getAllData());
        nodes.remove(key);
        ring.removeKey(nodeName);
    }

    void printNumberLine() {
        for (StorageNode node : nodes.values()) {
            System.out.println("Node : " + node);
            node.printNumberLine();
        }
    }
}