package application;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class SMeasureController {
	@FXML
	private BorderPane anchor;
	@FXML
	private ImageView imageview;
	@FXML
	private Label filename, os, ol, is, il, sm, unit, underLabel;
	@FXML
	private Button browse, backbutton, nextbutton, renewButton;
	@FXML
	private CheckBox binary, adjust;
	@FXML
	private Slider blurSlider, sensitivitySlider;
	@FXML
	private TextField textfield1;
	@FXML
	private TableView<TableValue> table;
	@FXML
	private TableColumn<TableValue, String> colname;
	@FXML
	private TableColumn<TableValue, Integer> col0;
	@FXML
	private TableColumn<TableValue, String> col1, col2, col3, col4;

	private File nowfile;
	private int nowfileindex;
	private File nowdirectory;
	private List<File> filelist = new ArrayList<File>();
	private File savefile;
	private File picturefile;

	private List<Values> values = new ArrayList<Values>();
	private List<Rect> recordResions = new ArrayList<Rect>();
	private Mat processedImage;
	private Mat blackImage;
	private Mat resizedImage;
	private Mat submat;
	private int blursize;
	private double thresh;

	private Point location;
	private double zoom;

	private Point initPoint;
	private boolean dragging;
	private Values currentSelectedValues;
	private int currentSelectedRow;

	private double mmperpx;

//////////////////////////////////////////////
	private final int min_cnt = 1000;
	private final int block = 80;
//////////////////////////////////////////////

	protected void init() {
		resizedImage = new Mat();
		blursize = (int)(blurSlider.getValue() * 2);
		thresh = (11.0 - sensitivitySlider.getValue()) * 2;
		location = new Point(0,0);
		zoom = 1.0;
		nowfileindex = 0;
		initPoint = new Point(0, 0);
		dragging = false;
		currentSelectedRow = 1;
		mmperpx = 1.0;

		imageview.setFitWidth(anchor.getWidth() - 330);
		imageview.setFitHeight(anchor.getHeight() - 100);

		colname.setCellValueFactory(new PropertyValueFactory<TableValue, String>("name"));
        col0.setCellValueFactory(new PropertyValueFactory<TableValue, Integer>("id"));
        col1.setCellValueFactory(new PropertyValueFactory<TableValue, String>("os"));
        col2.setCellValueFactory(new PropertyValueFactory<TableValue, String>("ol"));
        col3.setCellValueFactory(new PropertyValueFactory<TableValue, String>("is"));
        col4.setCellValueFactory(new PropertyValueFactory<TableValue, String>("il"));

	}

	private void processImage() {
		this.underLabel.setText("計算中…");

		values = new ArrayList<Values>();
		this.currentSelectedValues = null;
		this.location = new Point(0, 0);

		Mat sampleImage;
		sampleImage = Imgcodecs.imread(nowfile.getAbsolutePath());//BGR

		List<Mat> rgbImage = new ArrayList<Mat>();
		Core.split(sampleImage, rgbImage);

		Mat substracted = new Mat();
		Core.subtract(rgbImage.get(0), rgbImage.get(1), substracted);

		Mat binaryImage = new Mat();
		Imgproc.threshold(substracted, binaryImage, thresh, 255.0, Imgproc.THRESH_BINARY);

		Mat blurImage = new Mat();
		Imgproc.blur(binaryImage, blurImage, new Size(this.blursize, this.blursize));

		Mat adjusted = new Mat();
		Imgproc.dilate(blurImage, adjusted, new Mat(), new Point(-1, -1), 1);
		Imgproc.erode(adjusted, adjusted, new Mat(), new Point(-1, -1), 2);
		Imgproc.threshold(adjusted, adjusted, 128.0, 255.0, Imgproc.THRESH_BINARY);

		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> l_contours = new ArrayList<MatOfPoint>();
		List<MatOfPoint> outer_l_contours = new ArrayList<MatOfPoint>();
		Mat hiert = new Mat();
		Mat contourImage;
		Imgproc.findContours(adjusted, contours, hiert, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
		contourImage = Mat.zeros(new Size(sampleImage.width(),sampleImage.height()),CvType.CV_8UC3);
		for(int i = 0; i<contours.size(); i++) {
			if(Imgproc.contourArea(contours.get(i)) > min_cnt){
				MatOfPoint mp = contours.get(i);
				l_contours.add(mp);
				if(hiert.get(0, i)[3] == -1.0) {
					outer_l_contours.add(mp);
				}

			}
		}
        Imgproc.drawContours(contourImage, l_contours, -1, new Scalar(255,255,255),5);
        for(int i=0; i<l_contours.size(); i++) {
        	MatOfPoint2f mop2 = new MatOfPoint2f(l_contours.get(i).toArray());
        	RotatedRect rr = Imgproc.minAreaRect(mop2);
	        Point[] corner = new Point[4];
	        rr.points(corner);
        	for (int j = 0; j < 4; j++){
	            Imgproc.line(contourImage, corner[j], corner[(j+1)%4], new Scalar(0,255,0), 4);
	        }
        }

		Mat masureImage;
        masureImage = sampleImage.clone();
        //Imgproc.cvtColor(masureImage, masureImage, Imgproc.COLOR_GRAY2BGR);
        for(int i=0; i<outer_l_contours.size(); i++) {
        	double[] od = new double[2];
        	double[] id = new double[2];

        	MatOfPoint2f mop2 = new MatOfPoint2f(outer_l_contours.get(i).toArray() );
        	RotatedRect rr = Imgproc.minAreaRect(mop2);
	        Point[] corner = new Point[4];
	        rr.points(corner);
	        Point[] middle = new Point[4];
	        for(int j = 0; j<4; j++) {
	        	middle[j] = new Point((corner[j % 4].x + corner[(j+1) % 4].x)/2,
	        							(corner[j % 4].y + corner[(j+1) % 4].y)/2);
	        }
	        od[0] = this.length2Points(middle[0], middle[2]);
	        od[1] = this.length2Points(middle[1], middle[3]);
	        id[0] = od[0];
	        id[1] = od[1];

	        for(int j = 0; j<2; j++) {
	        	Mat l_line = Mat.zeros(new Size(sampleImage.width(),sampleImage.height()),CvType.CV_8UC1);
		        Imgproc.line(l_line, middle[j], middle[j+2], new Scalar(255,255,255), (int)(rr.size.width / 20));
		        Core.bitwise_and(adjusted, l_line, l_line);

		        List<MatOfPoint> l_cnt = new ArrayList<MatOfPoint>();
		        Mat hie = new Mat();
		        Imgproc.findContours(l_line, l_cnt, hie, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

		        if(l_cnt.size() != 2) {
		        	od[0] = -1.0;
		        	break;
		        }

		        for(int k=0; k<2; k++) {
		        	MatOfPoint2f mop22 = new MatOfPoint2f(l_cnt.get(k).toArray());
		        	RotatedRect l_rr = Imgproc.minAreaRect(mop22);
		        	Point[] l_corner = new Point[4];
		        	l_rr.points(l_corner);

		        	double len_side1 = this.length2Points(l_corner[0], l_corner[1]);
		        	double len_side2 = this.length2Points(l_corner[1], l_corner[2]);
		        	if(len_side1 <= len_side2){
		        		Imgproc.line(masureImage, l_corner[0], l_corner[1], new Scalar(0,0,255), 2);
		        		Imgproc.line(masureImage, l_corner[2], l_corner[3], new Scalar(0,0,255), 2);
		        		id[j] = id[j] - len_side2;
		        	}else {
		        		Imgproc.line(masureImage, l_corner[1], l_corner[2], new Scalar(0,0,255), 2);
		        		Imgproc.line(masureImage, l_corner[3], l_corner[0], new Scalar(0,0,255), 2);
		        		id[j] = id[j] - len_side1;
		        	}
		        }
	        }

	        if(od[0] != -1.0) {
	        	Rect r = rr.boundingRect();
	        	values.add(new Values(od[0], id[0], od[1], id[1], r));
	        	Imgproc.putText(masureImage, String.valueOf(values.size() - 1), corner[0], Core.FONT_HERSHEY_SIMPLEX, 2.0, new Scalar(150, 150, 255), 2);
	        }


        }

        this.underLabel.setText(this.values.size() + "個の領域");

        this.processedImage = masureImage.clone();
        this.blackImage = adjusted.clone();
        this.showImage();

	}

	private void showImage() {
		Mat img;
		if(binary.isSelected()) {
			img = this.blackImage.clone();
		}else {
			img = this.processedImage.clone();
		}
		if(this.currentSelectedValues != null) {
			Imgproc.rectangle(img, this.currentSelectedValues.getRectangle().br(), this.currentSelectedValues.getRectangle().tl(), new Scalar(255, 0, 255), 2);
		}
		Imgproc.resize(img, this.resizedImage, new Size(0,0), this.zoom, this.zoom, Imgproc.INTER_AREA);

		if(resizedImage.width() < imageview.getFitWidth()) {
			Mat m = Mat.zeros(resizedImage.height(), (int)imageview.getFitWidth(), CvType.CV_8UC3);
			Core.copyMakeBorder(resizedImage, m, 0, 0, 0, (int)(imageview.getFitWidth()-resizedImage.width()), 0);
			resizedImage = m.clone();
		}
		if(resizedImage.height() < imageview.getFitHeight()) {
			Mat m = Mat.zeros((int)imageview.getFitHeight(),resizedImage.width() , CvType.CV_8UC3);
			Core.copyMakeBorder(resizedImage, m, 0, (int)(imageview.getFitHeight()-resizedImage.height()), 0, 0, 0);
			resizedImage = m.clone();
		}
		submat = this.resizedImage.submat((int)this.location.y, (int)(this.location.y + imageview.getFitHeight()),
        		(int)this.location.x, (int)(this.location.x + imageview.getFitWidth()));
        Utils.onFXThread(imageview.imageProperty(), Utils.mat2Image(submat));
	}

	@FXML
	public void browseClicked() {
		this.savePictureWithMarker();

	    FileChooser fc = new FileChooser();
	    fc.setTitle("Choose File");
	    fc.getExtensionFilters().addAll(
	    	new FileChooser.ExtensionFilter("TIFF file", "*.TIF", "*.tif"),
	    	new FileChooser.ExtensionFilter("JPEG file", "*.JPG", "*.jpg", "*.jpeg")
	    );
	    //fc.setInitialDirectory(new File(System.getProperty("user.home")));

	    File temp = null;
	    if(nowfile != null) {
	    	 temp = nowfile;
	    }
	    nowfile = fc.showOpenDialog(null);

	    if(nowfile != null) {
	    	nowdirectory = nowfile.getParentFile();
	    	filelist = Arrays.asList(nowdirectory.listFiles(new FilenameFilter() {
	    		 public boolean accept(File file, String str){
	    			 if (str.endsWith("tif") || str.endsWith("TIF") || str.endsWith("jpg") || str.endsWith("JPG") || str.endsWith("jpeg") ){
	    				 return true;
	    			 }else{
	    				 return false;
	    			 }
	    		 }
	    	}));
	    	for(int i=0; i<filelist.size(); i++) {
	    		System.out.println(filelist.get(i).getAbsolutePath());
	    	}
	    	filelist.sort(new FileSort());
	    	for(int i=0; i<filelist.size(); i++) {
	    		System.out.println(filelist.get(i).getAbsolutePath());
	    	}

	    	for(int i=0; i<filelist.size(); i++) {
	    		if(filelist.get(i).getAbsolutePath().equals(nowfile.getAbsolutePath())) {
	    			this.nowfileindex = i;
	    			break;
	    		}
	    	}
	    	System.out.println(nowfileindex);

	    	this.renewButton.setDisable(false);
			this.underLabel.setText("計算中…");
	    	this.processImage();

	    	this.filename.setText(nowfile.getAbsolutePath());
	    }else {
	    	nowfile = temp;
	    }

	}

	@FXML
	public void backClicked() {
		if(this.nowfileindex != 0 && this.filelist.size() != 0) {
			this.saveValue();
			this.savePictureWithMarker();
			this.nowfileindex--;
			this.nowfile = this.filelist.get(nowfileindex);
			this.processImage();
			this.filename.setText(nowfile.getAbsolutePath());
		}
	}

	@FXML
	public void nextClicked() {
		if(this.nowfileindex < (this.filelist.size() - 1)) {
			this.saveValue();
			this.savePictureWithMarker();
			this.nowfileindex++;
			this.nowfile = this.filelist.get(nowfileindex);
			this.processImage();
			this.filename.setText(nowfile.getAbsolutePath());
		}
	}

	@FXML
	public void binaryChecked() {
		this.showImage();
	}

	@FXML
	public void adjustChecked() {
		this.showImage();
	}

	@FXML
	public void click(MouseEvent me) {
		Point p = new Point(me.getX(), me.getY());
		if(adjust.isSelected()) {

		}else {
			Point ap = this.getAbsolutePoint(p);
			for(int i=0; i<this.values.size(); i++) {
				if(this.values.get(i).getRectangle().contains(ap)) {
					this.currentSelectedValues = values.get(i);
					this.setValue(this.currentSelectedValues);
					this.showImage();
					this.underLabel.setText(i + "番の結果");
					break;
				}
			}
		}
	}

	@FXML
	public void drag(MouseEvent me) {
		Point p = new Point(me.getX(), me.getY());

		if(adjust.isSelected()) {
			if(!dragging) {
				initPoint = p;
				dragging = true;
			}
			Mat m = submat.clone();
			Imgproc.line(m, initPoint, p, new Scalar(255,0,0), 4);
			Utils.onFXThread(imageview.imageProperty(), Utils.mat2Image(m));
		}else {

		}
	}

	@FXML
	public void release(MouseEvent me) {
		dragging = false;
		Point p = new Point(me.getX(), me.getY());
		if(adjust.isSelected()) {
			Mat m = submat;
			Imgproc.line(m, initPoint, p, new Scalar(255,0,0), 4);
			Utils.onFXThread(imageview.imageProperty(), Utils.mat2Image(m));

			double l = this.length2Points(initPoint, p);
			this.mmperpx = 10 / (l / this.zoom);
			unit.setText(String.format("10mm = %1$.3fpx", l));
		}else {

		}
	}

	@FXML
	public void keyPress(KeyEvent ke) {
		if(this.textfield1.isFocused()) return;

		switch(ke.getCode()) {
		case A:
			this.location.x = this.location.x - block;
			if(this.location.x < 0) {
				this.location.x = 0;
			}
			this.showImage();
			break;
		case S:
			this.location.x = this.location.x + block;
			if(this.location.x + this.imageview.getFitWidth() > this.resizedImage.width()) {
				this.location.x = this.resizedImage.width() - this.imageview.getFitWidth();
			}
			this.showImage();
			break;
		case W:
			this.location.y = this.location.y - block;
			if(this.location.y < 0) {
				this.location.y = 0;
			}
			this.showImage();
			break;
		case Z:
			this.location.y = this.location.y + block;
			if(this.location.y + this.imageview.getFitHeight() > this.resizedImage.height()) {
				this.location.y = this.resizedImage.height() - this.imageview.getFitHeight();
			}
			this.showImage();
			break;
		case T:
			this.location.x = this.resizedImage.width() - this.imageview.getFitWidth();
			this.location.y = this.resizedImage.height() - this.imageview.getFitHeight();
			this.showImage();
			break;
		case X:
			this.writeValue();
			break;
		case Q:
			this.eraseValue();
			break;
		case DIGIT3:
			if(adjust.isSelected()) {
				this.mmperpx = 25.4 / 300;
				unit.setText(String.format("10mm = %1$.3fpx", 10 / mmperpx));
			}
			break;
		case DIGIT6:
			if(adjust.isSelected()) {
				this.mmperpx = 25.4 / 600;
				unit.setText(String.format("10mm = %1$.3fpx", 10 / mmperpx));
			}
			break;

		case DIGIT0:
			table.getItems().add(new TableValue(this.textfield1.getText(), this.currentSelectedRow, new Values(0,0,0,0,new Rect()), mmperpx));
			this.currentSelectedRow++;
			this.underLabel.setText("空値を記録しました");
			break;
		default:
			break;

		}
	}

	@FXML
	public void renew() {
		this.blursize = (int)(this.blurSlider.getValue()*2);
		thresh = (11.0 - sensitivitySlider.getValue()) * 2;
		this.processImage();
	}

	@FXML
	public void writeValue() {
		if(this.currentSelectedValues == null) return;
		table.getItems().add(new TableValue(this.textfield1.getText(), this.currentSelectedRow, this.currentSelectedValues, mmperpx));
		this.recordResions.add(this.currentSelectedValues.getRectangle());
		this.currentSelectedRow++;
		this.underLabel.setText("記録しました");
	}
	@FXML
	public void eraseValue() {
		if(this.currentSelectedRow<2) return;
		table.getItems().remove(this.currentSelectedRow - 2);
		this.recordResions.remove(this.recordResions.size() - 1);
		this.currentSelectedRow--;
		this.underLabel.setText("一つ消去しました");
	}
	@FXML
	public void saveValue() {
		if(savefile == null) {
			FileChooser fc = new FileChooser();
			fc.setTitle("名前を付けて保存");
			fc.getExtensionFilters().addAll(
					new FileChooser.ExtensionFilter("CSV file", "*.csv", "*.CSV")
			);
			//fc.setInitialDirectory(new File(System.getProperty("user.home")));
			this.savefile = fc.showSaveDialog(null);

			if(savefile == null) {
				return;
			}
		}


	    try {
	    	PrintWriter pw = new PrintWriter(this.savefile);
	    	pw.println("name,ID,outer_short,outer_long,inner_short,inner_long");
	        for(int i=0; i<(currentSelectedRow - 1); i++){
	        	String r = String.join(",", String.valueOf(colname.getCellData(i)), String.valueOf(col0.getCellData(i)),String.valueOf(col1.getCellData(i)),
	        				String.valueOf(col2.getCellData(i)), String.valueOf(col3.getCellData(i)), String.valueOf(col4.getCellData(i)));
	        	pw.println(r);
	        }
	        pw.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

	    this.underLabel.setText("データを保存しました");
	}

	@FXML
	public void savePicture() {
		FileChooser fc = new FileChooser();
	    fc.setTitle("名前を付けて保存");
	    fc.getExtensionFilters().addAll(
	    	new FileChooser.ExtensionFilter("JPEG file", "*.jpg", "*.JPG")
	    );
	    //fc.setInitialDirectory(new File(System.getProperty("user.home")));
	    File f = fc.showSaveDialog(null);

	    if(f == null) {
	    	return;
	    }

	    if(Imgcodecs.imwrite(f.getAbsolutePath(), this.processedImage)) {
	    	this.underLabel.setText("画像を保存しました");
	    }else {
	    	this.underLabel.setText("画像の保存に失敗しました");
	    }
	}

	public void savePictureWithMarker() {
		if(this.recordResions.size() == 0) return;

		while(this.picturefile == null) {
			DirectoryChooser dc = new DirectoryChooser();
			dc.setTitle("画像の保存場所を選択");
			dc.setInitialDirectory(new File(System.getProperty("user.home")));
			this.picturefile = dc.showDialog(null);
		}

		Mat m = this.processedImage.clone();
		for(int i = 0; i<this.recordResions.size(); i++) {
			Imgproc.rectangle(m, this.recordResions.get(i).br(), this.recordResions.get(i).tl(), new Scalar(255, 0, 255), 2);
		}
		this.recordResions.clear();

		String url = this.picturefile.getAbsolutePath() + File.separator + this.nowfile.getName() + ".jpg";
	    if(Imgcodecs.imwrite(url, m)) {
	    	this.underLabel.setText("画像を保存しました");
	    }else {
	    	this.underLabel.setText("画像の保存に失敗しました");
	    }
	}

	private double length2Points(Point a, Point b) {
		double ans;
		ans = Math.sqrt(Math.pow((a.x - b.x), 2) + Math.pow((a.y - b.y), 2));
		return ans;

	}

	private Point getAbsolutePoint(Point p) {
		Point ap = new Point((p.x + this.location.x) * this.zoom, (p.y + this.location.y) * this.zoom);
		return ap;
	}

	private void setValue(Values val) {
		this.os.setText(String.format("外短径：%1$.2fmm", val.getShorterOuterDiameter() * mmperpx));
		this.ol.setText(String.format("外長径：%1$.2fmm", val.getLongerOuterDiameter() * mmperpx));
		this.is.setText(String.format("内短径：%1$.2fmm", val.getShorterinnerDiameter() * mmperpx));
		this.il.setText(String.format("内長径：%1$.2fmm", val.getLongerInnerDiameter() * mmperpx));
		this.sm.setText(String.format("断面係数：%1$.2fmm3", val.getSectionModulus() * Math.pow(mmperpx, 3)));
	}

}
