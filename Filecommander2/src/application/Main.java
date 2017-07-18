package application;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.swing.Icon;
import javafx.beans.value.ChangeListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.filechooser.FileSystemView;

import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class Main extends Application {

	public static final int HGAP = 10;
	public static final int VGAP = 10;

	private String filename;

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	private TreeView<File> treeView = new TreeView<File>();

	public TreeView<File> getTreeView() {
		return treeView;
	}

	public void setTreeView(TreeView<File> treeView) {
		this.treeView = treeView;
	}

	@Override
	public void start(Stage primaryStage) {

		try {

			primaryStage.setTitle("Filecommander");
			primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/res/appIcon.png")));
			StackPane centralPane = new StackPane();
			BorderPane rootPane = new BorderPane();
			Scene scene = new Scene(rootPane, 700, 600);
			String css = this.getClass().getResource("/res/styles.css").toExternalForm();
			scene.getStylesheets().add(css);
			GridPane buttonsGgrid = new GridPane();
			buttonsGgrid.setHgap(HGAP);
			buttonsGgrid.setVgap(VGAP);
			VBox fileTreeVbox = new VBox();
			fileTreeVbox.prefWidthProperty().bind(primaryStage.widthProperty().multiply(0.80));
			Label statusBar = new Label();

			File[] paths = File.listRoots();
			setFilename(paths[0].toString());

			LazyTree lazyTree = new LazyTree();

			TreeItem<File> rootNode = lazyTree.createNode(new File(filename));

			refreshTreeVBox(rootNode, fileTreeVbox, statusBar);

			MenuBar menuBar = new MenuBar();

			Menu menuFile = new Menu("File");
			Menu menuView = new Menu("View");

			MenuItem showHiden = new CheckMenuItem("Show hiden files");
			showHiden.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {

					lazyTree.setHIDE_HIDDEN_FILES(!lazyTree.isHIDE_HIDDEN_FILES());
					TreeItem<File> rootNode = lazyTree.createNode(new File(getFilename()));
					refreshTreeVBox(rootNode, fileTreeVbox, statusBar);

					if (lazyTree.isHIDE_HIDDEN_FILES()) {
						statusBar.setText("Hidden files are hidden");
					} else {
						statusBar.setText("Hidden files are shown");
					}

				}
			});

			MenuItem menuExit = new CheckMenuItem("Close");
			menuExit.setOnAction(new EventHandler<ActionEvent>() {
				public void handle(ActionEvent event) {
					Platform.exit();
					System.exit(0);
				}
			});

			Button[] driveNameButton = new Button[paths.length];

			int i = 0;
			for (File path : paths) {
				driveNameButton[i] = new Button(path.toString());
				buttonsGgrid.add(driveNameButton[i], (i + (int) driveNameButton[i].getWidth()), 1);

				driveNameButton[i].setOnAction(new EventHandler<ActionEvent>() {
					@Override
					public void handle(ActionEvent event) {
						setFilename(path.toString());
						refreshTreeVBox(lazyTree.createNode(new File(getFilename())), fileTreeVbox, statusBar);
					}
				});

				i++;
			}

			Button createFolderButton = new Button("Create folder");
			createFolderButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					if (lazyTree.createFolder(treeView)) {
						statusBar.setText("Folder created");

					}
				};
			});

			// put all the elements

			rootPane.setTop(menuBar);
			rootPane.setCenter(centralPane);

			menuBar.getMenus().addAll(menuFile, menuView);
			menuFile.getItems().addAll(menuExit);
			menuView.getItems().addAll(showHiden);

			centralPane.getChildren().addAll(buttonsGgrid, fileTreeVbox, createFolderButton);
			centralPane.setAlignment(buttonsGgrid, Pos.TOP_LEFT);
			centralPane.setMargin(buttonsGgrid, new Insets(0.0, 0.0, 0.0, 5.0));

			centralPane.setMargin(fileTreeVbox, new Insets(40.0, 0.0, 0.0, 0.0));
			centralPane.setAlignment(fileTreeVbox, Pos.CENTER_LEFT);

			centralPane.setAlignment(createFolderButton, Pos.BOTTOM_LEFT);
			centralPane.setMargin(createFolderButton, new Insets(0.0, 0.0, 5.0, 5.0));

			rootPane.setBottom(statusBar);
			statusBar.setText("ready");
			primaryStage.setScene(scene);
			primaryStage.show();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private void refreshTreeVBox(TreeItem<File> rootNode, VBox fileTreeVbox, Label statusBar) {
		setTreeView(new TreeView<File>(rootNode));
		treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Object>() {

			@Override
			public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {

				TreeItem<File> selectedItem = (TreeItem<File>) newValue;
				if (selectedItem.isLeaf()) {
					statusBar.setText("Selected folder : " + selectedItem.getParent().getValue());
				} else {
					statusBar.setText("Selected folder : " + selectedItem.getValue());
				}

			}

		});

		treeView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
		rootNode.setExpanded(true);
		fileTreeVbox.getChildren().clear();
		fileTreeVbox.getChildren().add(treeView);
		fileTreeVbox.setVgrow(treeView, Priority.ALWAYS);
		fileTreeVbox.setMargin(treeView, new Insets(00.0, 00.0, 35.0, 0.0));
	}

	public static void main(String[] args) {
		launch(args);
	}

}
