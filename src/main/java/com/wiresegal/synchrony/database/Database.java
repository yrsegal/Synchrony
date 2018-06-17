package com.wiresegal.synchrony.database;

import gnu.trove.map.hash.TLongObjectHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

/**
 * A database class that binds DatabaseObjects to a backing map of {@linkplain long} IDs.
 *
 * @param <E> The type of database object contained within.
 * @author Wire Segal
 */
public class Database<E extends DatabaseObject> implements Collection<E> {
    @NotNull
    private final TLongObjectHashMap<E> registry = new TLongObjectHashMap<>();
    @NotNull
    private final TObjectLongHashMap<E> reverse = new TObjectLongHashMap<>();
    private long allocation = 0;

    /**
     * Construct a new database.
     */
    public Database() {
        // NO-OP
    }

    /**
     * Construct a database from existing information.
     *
     * @param allocation The total number of allocated values.
     * @param values     The values that were loaded.
     */
    public Database(long allocation, @NotNull Collection<E> values) {
        this.allocation = allocation;
        for (E value : values)
            put(value.id(), value);
    }

    /**
     * @return How many objects this database has allocated.
     * Note that this can be more than the number of values, but never less.
     */
    public long totalAllocated() {
        return allocation;
    }

    /**
     * Registers an object in the registry.
     *
     * @param key The ID to assign.
     * @param e   The object to give that ID.
     * @return Whether the put was successful.
     */
    protected final boolean put(long key, E e) {
        if (registry.contains(key) || reverse.contains(e)) return false;
        e.setId(key);
        if (key > allocation)
            allocation = key;
        registry.put(key, e);
        reverse.put(e, key);
        return true;
    }

    /**
     * @param key The ID to deregister.
     * @return Whether the collection was modified.
     */
    public boolean remove(long key) {
        E e = registry.get(key);
        if (e != null)
            return reverse.remove(e) != reverse.getNoEntryValue() || registry.remove(key) != null;
        return false;
    }

    /**
     * @param id The ID to look up.
     * @return Whether there is an object with the given ID.
     */
    public boolean contains(long id) {
        return registry.contains(id);
    }

    /**
     * @param key The ID to look up.
     * @return The object with that ID, if there is one.
     */
    @Nullable
    public E lookup(long key) {
        return registry.get(key);
    }

    /**
     * @param key The object to look up.
     * @return The ID of that object in this database, if there is one.
     */
    public long lookup(@NotNull E key) {
        return reverse.get(key);
    }

    @Override
    public int size() {
        return registry.size();
    }

    @Override
    public boolean isEmpty() {
        return registry.isEmpty();
    }

    @Override
    public boolean contains(@NotNull Object o) {
        return reverse.contains(o);
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return reverse.keySet().iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return reverse.keys();
    }

    @NotNull
    @Override
    @SuppressWarnings("SuspiciousToArrayCall")
    public <T> T[] toArray(@NotNull T[] a) {
        return reverse.keySet().toArray(a);
    }

    @Override
    public boolean add(E e) {
        return put(allocation++, e);
    }

    @Override
    public boolean remove(@NotNull Object o) {
        long key = reverse.get(o);
        if (key != reverse.getNoEntryValue())
            return reverse.remove(o) != reverse.getNoEntryValue() || registry.remove(key) != null;
        return false;
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return reverse.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        boolean any = false;
        for (E e : c) any |= add(e);
        return any;
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        boolean any = false;
        for (Object e : c) any |= remove(e);
        return any;
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        TLongHashSet retainer = new TLongHashSet();
        for (Object o : c)
            if (reverse.contains(o))
                retainer.add(reverse.get(o));
        Set<E> values = reverse.keySet();
        TLongSet forward = registry.keySet();
        return values.retainAll(c) || forward.retainAll(retainer);
    }

    @Override
    public void clear() {
        registry.clear();
        reverse.clear();
    }

}
