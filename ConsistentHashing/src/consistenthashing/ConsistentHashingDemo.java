package consistenthashing;

public final class ConsistentHashingDemo {

    public static void main(String... s) {
        NodeCluster cluster = new NodeCluster();
        cluster.addNode("Node 1");
        cluster.addNode("Node 2");
        cluster.addNode("Node 3");

        cluster.putObject("One", "One");
        cluster.putObject("Two", "Two");
        cluster.putObject("Three", "Three");
        cluster.putObject("Four", "Four");
        cluster.putObject("Five", "Five");
        cluster.putObject("Six", "Six");
        cluster.printNumberLine();

        cluster.addNode("Node 4");
        cluster.printNumberLine();

        cluster.removeNode("Node 2");
        cluster.printNumberLine();

        cluster.removeNode("Node 1");
        cluster.printNumberLine();
        cluster.removeNode("Node 3");
        cluster.printNumberLine();
        cluster.removeNode("Node 4");
        cluster.printNumberLine();
    }
}