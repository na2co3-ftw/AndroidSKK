package jp.gr.java_conf.na2co3.skk.engine;

import java.util.HashMap;
import java.util.Map;

class RomajiMap {
    private Map<String, Node> mMap = new HashMap<>();

    static class Node {
        private String key;
        private String value;
        private String next;
        private boolean leaf;

        Node(String key, String value, String next, boolean leaf) {
            this.key = key;
            this.value = value;
            this.next = next;
            this.leaf = leaf;
        }

        String getKey() { return key; }
        String getValue() { return value; }
        String getNext() { return next; }
        boolean isLeaf() { return leaf; }
    }

    void put(String key, String value) {
        put(key, value, null);
    }

    void put(String key, String value, String next) {
        Node oldNode = mMap.get(key);

        if (oldNode == null) {
            mMap.put(key, new Node(key, value, next, true));

            for (int i = key.length() - 1; i > 0; i--) {
                String prefix = key.substring(0, i);
                Node prefixNode = mMap.get(prefix);
                if (prefixNode != null) {
                    break;
                }
                mMap.put(prefix, new Node(prefix, null, null, false));
            }
        } else {
            mMap.put(key, new Node(key, value, next, oldNode.leaf));
        }
    }

    Node prefixSearch(String key) {
        for (int i = key.length(); i > 0; i--) {
            String prefix = key.substring(0, i);
            Node node = mMap.get(prefix);
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}
