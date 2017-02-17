package manga.process.video;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import org.opencv.core.Mat;

/**
 * @author PuiWa
 * 
 * For storing image extracted from key frame, contains the image and shot-based timestamps i.e. start, current and end
 * 
 */
public class FrameImage {
	private Mat img;
	private long cShotTimestamp;	// timestamp of the selected key frame in a shot
	private long sShotTimestamp;	// start timestamp of the shot 
	private long eShotTimestamp;	// end timestamp of the shot

	public FrameImage(Mat img, long cShotTimestamp, long sShotTimestamp, long eShotTimestamp) {
		this.img = img;
		this.cShotTimestamp = cShotTimestamp;
		this.sShotTimestamp = sShotTimestamp;
		this.eShotTimestamp = eShotTimestamp;
	}

	public Mat getImg() {
		return img;
	}

	public long getcShotTimestamp() {
		return cShotTimestamp;
	}

	public long getsShotTimestamp() {
		return sShotTimestamp;
	}

	public long geteShotTimestamp() {
		return eShotTimestamp;
	}
}
