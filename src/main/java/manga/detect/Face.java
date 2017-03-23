package manga.detect;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;


/**
 * @author PuiWa
 *
 */
public class Face {
	private int frameIndex;
	private Mat img;
	private Rect bound;
	private Mouth mouth;
	
	public Face(int frameIndex, Mat img, Rect bound, Mouth mouth) {
		this.frameIndex = frameIndex;
		this.img = img;
		this.bound = bound;
		this.mouth = mouth;
	}

	/**
	 * For re-scaling face
	 * @param face
	 * @param ratio
	 */
	public Face(Face face, double ratio) {
		this.frameIndex = face.frameIndex;
		Mat resizedImg = new Mat();
		Size sz = new Size(face.img.width()*ratio, face.img.height()*ratio);
		Imgproc.resize(face.img, resizedImg, sz);
		this.img = resizedImg;
		this.bound = new Rect((int)(face.bound.x*ratio), (int)(face.bound.y*ratio), (int)(face.bound.width*ratio), (int)(face.bound.height*ratio));
		if (face.mouth != null) {
			this.mouth = new Mouth(face.mouth, ratio);
		} else {
			this.mouth = null;
		}
	}
	
	/**
	 * For setting new bound top-left coordinates.
	 * Translate by (x,y).
	 * @param face
	 * @param newBound
	 */
	public Face(Face face, double x, double y) {
		this.frameIndex = face.frameIndex;
		this.img = face.img;
		this.bound = new Rect((int) (face.bound.x-x), (int) (face.bound.y-y), face.bound.width, face.bound.height);
		if (face.mouth != null) {
			this.mouth = new Mouth(face.mouth, x, y);
		} else {
			this.mouth = null;
		}
	}

	public Mouth getMouth() {
		return mouth;
	}

	public Mat getImg() {
		return img;
	}

	public Rect getBound() {
		return bound;
	}

	/**
	 * @param face face with smaller frame index
	 * @return whether the face follows another
	 */
	public boolean follows(Face face) {
		// if in consecutive frames, the frame index should be varied by 1
		if (this.frameIndex-face.frameIndex == 1) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toString() {
		return "Face [img=" + img + ", bound=" + bound + ", mouth=" + mouth + "]";
	}
}
