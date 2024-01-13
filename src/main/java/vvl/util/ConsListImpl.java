package vvl.util;

import java.util.Iterator;
import java.util.function.Function;

import org.checkerframework.common.returnsreceiver.qual.This;

public class ConsListImpl<E> implements ConsList<E> {
	
	private final Cons<E, ConsList<E>> list;
	
	public ConsListImpl() {
		this(null);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConsList<E> append(E e) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean isEmpty() {
		return false;
	}

	@Override
	public E car() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ConsList<E> cdr() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int size() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public <T> ConsList<T> map(Function<E, T> f) {
		// TODO Auto-generated method stub
		return null;
	}

}
