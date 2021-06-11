package application;

import org.opencv.core.Rect;

public class Values {

	private double lod, lid, sod, sid;
	private Rect rectangle;

	public Values(double outer_d1, double inner_d1, double outer_d2, double inner_d2, Rect r) {
		if(outer_d1 >= outer_d2) {
			this.lod = outer_d1;
			this.sod = outer_d2;
			this.lid = inner_d1;
			this.sid = inner_d2;
		}else {
			this.lod = outer_d2;
			this.sod = outer_d1;
			this.lid = inner_d2;
			this.sid = inner_d1;
		}

		this.rectangle = r;

	}

	public double getLongerOuterDiameter() {
		return lod;
	}
	public double getLongerInnerDiameter() {
		return lid;
	}
	public double getShorterOuterDiameter() {
		return sod;
	}
	public double getShorterinnerDiameter() {
		return sid;
	}

	public double getSectionModulus() {
		double ans;
		ans = (Math.PI / 32) * (Math.pow(sod, 3.0) * lod - Math.pow(sid, 3.0) * lid) / sod;
		return ans;
	}

	public Rect getRectangle() {
		return rectangle;
	}

}
