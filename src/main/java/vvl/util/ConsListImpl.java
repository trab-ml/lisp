package vvl.util;

import java.util.Iterator;
import java.util.function.Function;

import org.checkerframework.common.returnsreceiver.qual.This;

public class ConsListImpl<E> implements ConsList<E> {
	
	private final Cons<E, ConsList<E>> list;
	private int size = 0;

	public ConsListImpl() {
		this.list = null;
	}
	
	public ConsListImpl(Cons<E, ConsList<E>> list) {
		this.list = list;
		this.size = computeSize();
	}
	
	private int computeSize() {
	    int cpt = 0;
	    ConsList<E> current = this;
	    while (!current.isEmpty()) {
	    	cpt++;
	        current = current.cdr();
	    }
	    return cpt;
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
		return null;
    }
	
	@Override
	public boolean isEmpty() {
//		System.out.println("this.list --> " + this.list);
//		System.out.println("ConsList.nil() --> " + ConsList.nil());
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
//		System.out.println("ConsList current size: " + size);
		return size;
	}

	@Override
	public <T> ConsList<T> map(Function<E, T> f) {
		// TODO Auto-generated method stub
		return null;
	}

}
