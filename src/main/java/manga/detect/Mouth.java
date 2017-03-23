package manga.detect;

import org.opencv.core.Mat;
import org.opencv.core.Rect;

public class Mouth {
	private Rect bound;
	
	public Mouth(Rect bound) {
		this.bound = bound;
	}
	
	public Mouth(Mouth mouth, double ratio) {
		this.bound = new Rect((int)(mouth.bound.x*ratio), (int)(mouth.bound.y*ratio), (int)(mouth.bound.width*ratio), (int)(mouth.bound.height*ratio));
	}
	
	public Mouth(Mouth mouth, double x, double y) {
		this.bound = new Rect((int) (mouth.bound.x-x), (int) (mouth.bound.y-y), (int)(mouth.bound.width), (int)(mouth.bound.height));
	}

	public Rect getBound() {
		return bound;
	}

	@Override
	public String toString() {
		return "Mouth [bound=" + bound + "]";
	}
}
