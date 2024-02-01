package vvl.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

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
		return new Iterator<E>() {
			private ConsList<E> current = ConsListImpl.this;
			
			@Override
			public boolean hasNext() {
				if (current == null) {
					return false;
				}
				
				return !current.isEmpty();
			}

			@Override
			public E next() throws NoSuchElementException {
				try {
					E eltval = current.car();
					current = current.cdr(); // move the cursor
					return eltval;
				} catch (NoSuchElementException e) {
					throw new NoSuchElementException();
				}
			}
		};
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
			Cons<E, ConsList<E>> nwCons = new Cons<>(car(), cdr().append(e));
			return new ConsListImpl<>(nwCons);
		}
	}

	@Override
	public boolean isEmpty() {
		return this.list == null;
	}

	@Override
	public E car() {
		if (list == null) {
			throw new NoSuchElementException("car() called on an empty list");
		}
		return list.left();
	}

	@Override
	public ConsList<E> cdr() {
		if (list == null) {
			return ConsList.nil();
		}
		return list.right();
	}

	@Override
	public int size() {
		int cpt = 0;
		ConsList<E> curr = this;
		while (curr != null && !curr.isEmpty()) {
			cpt++;
			curr = curr.cdr();
		}
		return cpt;
	}

	@Override
	public <T> ConsList<T> map(Function<E, T> f) {
		if (isEmpty()) {
			return ConsList.nil();
		} else {
			return new ConsListImpl<>(new Cons<>(f.apply(car()), cdr().map(f)));
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
            return true;
		if (obj == null || getClass() != obj.getClass())
            return false;
		
		ConsListImpl<?> other = (ConsListImpl<?>) obj;
		Iterator<E> it1 = this.iterator();
		Iterator<?> it2 = other.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			E e1 = it1.next();
			Object e2 = it2.next();
			if (e1 == null ? e2 != null : !e1.equals(e2)) {
				return false;
			}
		}
		return !it1.hasNext() && !it2.hasNext();
	}
	
	@Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + list.hashCode();
        return result;
    }

	@Override
	public String toString() {
		int cpt = 0;
		StringBuilder builder = new StringBuilder("(");
		if(!isEmpty()) {
			Iterator<E> it = iterator();
			while (it.hasNext()) {
				if (cpt > 0) {
					builder.append(" ");
				}
				cpt++;
				builder.append(it.next());
			}
		}
		builder.append(")");
		return builder.toString();
	}

}
