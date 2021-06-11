package application;

import java.io.File;
import java.util.Comparator;

public class FileSort implements Comparator<File> {

	@Override
	public int compare(File o1, File o2) {
		// TODO 自動生成されたメソッド・スタブ
		int diff = o1.getName().compareTo(o2.getName());
		return diff;

	}

}
