package manga.process.subtitle;

/**
 * Helper class of SubtitleCompressor
 * 
 * @author PuiWa
 *
 * @param <X>
 * @param <Y>
 * @param <Z>
 */
public class Triplet<X, Y, Z> {
	private X x;
	private Y y;
	private Z z;
	
	public Triplet(X x, Y y, Z z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public X getWeight() {
		return x;
	}

	public Y getNode() {
		return y;
	}

	@Override
	public String toString() {
		return "Triplet [w=" + x + ", n=" + y + ", id=" + z + "]";
	}

	public X getX() {
		return x;
	}
}
