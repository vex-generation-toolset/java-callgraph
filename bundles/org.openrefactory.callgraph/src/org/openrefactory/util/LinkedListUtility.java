/*
 * Copyright (C) 2025-present OpenRefactory, Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.openrefactory.util;

import java.util.Comparator;
import java.util.List;

/**
 * Utility method for entering information in a sorted order in a linked list
 *
 * @author Munawar Hafiz
 */
public class LinkedListUtility {

    /**
     * Insert into the linked list in sorted order
     *
     * @param list The linked list
     * @param newItem The new item to insert
     * @param comparator The comparator to use to rank nodes
     *     <p>Insert the item in the linked list in the sorted position, if the item is already in the list, no
     *     insertion done
     */
    public static <T> void insertIntoList(List<T> list, T newItem, Comparator<T> comparator) {
        int compResult = 0;
        for (int i = 0; i < list.size(); i++) {
            T temp = list.get(i);

            compResult = comparator.compare(newItem, temp);
            if (compResult == -1) {
                // If the new item is smaller, insert at this position
                list.add(i, newItem);
                return;
            } else if (compResult == 0) {
                return;
            } else {
                // The new item is larger, continue exploring
            }
        }
        // insert at the end of the list, since this is the largest entry so far
        list.add(newItem);
    }

    /**
     * Insert into the linked list in sorted order
     *
     * @param list The linked list
     * @param newItem The new item to insert
     *     <p>Insert the item in the linked list in the sorted position, if the item is already in the list, no
     *     insertion done
     */
    public static <T extends Comparable<T>> void insertIntoList(List<T> list, T newItem) {
        int compResult = 0;
        for (int i = 0; i < list.size(); i++) {
            T temp = list.get(i);

            compResult = newItem.compareTo(temp);
            if (compResult == -1) {
                // If the new item is smaller, insert at this position
                list.add(i, newItem);
                return;
            } else if (compResult == 0) {
                return;
            } else {
                // The new item is larger, continue exploring
            }
        }
        // insert at the end of the list, since this is the largest entry so far
        list.add(newItem);
    }
}
