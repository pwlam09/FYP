package manga.detect;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import pixelitor.filters.comp.Resize;

public class Face {
	private Mat img;
	private Rect bound;
	private Mouth mouth;
	
	public Face(Mat img, Rect bound, Mouth mouth) {
		this.img = img;
		this.bound = bound;
		this.mouth = mouth;
	}

	public Face(Face face, double ratio) {
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

	public Mouth getMouth() {
		return mouth;
	}

	public Mat getImg() {
		return img;
	}

	public Rect getBound() {
		return bound;
	}

	@Override
	public String toString() {
		return "Face [img=" + img + ", bound=" + bound + ", mouth=" + mouth + "]";
	}
}
