package squirrel.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.awt.Desktop;

public class UTIL_FileOperations {

	// copy a file on a destination dir on the disk
	public static void copyFile(File sourceFile, File destFile) throws IOException {
		if (!destFile.exists()) {
			destFile.createNewFile();
		}

		FileChannel source = null;
		FileChannel destination = null;

		try {
			source = new FileInputStream(sourceFile).getChannel();
			destination = new FileOutputStream(destFile).getChannel();
			destination.transferFrom(source, 0, source.size());
		} finally {
			if (source != null) {
				source.close();
			}
			if (destination != null) {
				destination.close();
			}
		}
	}

	// try to open a file in a system window
	public static void openFileWindow(String filename) {

		try {
			File file = new File(filename);
			if (file.exists()) {
				if (Desktop.isDesktopSupported()) {
					Desktop.getDesktop().open(file);
				}
			}
		} catch (Exception ex) {
			System.out.println(filename + "could not be opened.");
		}

	}
}
