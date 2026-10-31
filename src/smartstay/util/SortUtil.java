package smartstay.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Merge sort, written by hand to show the sorting algorithm clearly.
 * Used to sort rooms by price and reservations by check-in date.
 *
 * How it works:
 *  1. Split the list into two halves.
 *  2. Sort each half (by calling mergeSort again - recursion).
 *  3. Merge the two sorted halves by repeatedly taking the smaller front item.
 *
 * Time complexity:  O(n log n) in every case.
 * Space complexity: O(n) for the temporary lists.
 * It is "stable": items that compare equal keep their original order.
 */
public final class SortUtil {

    private SortUtil() {
        // Utility class - no objects needed.
    }

    /** Returns a NEW sorted list; the original list is not changed. */
    public static <T> List<T> mergeSort(List<T> items, Comparator<? super T> comparator) {
        if (items.size() <= 1) {
            return new ArrayList<>(items);
        }
        int middle = items.size() / 2;
        List<T> left = mergeSort(items.subList(0, middle), comparator);
        List<T> right = mergeSort(items.subList(middle, items.size()), comparator);
        return merge(left, right, comparator);
    }

    private static <T> List<T> merge(List<T> left, List<T> right, Comparator<? super T> comparator) {
        List<T> merged = new ArrayList<>(left.size() + right.size());
        int i = 0;
        int j = 0;
        while (i < left.size() && j < right.size()) {
            // "<= 0" keeps the sort stable.
            if (comparator.compare(left.get(i), right.get(j)) <= 0) {
                merged.add(left.get(i++));
            } else {
                merged.add(right.get(j++));
            }
        }
        while (i < left.size()) {
            merged.add(left.get(i++));
        }
        while (j < right.size()) {
            merged.add(right.get(j++));
        }
        return merged;
    }
}
