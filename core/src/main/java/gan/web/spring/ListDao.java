package gan.web.spring;

import java.util.List;

public interface ListDao<E> {
	E 		queryById(String id);
	List<E> list(int offset);
	void  	add(E share);
	int   	count(int index);
	public void delete(String id);
	public void update(E e);
	public List<E> getAll();
}
