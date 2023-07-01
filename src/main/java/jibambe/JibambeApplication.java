/*
 * Copyright 2021 patrick.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jibambe;

import java.io.File;
import java.net.MalformedURLException;
import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import static javafx.scene.media.MediaPlayer.Status.PLAYING;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 *
 * @author patrick
 */
public class JibambeApplication extends Application{
    
    MenuBar menuBar;
    Menu mediaFile, metadata, help, tools;
    BorderPane borderPane;
    Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
    Media media;
    MediaPlayer player;
    MediaView view = new MediaView();
    Stage mainStage;
    MenuItem openFile;
    Button play, forward, backward, stop, pause;
    boolean isPlaying = false;
    Slider playProgress, volumeAdjust;
    Label imageLabel;
    ImageView songImage;
    ObservableMap<String, Object> songMetadata;
    private Duration mediaDuration;
    
    public JibambeApplication() {
        volumeAdjust = new Slider(0.0, 1.0, 0.5);
        playProgress = new Slider();
    }
    
    public void playMusic(String mediaResource) throws MalformedURLException{
        if(mediaResource == null){
            locateMediaFile();
        }else{
            try{
                if(isPlaying){
                    player.stop();
                    player.dispose();
                }
                System.out.println(mediaResource);
                media = new Media(mediaResource);
                media.getMetadata().addListener(new MapChangeListener<String, Object>(){
                	@Override
                	public void onChanged(MapChangeListener.Change<? extends String,? extends Object>ch) {
                		//System.out.println("On change");
                		if(ch.wasAdded()) {
                                    String key = ch.getKey();
                                    Object value = ch.getValueAdded();
                                    //System.out.println("In listener change state");
                                    switch (key) {
					case "image":
                                        songImage = new ImageView((Image) value);
                        		borderPane.setCenter(imageLabel);
                        		songImage.fitHeightProperty().bind(mainStage.heightProperty().divide(1.3));
                        		songImage.setPreserveRatio(true);
                        		imageLabel.setGraphic(songImage);
                        		imageLabel.setText(null);
								break;
							case "title":
								mainStage.setTitle("Jibambe Player" +
										"--" + value.toString());
							default:
								break;
							}
                		}else {
                			System.out.println("Sth was removed from metadata ");
                		}
                                if(ch.wasRemoved()){
                                    System.out.println("ch is empty");
                                }
                	}
                });
                player = new MediaPlayer(media);
                player.setOnReady(()->{
                	mediaDuration = player.getMedia().getDuration();
                });
                
                player.statusProperty().addListener((prop, oldStatus, newStatus) -> {
                    System.out.println("Status changed from " + oldStatus + " to " + newStatus);
                    if(player.getStatus()==PLAYING){
                        player.setAutoPlay(false);
                        play.setText("||");
                        isPlaying = true;
                    }else {
                        play.setText(">");
                        isPlaying = false;
                    }
                });
                player.currentTimeProperty().addListener((prop,oldStatus,newStatus)->{
                    Double currentTm = (newStatus.toSeconds()/mediaDuration.toSeconds())*100;
                    //System.out.println("totalDuration: " + currentTm.toString());
                    playProgress.setValue(currentTm);
                    //System.out.println("Current time : "+newStatus.toMinutes());
                });
                
                player.volumeProperty().bind(volumeAdjust.valueProperty());
                playProgress.valueProperty().addListener(new InvalidationListener() {
					
                    @Override
                    public void invalidated(Observable arg0) {
                            if(playProgress.isValueChanging()) {
                            player.seek(mediaDuration.multiply(playProgress.getValue()/100.0));
                            //System.out.println(playProgress.getValue());
                        }
                    }
                });
                player.setOnEndOfMedia(()->{
                	play.setText(">");
                	player.stop();
                });
                player.setOnPlaying(()->{
                	System.out.print(player.getStatus().toString());
                	play.setText("||");
                });
                view.prefWidth(Double.MAX_VALUE);
                view.prefHeight(Region.USE_COMPUTED_SIZE);
                view.setMediaPlayer(player);
                System.out.println(view.getMediaPlayer().toString());
                if(view != null) {
                	borderPane.setCenter(view);
                        mainStage.setTitle("Jibambe Playe -- " + player.getMedia().getSource());
                }
                DoubleProperty mvw = view.fitWidthProperty();
                DoubleProperty mvh = view.fitHeightProperty();
                mvw.bind(mainStage.widthProperty().divide(1.1));
                mvh.bind(mainStage.heightProperty().divide(1.2));
                //view.setFitHeight(bounds.getHeight()-70);
                view.setSmooth(true);
                view.setPreserveRatio(true);
                System.out.println("Player is in :" + player.getStatus());
                player.setAutoPlay(true);
            }catch(Exception e){
                //System.out.println("Not able due to " + e);
                //e.printStackTrace();
                Alert error = new Alert(Alert.AlertType.ERROR);
                error.setTitle("Error!");
                error.setHeaderText("Media Error !");
                error.setContentText(e.getMessage());
                error.initOwner(mainStage);
                error.initModality(Modality.APPLICATION_MODAL);
                error.initStyle(StageStyle.DECORATED);
                error.showAndWait();
            }
        }
    }
    
