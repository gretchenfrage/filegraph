package filegraph;

public class Box<E> {

	private volatile E obj;
	
	public Box(E obj) {
		this.obj = obj;
	}
	
	public E get() {
		return obj;
	}
	
	public void set(E obj) {
		this.obj = obj;
	}
	
}
