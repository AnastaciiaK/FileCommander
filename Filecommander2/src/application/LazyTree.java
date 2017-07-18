package application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.swing.filechooser.FileSystemView;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.*;
import javafx.stage.Stage;

public class LazyTree extends TreeView<File> {
	private boolean HIDE_HIDDEN_FILES = true;

	public boolean isHIDE_HIDDEN_FILES() {
		return HIDE_HIDDEN_FILES;
	}

	public void setHIDE_HIDDEN_FILES(boolean hIDE_HIDDEN_FILES) {
		HIDE_HIDDEN_FILES = hIDE_HIDDEN_FILES;
	}

	public File getSelectedItem(TreeView<File> treeView) {
		TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
		return selectedItem == null ? null : selectedItem.getValue();
	}

	public boolean createFolder(TreeView<File> treeView) {
		TreeItem<File> selectedItem = treeView.getSelectionModel().getSelectedItem();
		File selectedFile = getSelectedItem(treeView);
		File newFolder;
		if (selectedFile != null) {

			TextInputDialog dialog = new TextInputDialog("New Folder");
			dialog.setTitle("Create folder");
			dialog.setHeaderText("Folder creation");
			dialog.setContentText("Please enter folder name:");
			Optional<String> result = dialog.showAndWait();
			if (result.isPresent()) {
				if (!selectedFile.isDirectory())
					selectedFile = selectedFile.getParentFile();
				newFolder = new File(selectedFile + "\\" + result.get());
				if (!newFolder.exists()) {
					if (newFolder.mkdir()) {
						TreeItem newFolderItem = new TreeItem(newFolder);
						TreeItem<File> addedItem = createNode(newFolder);
						addedItem.setExpanded(true);
						selectedItem.getChildren().add(addedItem);
						System.out.println("Directory is created!" + newFolder);
						return true;
					} else {
						System.out.println("Failed to create directory " + newFolder);
						return false;
					}
				}
			}

		} else {

			return false;
		}
		return false;
	};

	public TreeItem<File> createNode(final File f) {
		return new TreeItem<File>(f) {
			public boolean isLeaf;
			public boolean isFirstTimeChildren = true;
			public boolean isFirstTimeLeaf = true;
			private ProgressIndicator pinMain;

			@Override
			public ObservableList<TreeItem<File>> getChildren() {
				if (isFirstTimeChildren) {

					isFirstTimeChildren = false;
					
					pinMain = new ProgressIndicator();
					pinMain.setPrefSize(20, 20);

					new Thread(() -> {
						try {

							Platform.runLater(() -> {
								setGraphic(pinMain);
							});
							Thread.sleep(1500);
							loadChildren();
							Platform.runLater(() -> {
								setGraphic(null);
							});

						} catch (Exception e1) {
						}
					}).start();
				}
				return super.getChildren();
			}

			private void loadChildren() {
				super.getChildren().setAll(buildChildren(this));
			}

			@Override
			public boolean isLeaf() {
				if (isFirstTimeLeaf) {
					isFirstTimeLeaf = false;
					File f = (File) getValue();
					isLeaf = f.isFile();
				}
				return isLeaf;
			}

			public ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
				File f = TreeItem.getValue();
				if (f == null) {
					return FXCollections.emptyObservableList();
				}
				if (f.isFile()) {

					return FXCollections.emptyObservableList();
				}
				TreeItem childFiles = null;
				File[] files = f.listFiles();
				Image fxImage = null;
				if (files != null) {

					ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

					for (File childFile : files) {

						if ((HIDE_HIDDEN_FILES && !childFile.isHidden()) || (!childFile.isHidden())
								|| (!HIDE_HIDDEN_FILES && childFile.isHidden())) {
							childFiles = createNode(childFile);
							if (!childFile.isDirectory()) {
								fxImage = getFileIcon(childFile.getName());
							}
							ImageView imageView = new ImageView(fxImage);
							childFiles = createNode(childFile);
							childFiles.setGraphic(imageView);
							children.add(childFiles);
						}
					}
					return children;
				}
				return FXCollections.emptyObservableList();
			}
		};
	}

	static HashMap<String, Image> mapOfFileExtToSmallIcon = new HashMap<String, Image>();

	public static String getFileExt(String fname) {
		String ext = ".";
		int p = fname.lastIndexOf('.');
		if (p >= 0) {
			ext = fname.substring(p);
		}
		return ext.toLowerCase();
	}

	public static javax.swing.Icon getJSwingIconFromFileSystem(File file) {

		// Windows {
		FileSystemView view = FileSystemView.getFileSystemView();
		javax.swing.Icon icon = view.getSystemIcon(file);
		// }

		// OS X {
		// final javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
		// javax.swing.Icon icon = fc.getUI().getFileView(fc).getIcon(file);
		// }

		return icon;
	}

	public static Image getFileIcon(String fname) {
		final String ext = getFileExt(fname);
		Image fileIcon = mapOfFileExtToSmallIcon.get(ext);
		if (fileIcon == null) {
			javax.swing.Icon jswingIcon = null;
			File file = new File(fname);
			if (file.exists()) {
				jswingIcon = getJSwingIconFromFileSystem(file);
			} else {
				File tempFile = null;
				try {
					tempFile = File.createTempFile("icon", ext);
					jswingIcon = getJSwingIconFromFileSystem(tempFile);
				} catch (IOException ignored) {
				} finally {
					if (tempFile != null)
						tempFile.delete();
				}
			}
			if (jswingIcon != null) {
				fileIcon = jswingIconToImage(jswingIcon);
				mapOfFileExtToSmallIcon.put(ext, fileIcon);
			}
		}
		return fileIcon;
	}

	public static Image jswingIconToImage(javax.swing.Icon jswingIcon) {
		BufferedImage bufferedImage = new BufferedImage(jswingIcon.getIconWidth(), jswingIcon.getIconHeight(),
				BufferedImage.TYPE_INT_ARGB);
		jswingIcon.paintIcon(null, bufferedImage.getGraphics(), 0, 0);
		return SwingFXUtils.toFXImage(bufferedImage, null);
	}

}
