package manga.process.video;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.ArrayList;

import org.opencv.core.Mat;

/**
 * @author PuiWa
 * 
 * For storing image extracted from key frame, contains the image and shot-based timestamps i.e. start, current and end
 * 
 */
public class KeyFrame {
	private Mat img;
	private double cShotTimestamp;	// timestamp of the selected key frame in a shot
	private double sShotTimestamp;	// start timestamp of the shot 
	private double eShotTimestamp;	// end timestamp of the shot

	public KeyFrame(Mat img, double cShotTimestamp, double sShotTimestamp, double eShotTimestamp) {
		this.img = img;
		this.cShotTimestamp = cShotTimestamp;
		this.sShotTimestamp = sShotTimestamp;
		this.eShotTimestamp = eShotTimestamp;
	}

	public Mat getImg() {
		return img;
	}

	public double getcShotTimestamp() {
		return cShotTimestamp;
	}

	public double getsShotTimestamp() {
		return sShotTimestamp;
	}

	public double geteShotTimestamp() {
		return eShotTimestamp;
	}

	@Override
	public String toString() {
		return "KeyFrame [img=" + img + ", cShotTimestamp=" + cShotTimestamp + ", sShotTimestamp=" + sShotTimestamp
				+ ", eShotTimestamp=" + eShotTimestamp + "]";
	}
}
