package manga.process.subtitle;

import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * @author PuiWa
 *
 */
public class CustomEdge extends DefaultWeightedEdge {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public String toString() {
		return String.format("(%s->%s, w: %f)", getSource().toString(), getTarget().toString(), getWeight());
//		return String.format("(w: %f)", getWeight());
//		return "";
	}
}
