package gan.web.spring;

import java.util.Collection;

public interface ListService<E> {
	E 				getById(String id);
	void			add(E item);
	Collection<E>   list(int offset);
	int 			count();
	
	public void 	delete(String id);
	public Collection<E> 	getAll();
}
