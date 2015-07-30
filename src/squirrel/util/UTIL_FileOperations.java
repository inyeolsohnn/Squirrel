package squirrel.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.awt.Desktop;

import squirrel.smt.aligner.IBM1.Bitext;
import squirrel.smt.aligner.IBM1.WordPair;

public class UTIL_FileOperations {

	// copy a file on a destination dir on the disk
	public static void copyFile(File sourceFile, File destFile)
			throws IOException {
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

	public static boolean store(Object obj, String dn) {
		// TODO Auto-generated method stub
		if (obj == null)
			return false;
		else {
			try {
				FileOutputStream fileOut = new FileOutputStream(dn);
				ObjectOutputStream out = new ObjectOutputStream(fileOut);
				out.writeObject(obj);
				out.close();
				fileOut.close();
				System.out.printf("object saved as " + dn);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				return false;
			}
			return true;
		}
	}

	public static Object openObject(String dn) {
		Object result;
		try {
			FileInputStream fis = new FileInputStream(new File(dn));

			ObjectInputStream ois = new ObjectInputStream(fis);

			result = ois.readObject();

		} catch (IOException | ClassNotFoundException ioe) {
			ioe.printStackTrace();
			return null;
		}
		return result;
	}

}
