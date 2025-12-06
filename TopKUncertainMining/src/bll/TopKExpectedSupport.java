package bll;

import java.util.*;
import model.Transaction;
import model.Itemset;

public class TopKExpectedSupport {

    private final List<Transaction> transactions;
    private final int k;
    private final List<String> allItems;

    private final PriorityQueue<Itemset> topKQueue;

    private double minES = 0.0;

    public TopKExpectedSupport(List<Transaction> transactions, int k) {
        this.transactions = transactions;
        this.k = k;

        Map<String, Double> esMap = new HashMap<>();
        for (Transaction t : transactions) {
            for (Map.Entry<String, Double> e : t.getItems().entrySet()) {
                esMap.merge(e.getKey(), e.getValue(), Double::sum);
            }
        }

        List<String> items = new ArrayList<>(esMap.keySet());
        items.sort((a, b) -> Double.compare(esMap.get(b), esMap.get(a)));
        this.allItems = items;

        this.topKQueue = new PriorityQueue<>(Comparator.comparingDouble(Itemset::getExpectedSupport));
    }

    public List<Itemset> findTopK() {
        explore(0, new LinkedHashSet<>(), 0.0);

        List<Itemset> result = new ArrayList<>(topKQueue);
        result.sort((a, b) -> Double.compare(b.getExpectedSupport(), a.getExpectedSupport()));
        return result;
    }

    private void explore(int index, Set<String> curr, double currES) {

        for (int i = index; i < allItems.size(); i++) {
            String item = allItems.get(i);

            // UB = ES(item)
            double ub = calculateSingleES(item);

            if (ub < minES) {
                continue;
            }

            curr.add(item);

            // Tính exact ES
            double es = calculateExpectedSupport(curr);

            if (es >= minES) {
                pushTopK(new Itemset(new LinkedHashSet<>(curr), es));
            }

            if (es >= minES) {
                explore(i + 1, curr, es);
            }

            curr.remove(item);
        }
    }

    // ------------------------------------------------------------
    // ------------------------------------------------------------
    private void pushTopK(Itemset it) {
        if (topKQueue.size() < k) {
            topKQueue.add(it);
        } else if (it.getExpectedSupport() > topKQueue.peek().getExpectedSupport()) {
            topKQueue.poll();
            topKQueue.add(it);
        }

        if (topKQueue.size() == k) {
            minES = topKQueue.peek().getExpectedSupport();
        }
    }

    private double calculateSingleES(String item) {
        double sum = 0.0;
        for (Transaction t : transactions) {
            Double v = t.getItems().get(item);
            if (v != null) {
                sum += v;
            }
        }
        return sum;
    }

    private double calculateExpectedSupport(Set<String> itemset) {
        double sum = 0.0;
        for (Transaction t : transactions) {
            double p = 1.0;
            for (String item : itemset) {
                Double val = t.getItems().get(item);
                if (val == null) {
                    p = 0.0;
                    break;
                }
                p *= val;
            }
            sum += p;
        }
        return sum;
    }
}
