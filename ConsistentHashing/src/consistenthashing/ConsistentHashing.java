package consistenthashing;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.TreeSet;

public final class ConsistentHashing {

    private TreeSet<Key> keySet = new TreeSet<>();

    public void addKey(String ikey) {
        Key key = Key.of(ikey);
        keySet.add(key);
    }

    public void removeKey(String ikey) {
        Key key = Key.of(ikey);
        if (!keySet.contains(key)) {
            throw new IllegalArgumentException("Key no found");
        }
        keySet.remove(key);
    }

    public Key findNextKey(String ikey) {
        Key currentKey = Key.of(ikey);
        return findNexKey(currentKey);

    }

    public Key findNexKey(Key currentKey) {
        Key foundKey = keySet.higher(currentKey);
        if (foundKey == null) {
            foundKey = keySet.higher(Key.MIN_KEY);
        }
        return foundKey;
    }

    public static class Key implements Comparable<Key> {
        public static Key MIN_KEY = new Key(Integer.MIN_VALUE);
        private int hash;
        private String originalString;

        private Key(String s) {
            this.originalString = s;
            this.hash = hashfn(originalString);
        }

        private Key(Integer i) {
            this.originalString = "Integer Key";
            this.hash = i;
        }

        public static Key of(String ikey) {
            return new Key(ikey);
        }

        public static Key of(Keyable object) {
            return new Key(object.getKeyString());
        }

        public String getOriginalString() {
            return originalString;
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

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public String toString() {
            return originalString + "#" + hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Key) {
                Key other = (Key) obj;
                return this.hash == other.hash;
            }
            return false;
        }

        @Override
        public int compareTo(ConsistentHashing.Key o) {
            return Integer.compare(hash, o.hash);
        }

    }

    public static interface Keyable {
        public String getKeyString();
    }

}
