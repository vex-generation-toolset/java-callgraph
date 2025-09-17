package org.openrefactory.util.datastructure;

import java.io.Serializable;

/**
 * An ordered pair <i>(int, snd)</i>.
 *
 * @author Munawar Hafiz
 *
 */
public final class IntObjectPair<T> implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** Factory method */
    public static <X> IntObjectPair<X> of(int fst, X snd)
    {
        return new IntObjectPair<X>(fst, snd);
    }

    public final int fst;
    public final T snd;

    public IntObjectPair(int fst, T snd)
    {
        this.fst = fst;
        this.snd = snd;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + fst;
        result = prime * result + ((snd == null) ? 0 : snd.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        @SuppressWarnings("rawtypes")
        IntObjectPair other = (IntObjectPair)obj;
        if (fst != other.fst) return false;
        if (snd == null) {
            if (other.snd != null) return false;
        } else if (!snd.equals(other.snd)) return false;
        return true;
    }

    @Override public String toString()
    {
        return "<" + fst + ", " + snd + ">";
    }
}
