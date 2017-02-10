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

	public BufferedImage getImg() {
		return Mat2BufferedImage(img);
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
	
	/**
	 * @param m Mat of extracted frame
	 * @return BufferedImage of Mat
	 */
	private static BufferedImage Mat2BufferedImage(Mat m) {
		// Fastest code
		// output can be assigned either to a BufferedImage or to an Image
		
		int type = BufferedImage.TYPE_BYTE_GRAY;
		if ( m.channels() > 1 ) {
		    type = BufferedImage.TYPE_3BYTE_BGR;
		}
		int bufferSize = m.channels()*m.cols()*m.rows();
		byte [] b = new byte[bufferSize];
		m.get(0,0,b); // get all the pixels
		BufferedImage image = new BufferedImage(m.cols(),m.rows(), type);
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(b, 0, targetPixels, 0, b.length);  
		return image;
	}
}
