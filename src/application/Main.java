package application;

import org.opencv.core.Core;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class Main extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		// TODO 自動生成されたメソッド・スタブ
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("SMeasure.fxml"));
			BorderPane root = (BorderPane)loader.load();
			Scene scene = new Scene(root,800,600);
			scene.getStylesheets().add(getClass().getResource("application.css").toExternalForm());
			primaryStage.setTitle("SMeasure v2.00");
			primaryStage.setScene(scene);
			primaryStage.setMaximized(true);
			primaryStage.show();

			SMeasureController controller = loader.getController();
			controller.init();
		} catch(Exception e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		//System.out.println(System.getProperty("user.dir"));
		System.load(System.getProperty("user.dir") + "\\opencv_java500.dll"); //配布版で有効にする
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		launch(args);

	}

}