    public void locateMediaFile() throws MalformedURLException{
        FileChooser fileDialog = new FileChooser();
        fileDialog.setTitle("Please choose a media file!");
        fileDialog.setInitialDirectory(new File(System.getProperty("user.home")));
        fileDialog.getExtensionFilters().addAll(
        new FileChooser.ExtensionFilter("Media Files",
                "*.mkv", "*.VOB", "*.mp4", "*.webm", "*.mp3", "*.ogg", "*.aac"));
        File file = fileDialog.showOpenDialog(mainStage);
        if(file == null) return;
        String fileFound = file.toURI().toURL().toString();
        System.out.println(fileFound);
        //if(fileFound == null) return;
        playMusic(fileFound);
    }
    
    @Override
    public void start(Stage primaryStage) {
        mainStage = primaryStage;
        menuBar = new MenuBar();
        menuBar.setUseSystemMenuBar(true);
        
        mediaFile = new Menu("Media");
        metadata = new Menu("Metadata");
        tools = new Menu("Tools");
        help = new Menu("Help");
        openFile = new MenuItem("Open media ...");
        openFile.setOnAction((ActionEvent evt) -> {
            try {
                locateMediaFile();
            } catch (MalformedURLException ex) {
                System.err.println(ex);
            }
        });
        menuBar.getMenus().addAll(mediaFile, metadata, tools, help);
        mediaFile.getItems().addAll(openFile);
        
        //control buttons
        play = new Button(">");
        play.setOnAction((ActionEvent evt) -> {
            if(play.getText().equals(">")){
                player.play();
                play.setText("||");
            }else {
                player.pause();
                play.setText(">");
            }
            
        });
        
        backward = new Button("<<");
        backward.setOnAction((evt)->{
            player.seek(player.getCurrentTime().divide(1.1));
        });
        
        forward = new Button(">>");
        forward.setOnAction((evt)->{
            player.seek(player.getCurrentTime().multiply(1.1));
        });
        
        HBox controller = new HBox(10);
        VBox bottomContainer = new VBox();
        bottomContainer.getChildren().addAll(playProgress, controller);
        controller.setAlignment(Pos.TOP_CENTER);
        controller.getChildren().addAll(play, backward, forward,volumeAdjust);
        borderPane = new BorderPane();
        borderPane.setPrefSize(500, 400);
        BorderPane.setAlignment(view, Pos.CENTER);
        borderPane.setPadding(new Insets(5, 10, 5, 10));
        borderPane.setTop(menuBar);
        borderPane.setCenter(view);
        imageLabel = new Label("here is the image");
        //borderPane.setLeft(imageLabel);
        borderPane.setStyle("-fx-background-color:black");
        //borderPane.set
        borderPane.setBottom(bottomContainer);
        
        //the icon for stage/the Jibambe App
        Image icon = new Image(getClass().getResourceAsStream("/icons/desktop-icon.png"));
        mainStage.getIcons().add(icon);
        
        double x = bounds.getMinX() + (bounds.getWidth() - mainStage.getWidth())/2.0;
        double y = bounds.getMinY() + (bounds.getHeight() - mainStage.getHeight())/2.0;
        mainStage.setX(x);
        mainStage.setY(y);
        Scene scene = new Scene(borderPane, bounds.getMaxX(), bounds.getMaxY()-bounds.getMinY());
        mainStage.setMinWidth(620);
        mainStage.setMinHeight(400);
        mainStage.setTitle("Jibambe Player");
        scene.setFill(Color.BLACK);
        mainStage.setScene(scene);
        mainStage.show();
    }
    
}
