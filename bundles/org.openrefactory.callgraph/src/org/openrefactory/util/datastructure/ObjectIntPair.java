package org.openrefactory.util.datastructure;

import java.io.Serializable;

/**
 * An ordered pair <i>(fst, int)</i>.
 *
 * @author Munawar Hafiz
 *
 */
public final class ObjectIntPair<T> implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** Factory method */
    public static <X> ObjectIntPair<X> of(X fst, int snd)
    {
        return new ObjectIntPair<X>(fst, snd);
    }

    public final T fst;
    public final int snd;

    public ObjectIntPair(T fst, int snd)
    {
        this.fst = fst;
        this.snd = snd;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((fst == null) ? 0 : fst.hashCode());
        result = prime * result + snd;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        @SuppressWarnings("rawtypes")
        ObjectIntPair other = (ObjectIntPair)obj;
        if (fst == null) {
            if (other.fst != null) return false;
        } else if (!fst.equals(other.fst)) return false;
        if (snd != other.snd) return false;
        return true;
    }

    @Override
    public String toString()
    {
        return "<" + fst + ", " + snd + ">";
    }
}
