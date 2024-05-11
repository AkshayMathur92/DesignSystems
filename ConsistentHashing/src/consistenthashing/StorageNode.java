package consistenthashing;

import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public final class StorageNode {
    private String name;
    private TreeMap<String, String> dataStore = new TreeMap<>();

    public StorageNode(String name) {
        this.name = name;
    }

    public void addAllData(SortedMap<String, String> data) {
        dataStore.putAll(data);
    }

    public SortedMap<String, String> deleteDataBeforeRange(String objectKey) {
        SortedMap<String, String> dataToDelete = new TreeMap<>(dataStore.headMap(objectKey));
        dataToDelete.keySet().stream().forEach(i -> dataStore.remove(i));
        return dataToDelete;
    }

    public SortedMap<String, String> getAllData() {
        return dataStore;
    }

    public void addData(String key, String value) {
        dataStore.put(key, value);
    }

    public String getData(String key) {
        return dataStore.get(key);
    }

    @Override
    public String toString() {
        return name;
    }

    public void printNumberLine() {
        for (Map.Entry<String, String> entries : dataStore.entrySet()) {
            System.out.print("\t");
            System.out.println("Key " + entries.getKey() + " with value " + entries.getValue());
        }
    }
}