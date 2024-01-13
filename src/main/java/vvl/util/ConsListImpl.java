package vvl.util;

import java.util.Iterator;
import java.util.function.Function;

import org.checkerframework.common.returnsreceiver.qual.This;

public class ConsListImpl<E> implements ConsList<E> {
	
	private final Cons<E, ConsList<E>> list;

	public ConsListImpl() {
		this.list = null;
	}
	
	public ConsListImpl(Cons<E, ConsList<E>> list) {
		this.list = list;
	}

	@Override
	public Iterator<E> iterator() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConsList<E> prepend(E e) {
		return new ConsListImpl<>(new Cons<>(e, this));
	}
	
	@Override
	public ConsList<E> append(E e) {
	    if (isEmpty()) {
	        return new ConsListImpl<>(new Cons<>(e, ConsList.nil()));
	    } else {
	        return new ConsListImpl<>(new Cons<>(car(), cdr().append(e)));
	    }
	}
	
	@Override
	public boolean isEmpty() {
		return this.list == null;
	}

	@Override
    public E car() {
        if (list == null) {
            throw new UnsupportedOperationException("car() called on an empty list");
        }
        return list.left();
    }

	@Override
	public ConsList<E> cdr() {
        if (list == null) {
            throw new UnsupportedOperationException("cdr() called on an empty list");
        }
        return list.right();
    }

	@Override
	public int size() {
		int cpt = 0;
	    ConsList<E> curr = this;
	    while (!curr.isEmpty()) {
	    	cpt++;
	        curr = curr.cdr();
	    }
	    return cpt;	
	}

	@Override
	public <T> ConsList<T> map(Function<E, T> f) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return super.equals(obj);
	}

}
