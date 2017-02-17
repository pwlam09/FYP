package manga.detect;

import java.util.ArrayList;

import org.opencv.core.Rect;

public class Speaker {
	private Face face;

	public Speaker(Face face) {
		this.face = face;
	}

	public Face getFace() {
		return face;
	}
}
