package org.openrefactory.util.datastructure;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe LRU (Least Recently Used) cache implementation.
 *
 * <p>This class provides a cache with automatic eviction of least recently used entries
 * when the capacity is exceeded. It uses read-write locks for thread safety and
 * LinkedHashMap for maintaining insertion order.</p>
 *
 * @param <K> the type of keys maintained by this cache
 * @param <V> the type of mapped values
 */
public class NeoLRUCache<K, V> {
    private final Map<K, V> cache;
    private volatile int cacheSize;
    private ReadWriteLock rwl;

    public NeoLRUCache(int capacity) {
        this.cacheSize = capacity;
        this.rwl = new ReentrantReadWriteLock();
        this.cache = new LinkedHashMap<K, V>(this.cacheSize){

            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry(Entry<K, V> eldest) {
                return this.size() > capacity;
            }
        };
    }

    public NeoLRUCache(int capacity, Map<K, V> cache) {
        this.cacheSize = capacity;
        this.rwl = new ReentrantReadWriteLock();
        this.cache = cache;
    }

    public boolean contains(K key) {
        try {
            rwl.readLock().lock();
            return this.cache.containsKey(key);
        } finally {
            rwl.readLock().unlock();
        }
    }

    public V get(K key) {
        try {
            rwl.readLock().lock();
            return this.cache.get(key);
        } finally {
            rwl.readLock().unlock();
        }
    }

    public void remove(K key) {
        try {
            rwl.writeLock().lock();
            this.cache.remove(key);
        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public Collection<V> getValues() {
        try {
            rwl.readLock().lock();
            return this.cache.values();
        } finally {
            rwl.readLock().unlock();
        }
    }

    public Collection<K> getKeys() {
        try {
            rwl.readLock().lock();
            return this.cache.keySet();
        } finally {
            rwl.readLock().unlock();
        }
    }

    // put data in cache
    public void cache(K key, V data){
        // check key is exist of not if exist than remove and again add to make it recently used
        // remove element if window size is exhaust
        // To avoid lock downgrade, we are checking whether the
        // key is present outside of the write Lock
        if(contains(key)){
            rwl.writeLock().lock();
            try {
                cache.remove(key);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                rwl.writeLock().unlock();
            }
        }
        rwl.writeLock().lock();
        try {
            cache.put(key,data);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            rwl.writeLock().unlock();
        }
    }

    public void clear()
    {
        rwl.writeLock().lock();
        try {
            cache.clear();
        } finally {
            rwl.writeLock().unlock();
        }
    }

}