package com.musicplayer.ui.controllers;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import com.musicplayer.data.models.Album;
import com.musicplayer.data.models.Playlist;
import com.musicplayer.data.models.Song;
import com.musicplayer.data.repositories.AlbumRepository;
import com.musicplayer.data.repositories.PersistentAlbumRepository;
import com.musicplayer.data.repositories.PersistentPlaylistRepository;
import com.musicplayer.data.repositories.PersistentSongRepository;
import com.musicplayer.data.repositories.PlaylistRepository;
import com.musicplayer.data.repositories.SongRepository;
import com.musicplayer.data.storage.JsonLibraryStorage;
import com.musicplayer.data.storage.LibraryStorage;
import com.musicplayer.services.AudioPlayerService;
import com.musicplayer.services.FavoritesService;
import com.musicplayer.services.LibraryService;
import com.musicplayer.services.ListeningStatsService;
import com.musicplayer.services.MusicLibraryManager;
import com.musicplayer.services.PlaylistManager;
import com.musicplayer.services.PlaylistService;
import com.musicplayer.services.SettingsService;
import com.musicplayer.services.UpdateService;
import com.musicplayer.ui.components.ActivityFeedItem;
import com.musicplayer.ui.components.AlbumGridView;
import com.musicplayer.ui.components.AudioVisualizer;
import com.musicplayer.ui.components.NowPlayingBar;
import com.musicplayer.ui.components.PinboardItem;
import com.musicplayer.ui.components.PinboardPanel;
import com.musicplayer.ui.components.PlaylistCell;
import com.musicplayer.ui.components.RescanButtonFactory;
import com.musicplayer.ui.controllers.AudioConversionController;
import com.musicplayer.ui.dialogs.FirstRunWizard;
import com.musicplayer.ui.dialogs.MissingFilesDialog;
import com.musicplayer.ui.dialogs.PlaylistSelectionPopup;
import com.musicplayer.ui.dialogs.UpdateDialog;
import com.musicplayer.ui.handlers.PlaylistActionHandler;
import com.musicplayer.ui.util.AlbumArtLoader;
import com.musicplayer.ui.util.SearchManager;
import com.musicplayer.ui.util.SongContextMenuProvider;
import com.musicplayer.ui.windows.MiniPlayerWindow;
import com.musicplayer.ui.windows.MiniPlayerWindow.ShowSongInLibraryEvent;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

public class MainController implements Initializable, IControllerCommunication {
    
    @FXML private ListView<String> libraryListView;
    @FXML private ListView<Playlist> playlistsListView;
    @FXML private TableView<Song> songsTableView;
    @FXML private TableColumn<Song, Boolean> favoriteColumn;
    @FXML private TableColumn<Song, String> titleColumn;
    @FXML private TableColumn<Song, String> artistColumn;
    @FXML private TableColumn<Song, String> albumColumn;
    @FXML private TableColumn<Song, String> durationColumn;
    @FXML private Button previousButton;
    @FXML private Button playPauseButton;
    @FXML private Button nextButton;
    @FXML private Button selectMusicFolderButton;
    @FXML private Button addPlaylistButton;
    @FXML private Button addToPlaylistButton;
    @FXML private Button settingsButton;
    @FXML private Label currentTimeLabel;
    @FXML private Label totalTimeLabel;
    @FXML private Slider timeSlider;
    @FXML private Slider volumeSlider;
    @FXML private ImageView volumeIcon;
    @FXML private Label volumePercentageLabel;
    
    // Album art display elements
    @FXML private StackPane albumArtContainer;
    @FXML private ImageView albumArtImageView;
    @FXML private ImageView albumArtImageView2;
    @FXML private Label songTitleLabel;
    @FXML private Label songArtistLabel;
    
    // Icons for play/pause button
    private Image playIcon;
    private Image pauseIcon;
    private ImageView playPauseImageView;
    
    private LibraryService libraryService;
    private MusicLibraryManager musicLibraryManager;
    private PlaylistService playlistService;
    private PlaylistManager playlistManager;
    private AudioPlayerService audioPlayerService;
    private ListeningStatsService listeningStatsService;
    private FavoritesService favoritesService;
    private SettingsService settingsService;
    private UpdateService updateService;
    private ObservableList<Song> songs;
    private ObservableList<Playlist> playlists;

    private FilteredList<Song> filteredSongs;
    private FilteredList<Playlist> filteredPlaylists;

    private Playlist playingPlaylist;
    
    // Extracted playback controller
    private PlaybackController playbackController;
    
    // Audio conversion controller
    private AudioConversionController audioConversionController;
    
    // Repositories
    private AlbumRepository albumRepository;
    private SongRepository songRepository;

    @FXML private Button playlistSearchButton;
    @FXML private TextField playlistSearchField;
    @FXML private Button songSearchButton;
    @FXML private TextField songSearchField;

    private VisualizerController visualizerController;
    private AlbumGridView albumGridView;
    @FXML private VBox libraryContainer; // parent VBox containing library section
    private PinboardPanel pinboardPanel;
    private NowPlayingBar nowPlayingBar;
    @FXML private HBox controlBar;

    private MiniPlayerWindow miniPlayerWindow;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Initialize storage and repositories
        LibraryStorage storage = new JsonLibraryStorage();
        songRepository = new PersistentSongRepository(storage);
        PlaylistRepository playlistRepository = new PersistentPlaylistRepository(storage);
        albumRepository = new PersistentAlbumRepository(storage);
        libraryService = new LibraryService(songRepository);
        playlistService = new PlaylistService(playlistRepository);
        
        // Initialize the music library manager
        musicLibraryManager = new MusicLibraryManager(songRepository);
        
        // Initialize the playlist manager
        playlistManager = new PlaylistManager(playlistRepository);
        
        // Initialize audio player service
        audioPlayerService = new AudioPlayerService();
        
        // Initialize listening stats service
        listeningStatsService = new ListeningStatsService(songRepository);
        
        // Initialize the songs and playlists lists BEFORE creating AudioController
        songs = FXCollections.observableArrayList();
        playlists = FXCollections.observableArrayList();
        
        // Create the audio controller with all required dependencies
        playbackController = new PlaybackController();
        playbackController.setUIComponents(
            playPauseButton,
            previousButton,
            nextButton,
            timeSlider,
            volumeSlider,
            volumeIcon,
            volumePercentageLabel,
            currentTimeLabel,
            totalTimeLabel,
            albumArtContainer,
            albumArtImageView,
            albumArtImageView2,
            songTitleLabel,
            songArtistLabel
        );
        playbackController.initialize(audioPlayerService, listeningStatsService, songs, songsTableView);
        
        // Set up communication interface
        playbackController.setCommunicationInterface(this);
        
        // Initialize favorites service
        favoritesService = new FavoritesService();
        
        // Initialize settings service
        settingsService = new SettingsService();
        
        // Initialize update service
        updateService = new UpdateService(settingsService);
        
        // Set up error handling for missing files
        audioPlayerService.setOnError(() -> handleMissingFiles());

        // Wrap with filtered lists for searching
        filteredSongs = new FilteredList<>(songs, s -> true);
        filteredPlaylists = new FilteredList<>(playlists, p -> true);
        
        // Set up callback to update UI when library changes
        musicLibraryManager.setLibraryUpdateCallback(updatedSongs -> {
            // Update favorite status for all songs
            favoritesService.updateFavoriteStatus(updatedSongs);
            
            songs.clear();
            songs.addAll(updatedSongs);
            // Update audio player playlist when library changes
            audioPlayerService.setPlaylist(songs);
            // Refresh library service to update albums
            libraryService.refreshLibrary();
            // Synchronize albums from library engine to the persistent repository so that
            // newly discovered albums are also persisted and available for editing next session.
            syncAlbumsWithRepository();
            // Refresh album view if it exists
            if (albumGridView != null) {
                javafx.application.Platform.runLater(() -> {
                    // Use album repository to retrieve albums so that persisted edits (name, cover art) are reflected
                    albumGridView.refresh(albumRepository.findAll());
                });
            }
            // Update library stats in pinboard panel
            if (pinboardPanel != null) {
                int totalAlbums = albumRepository.findAll().size();
                File musicFolderFile = musicLibraryManager.getCurrentMusicFolder();
                String musicFolder = musicFolderFile != null ? musicFolderFile.getAbsolutePath() : null;
                pinboardPanel.updateLibraryStats(updatedSongs.size(), totalAlbums, musicFolder);
                pinboardPanel.addActivity(ActivityFeedItem.ActivityType.SCAN_COMPLETE, 
                    "Library updated: " + updatedSongs.size() + " songs");
            }
            
            // Re-enable error dialogs after library scan completes
            audioPlayerService.setSuppressErrorDialogs(false);
        });
        
        // Set up callback to update UI when playlists change
        playlistManager.setPlaylistUpdateCallback(updatedPlaylists -> {
            playlists.clear();
            playlists.addAll(updatedPlaylists);
            if (pinboardPanel != null) {
                pinboardPanel.addActivity(ActivityFeedItem.ActivityType.PLAYLIST_SAVED, 
                    "Playlist updated");
            }
        });
        
        // Load existing library data if available
        musicLibraryManager.initializeLibrary();
        // Ensure albums detected from the initial library scan are persisted
        syncAlbumsWithRepository();
        
        // Load existing playlists if available
        playlistManager.initializePlaylists();
        
        // Set up playlist controls
        setupPlaylistControls();
        
        // Now playing bar under playlists
        setupNowPlayingBar();
        
        // Set up audio controller callbacks
        playbackController.setOnStatsUpdate(() -> updateListeningStats());
        playbackController.setOnPlaybackStateChange(() -> {
            songsTableView.refresh();
            refreshPlaylistCells();
            if (nowPlayingBar != null) {
                nowPlayingBar.update(audioPlayerService.getCurrentSong(), audioPlayerService.isPlaying());
            }
            // Update visualizer state when playback state changes
            if (visualizerController != null) {
                visualizerController.updateMainVisualizerState();
            }
        });
        
        // Set up table columns
        setupFavoriteColumn();
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        artistColumn.setCellValueFactory(new PropertyValueFactory<>("artist"));
        albumColumn.setCellValueFactory(new PropertyValueFactory<>("album"));
        durationColumn.setCellValueFactory(cellData -> {
            long durationInSeconds = cellData.getValue().getDuration();
            return new javafx.beans.property.SimpleStringProperty(formatDuration(durationInSeconds));
        });
        
        // Bind the table to the songs list
        songsTableView.setItems(filteredSongs);

        // Enable multi-selection
        songsTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Set up table row factory for double-click to play and drag-and-drop
        setupTableRowFactory();
        
        // Configure Add-to-Playlist button
        addToPlaylistButton.setVisible(false);
        addToPlaylistButton.setDisable(true);

        // Show button when a song is selected
        songsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            boolean hasSelection = newSel != null;
            addToPlaylistButton.setVisible(hasSelection);
            addToPlaylistButton.setDisable(!hasSelection);
        });

        // Initialize library list
        setupPinboardPanel();
        
        setupSearchControls();

        // Apply settings to visualizer
        applyVisualizerSettings();
        
        // After setting up library controls or right after selectMusicFolderButton creation finish insert rescan button
        HBox libraryHeader = (HBox) selectMusicFolderButton.getParent();
        int index = libraryHeader.getChildren().indexOf(selectMusicFolderButton);
        libraryHeader.getChildren().add(RescanButtonFactory.createRescanButton(musicLibraryManager));
        
        // Initialize album view after everything is set up
        javafx.application.Platform.runLater(() -> {
            if ("All Songs".equals(libraryListView.getSelectionModel().getSelectedItem())) {
                showSongsWithAlbums();
            }
            
            // Check for first run
            checkFirstRun();
        });
        
        // Initialize visualizer controller
        visualizerController = new VisualizerController(audioPlayerService, settingsService);
        
        // Initialize audio conversion controller
        audioConversionController = new AudioConversionController(musicLibraryManager);
        
        // Initialize the main visualizer after scene is ready
        Platform.runLater(() -> {
            if (playPauseButton != null && playPauseButton.getScene() != null && playPauseButton.getScene().getWindow() != null) {
                Stage stage = (Stage) playPauseButton.getScene().getWindow();
                visualizerController.initializeMainVisualizer(stage);
            }
        });
        
        // Start auto-update check after initialization
        updateService.startAutoUpdateCheck();
        
        System.out.println("MainController initialized.");
    }
    
    private void checkFirstRun() {
        // Check if this is first run (no songs in library and no music folder set)
        if (musicLibraryManager.getSongCount() == 0 && musicLibraryManager.getCurrentMusicFolder() == null) {
            File selectedFolder = FirstRunWizard.show(selectMusicFolderButton.getScene().getWindow());
            if (selectedFolder != null) {
                // Suppress error dialogs during initial library scan
                audioPlayerService.setSuppressErrorDialogs(true);
                musicLibraryManager.scanMusicFolder(selectedFolder, true);
            }
        }
    }
    
    
    private void setupTableRowFactory() {
        songsTableView.setRowFactory(tv -> {
            TableRow<Song> row = new TableRow<Song>() {
                @Override
                protected void updateItem(Song song, boolean empty) {
                    super.updateItem(song, empty);
                    
                    // Highlight currently playing song
                    if (song != null && !empty) {
                        if (song.equals(audioPlayerService.getCurrentSong()) && audioPlayerService.isPlaying()) {
                            setStyle("-fx-background-color:rgba(50, 205, 50, 0.21);");
                        } else if (song.equals(audioPlayerService.getCurrentSong())) {
                            setStyle("-fx-background-color:rgba(50, 205, 50, 0.21);");
                        } else {
                            setStyle("");
                        }
                    } else {
                        setStyle("");
                    }
                }
            };
            
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Song selectedSong = row.getItem();
                    playbackController.playSelectedSong(selectedSong);
                }
            });
            
            // Attach context menu for playlist operations
            SongContextMenuProvider.attachContextMenu(row, playlists, playlistManager, playlistsListView, songs, pinboardPanel);
            
            // Enable drag-and-drop reordering when a playlist is selected
            row.setOnDragDetected(event -> {
                if (!row.isEmpty() && playlistsListView.getSelectionModel().getSelectedItem() != null) {
                    Dragboard db = row.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent cc = new ClipboardContent();
                    cc.putString(Integer.toString(row.getIndex()));
                    db.setContent(cc);
                    event.consume();
                }
            });

            row.setOnDragOver(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    if (row.getIndex() != draggedIndex) {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }
                event.consume();
            });

            row.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    int draggedIndex = Integer.parseInt(db.getString());
                    int dropIndex = row.isEmpty() ? songs.size() : row.getIndex();

                    if (draggedIndex != dropIndex) {
                        Song draggedSong = songs.remove(draggedIndex);
                        songs.add(dropIndex, draggedSong);

                        // Update playlist order in repository
                        Playlist selectedPl = playlistsListView.getSelectionModel().getSelectedItem();
                        if (selectedPl != null) {
                            selectedPl.setSongs(new ArrayList<>(songs));
                            playlistManager.updatePlaylistSongs(selectedPl.getId(), selectedPl.getSongs());
                        }

                        event.setDropCompleted(true);
                        songsTableView.getSelectionModel().select(dropIndex);
                    } else {
                        event.setDropCompleted(false);
                    }
                } else {
                    event.setDropCompleted(false);
                }
                event.consume();
            });
            return row;
        });
    }
    
    @FXML
    private void handleSelectMusicFolder() {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Music Folder");
        File selectedDirectory = directoryChooser.showDialog(selectMusicFolderButton.getScene().getWindow());
        
        if (selectedDirectory != null) {
            System.out.println("Selected music folder: " + selectedDirectory.getAbsolutePath());
            
            // Check if there's existing data and ask user what to do
            if (musicLibraryManager.hasExistingData()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Music Library");
                alert.setHeaderText("Existing music library found");
                alert.setContentText("You have " + musicLibraryManager.getSongCount() + 
                                   " songs in your library. What would you like to do?");
                
                ButtonType clearAndScan = new ButtonType("Clear & Scan New Folder");
                ButtonType addToLibrary = new ButtonType("Add to Existing Library");
                ButtonType cancel = new ButtonType("Cancel", ButtonType.CANCEL.getButtonData());
                
                alert.getButtonTypes().setAll(clearAndScan, addToLibrary, cancel);
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == clearAndScan) {
                        // Suppress error dialogs during library scan
                        audioPlayerService.setSuppressErrorDialogs(true);
                        musicLibraryManager.scanMusicFolder(selectedDirectory, true);
                        // Re-enable error dialogs after scan
                        audioPlayerService.setSuppressErrorDialogs(false);
                        // Check for convertible files and prompt user
                        audioConversionController.checkDirectoryForConversion(
                            selectMusicFolderButton.getScene().getWindow(), selectedDirectory);
                    } else if (response == addToLibrary) {
                        // Suppress error dialogs during library scan
                        audioPlayerService.setSuppressErrorDialogs(true);
                        musicLibraryManager.scanMusicFolder(selectedDirectory, false);
                        // Re-enable error dialogs after scan
                        audioPlayerService.setSuppressErrorDialogs(false);
                        // Check for convertible files and prompt user
                        audioConversionController.checkDirectoryForConversion(
                            selectMusicFolderButton.getScene().getWindow(), selectedDirectory);
                    }
                    // If cancel, do nothing
                });
            } else {
                // No existing data, just scan normally
                // Suppress error dialogs during library scan
                audioPlayerService.setSuppressErrorDialogs(true);
                musicLibraryManager.scanMusicFolder(selectedDirectory, true);
                // Re-enable error dialogs after scan
                audioPlayerService.setSuppressErrorDialogs(false);
                // Check for convertible files and prompt user
                audioConversionController.checkDirectoryForConversion(
                    selectMusicFolderButton.getScene().getWindow(), selectedDirectory);
            }
            
            // Update library stats after scanning
            if (pinboardPanel != null) {
                int totalSongs = musicLibraryManager.getSongCount();
                int totalAlbums = albumRepository.findAll().size();
                String musicFolder = selectedDirectory.getAbsolutePath();
                pinboardPanel.updateLibraryStats(totalSongs, totalAlbums, musicFolder);
            }
        }
    }
    
    @FXML
    private void handlePlayPause() {
        playbackController.handlePlayPause();
    }
    
    @FXML
    private void handlePrevious() {
        playbackController.handlePrevious();
    }
    
    @FXML
    private void handleNext() {
        playbackController.handleNext();
    }
    
    /**
     * Stop playback completely.
     */
    public void stopPlayback() {
        if (playbackController != null) {
            playbackController.stopPlayback();
        }
    }
    
    /**
     * Get information about the currently playing song.
     *
     * @return String with current song info, or null if no song is playing
     */
    public String getCurrentSongInfo() {
        if (playbackController != null) {
            return playbackController.getCurrentSongInfo();
        }
        return null;
    }
    
    private String formatDuration(long durationInSeconds) {
        long minutes = durationInSeconds / 60;
        long seconds = durationInSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    /**
     * Cleanup method to dispose of resources when the application is closing.
     */
    public void cleanup() {
        System.out.println("Shutting down application...");
        
        // Save library data to persistent storage
        if (musicLibraryManager != null) {
            musicLibraryManager.forceSave();
            System.out.println("Library data saved to storage");
        }
        
        // Save playlist data to persistent storage
        if (playlistManager != null) {
            playlistManager.forceSave();
            System.out.println("Playlist data saved to storage");
        }
        
        // Save favorites data to persistent storage
        if (favoritesService != null) {
            favoritesService.forceSave();
            System.out.println("Favorites data saved to storage");
        }
        
        // Cleanup visualizer controller
        if (visualizerController != null) {
            visualizerController.cleanup();
            System.out.println("Visualizer controller cleanup completed");
        }
        
        // Dispose of audio resources through playback controller
        if (playbackController != null) {
            playbackController.cleanup();
            System.out.println("Playback controller cleanup completed");
        }
        
        // Shutdown update service
        if (updateService != null) {
            updateService.shutdown();
            System.out.println("Update service shutdown");
        }
        
        System.out.println("Application shutdown complete");
    }
    
    /**
     * Sets up the playlist controls and ListView.
     */
    private void setupPlaylistControls() {
        // Set up the playlist ListView
        playlistsListView.setItems(filteredPlaylists);
        
        // Create playlist action handler
        PlaylistActionHandler playlistActionHandler = new PlaylistActionHandler(audioPlayerService, songs);
        playlistActionHandler.setOnPlaylistSelected(() -> {
            // Clear selection to show we're playing from a direct action
            playlistsListView.getSelectionModel().clearSelection();
            refreshPlaylistCells();
        });
        
        // Create custom cell factory for playlists
        playlistsListView.setCellFactory(listView -> {
            PlaylistCell cell = new PlaylistCell();
            
            // Set up play callback
            cell.setOnPlay(playlist -> {
                playlistActionHandler.playPlaylist(playlist);
                playingPlaylist = playlist;
                refreshPlaylistCells();
            });
            
            // Set up shuffle callback
            cell.setOnShuffle(playlist -> {
                playlistActionHandler.shufflePlaylist(playlist);
                playingPlaylist = playlist;
                refreshPlaylistCells();
            });
            
            // Set up delete callback
            cell.setOnDelete(playlist -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Delete Playlist");
                alert.setHeaderText("Delete playlist '" + playlist.getName() + "'?");
                alert.setContentText("This action cannot be undone.");
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        playlistManager.deletePlaylist(playlist.getId());
                    }
                });
            });
            
            // Set up rename callback
            cell.setOnRename(renameRequest -> {
                boolean success = playlistManager.renamePlaylist(
                    renameRequest.getPlaylist().getId(), 
                    renameRequest.getNewName()
                );
                
                if (!success) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Rename Failed");
                    alert.setHeaderText("Could not rename playlist");
                    alert.setContentText("A playlist with that name already exists or the name is invalid.");
                    alert.showAndWait();
                }
            });

            // After each update, adjust playing icon
            cell.itemProperty().addListener((o, oldPl, newPl) -> {
                cell.updatePlaying(newPl != null && newPl.equals(playingPlaylist) && audioPlayerService.isPlaying());
            });
            // Also update when playing state changes
            audioPlayerService.playingProperty().addListener((o, ov, nv) -> {
                Playlist item = cell.getItem();
                if (item != null) {
                    cell.updatePlaying(item.equals(playingPlaylist) && nv);
                }
            });
            return cell;
        });
        
        // Handle playlist selection to show songs
        playlistsListView.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldPlaylist, newPlaylist) -> {
                if (newPlaylist != null) {
                    songs.clear();
                    songs.addAll(newPlaylist.getSongs());
                    audioPlayerService.setPlaylist(songs);
                    playbackController.updatePlaylist(songs);
                    playingPlaylist = newPlaylist; // mark as playing
                    refreshPlaylistCells();
                }
            }
        );
    }
    
    /**
     * Handles the add playlist button action.
     */
    @FXML
    private void handleAddPlaylist() {
        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
        dialog.setTitle("New Playlist");
        dialog.setHeaderText("Create a new playlist");
        dialog.setContentText("Enter playlist name:");
        
        dialog.showAndWait().ifPresent(name -> {
            if (!name.trim().isEmpty()) {
                try {
                    playlistManager.createPlaylist(name.trim());
                } catch (IllegalArgumentException e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Creation Failed");
                    alert.setHeaderText("Could not create playlist");
                    alert.setContentText(e.getMessage());
                    alert.showAndWait();
                }
            }
        });
    }

    /**
     * Configures behavior of the library list ("All Songs", "Artists", etc.).
     * Currently handles showing all songs when the user selects "All Songs".
     */
    private void setupPinboardPanel() {
        pinboardPanel = new PinboardPanel();
        
        // Replace libraryListView with pinboard
        VBox parentBox = (VBox) libraryListView.getParent();
        int index = parentBox.getChildren().indexOf(libraryListView);
        parentBox.getChildren().set(index, pinboardPanel);
        
        // Add default pins for quick access
        pinboardPanel.addPinnedItem("all-songs", "All Songs", PinboardItem.ItemType.PLAYLIST, 
            () -> showAllSongs());
        pinboardPanel.addPinnedItem("albums", "Albums", PinboardItem.ItemType.ALBUM, 
             () -> showAlbumsOnly());
        pinboardPanel.addPinnedItem("favorites", "Favorites", PinboardItem.ItemType.PLAYLIST,
            () -> showFavorites());
        
        // Initialize library stats
        int totalSongs = musicLibraryManager.getSongCount();
        int totalAlbums = albumRepository.findAll().size();
        File musicFolderFile = musicLibraryManager.getCurrentMusicFolder();
        String musicFolder = musicFolderFile != null ? musicFolderFile.getAbsolutePath() : null;
        pinboardPanel.updateLibraryStats(totalSongs, totalAlbums, musicFolder);
        
        // Initialize listening stats with real data
        updateListeningStats();
        
        // Show all songs by default
        showAllSongs();
    }
    
    private void showAllSongs() {
        songs.setAll(musicLibraryManager.getAllSongs());
        audioPlayerService.setPlaylist(songs);
        playbackController.updatePlaylist(songs);
        playlistsListView.getSelectionModel().clearSelection();
        showSongsWithAlbums();
    }
    
    private void showAlbumsOnly() {
        showAlbumsView();
    }
    
    private void showFavorites() {
        List<Song> favoriteSongs = favoritesService.getFavoriteSongs(musicLibraryManager.getAllSongs());
        songs.clear();
        songs.addAll(favoriteSongs);
        audioPlayerService.setPlaylist(songs);
        playbackController.updatePlaylist(songs);
        playlistsListView.getSelectionModel().clearSelection();
        
        // Hide album view when showing favorites
        if (albumGridView != null) {
            albumGridView.setVisible(false);
        }
        songsTableView.setVisible(true);
    }
    
    private void updateListeningStats() {
        if (pinboardPanel != null && listeningStatsService != null) {
            int todayCount = listeningStatsService.getSongsPlayedToday();
            int weeklyCount = listeningStatsService.getSongsPlayedThisWeek();
            int monthlyCount = listeningStatsService.getSongsPlayedThisMonth();
            String mostPlayedDisplay = listeningStatsService.getMostPlayedSongDisplay();
            
            pinboardPanel.updateListeningStats(todayCount, weeklyCount, monthlyCount, mostPlayedDisplay);
        }
    }

    private void showSongsWithAlbums() {
        // Show albums at top and all songs below
        if (albumGridView == null) {
            // Use album repository to retrieve albums so that persisted edits (name, cover art) are reflected
            albumGridView = new AlbumGridView(albumRepository.findAll(), this::onAlbumSelected, albumRepository, songRepository);
            albumGridView.setPrefHeight(150);
            albumGridView.setMaxHeight(200);
            VBox container = (VBox) songsTableView.getParent();
            int tableIndex = container.getChildren().indexOf(songsTableView);
            container.getChildren().add(tableIndex, albumGridView);
        } else {
            albumGridView.refresh(albumRepository.findAll());
        }
        albumGridView.setVisible(true);
        songsTableView.setVisible(true);
        // Show all songs when in "All Songs" view
        songs.setAll(musicLibraryManager.getAllSongs());
    }

    private void showAlbumsView() {
        // Just reuse the same method since we always show albums above songs
        showSongsWithAlbums();
        // But clear songs since this is albums-only view
        songs.clear();
    }

    private void onAlbumSelected(Album album) {
        songs.clear();
        songs.setAll(album.getSongs());
        audioPlayerService.setPlaylist(songs);
        playbackController.updatePlaylist(songs);
        // Clear any playlist selection since we're now showing album songs
        playlistsListView.getSelectionModel().clearSelection();
    }

    /**
     * Handles clicking the “add to playlist” button for a selected song.
     */
    @FXML
    private void handleAddSongToPlaylist() {
        javafx.collections.ObservableList<Song> selectedSongs = songsTableView.getSelectionModel().getSelectedItems();
        if (selectedSongs == null || selectedSongs.isEmpty()) {
            return;
        }

        if (playlists.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Playlists");
            alert.setHeaderText("No playlists available");
            alert.setContentText("Please create a playlist first.");
            alert.showAndWait();
            return;
        }

        PlaylistSelectionPopup.show(addToPlaylistButton.getScene().getWindow(), playlists)
                .ifPresent(pl -> {
                    for (Song s : selectedSongs) {
                        playlistManager.addSongToPlaylist(pl.getId(), s);
                    }

                    if (pl.equals(playlistsListView.getSelectionModel().getSelectedItem())) {
                        songs.setAll(pl.getSongs());
                    }
                });
    }
    
    /**
     * Handle the audio conversion menu item click.
     */
    @FXML
    private void handleAudioConversion() {
        if (audioConversionController != null) {
            // Get the current music folder from the library manager
            File musicDir = musicLibraryManager.getCurrentMusicFolder();
            if (musicDir != null && musicDir.exists() && musicDir.isDirectory()) {
                // Use the directory-based conversion check
                audioConversionController.checkDirectoryForConversion(
                    selectMusicFolderButton.getScene().getWindow(), musicDir);
            } else {
                // Show dialog to select music folder first
                showInfo("No Music Folder", "Please select a music folder first using the 'Select Music Folder' button.");
            }
        } else {
            showError("Error", "Audio conversion service is not available.");
        }
    }
    
    /**
     * Handle the settings button click.
     */
    @FXML
    private void handleSettings() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            DialogPane dialogPane = loader.load();
            SettingsController controller = loader.getController();
            controller.setSettingsService(settingsService);
            controller.setUpdateService(updateService);
            
            // Create a custom dialog without default buttons
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setDialogPane(dialogPane);
            dialog.setTitle("Settings");
            dialog.initOwner(settingsButton.getScene().getWindow());
            
            // Apply CSS for icon button styling
            dialogPane.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            
            // Add a hidden button type to allow dialog to be closeable
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setVisible(false);
            dialog.getDialogPane().lookupButton(ButtonType.CANCEL).setManaged(false);
            
            // Handle custom buttons
            controller.getSaveButton().setOnAction(e -> {
                controller.saveSettings();
                applyVisualizerSettings();
                // Also update mini player visualizer settings if it exists
                if (miniPlayerWindow != null) {
                    miniPlayerWindow.updateVisualizerSettings();
                }
                dialog.setResult(ButtonType.OK);
                dialog.close();
            });
            
            controller.getCancelButton().setOnAction(e -> {
                dialog.setResult(ButtonType.CANCEL);
                dialog.close();
            });
            
            // Also handle ESC key to close dialog
            dialogPane.setOnKeyPressed(event -> {
                if (event.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
                    dialog.setResult(ButtonType.CANCEL);
                    dialog.close();
                }
            });
            
            dialog.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Could not open settings");
            alert.setContentText("An error occurred while opening the settings dialog.");
            alert.showAndWait();
        }
    }
    
    /**
     * Apply the current visualizer settings.
     */
    private void applyVisualizerSettings() {
        if (visualizerController != null) {
            visualizerController.applySettings();
        }
    }

    private void setupSearchControls() {
        if (songSearchButton != null) {
            songSearchButton.setOnAction(e -> songSearchField.requestFocus());
        }
        if (playlistSearchButton != null) {
            playlistSearchButton.setOnAction(e -> playlistSearchField.requestFocus());
        }

        // Delegate predicate logic to SearchManager
        SearchManager.bindSongSearch(songSearchField, filteredSongs);
        SearchManager.bindPlaylistSearch(playlistSearchField, filteredPlaylists);
    }

    /**
     * Set up window state monitoring to pause/resume visualizer when minimized.
     * This should be called from Main.java after the controller is loaded.
     * @param stage The primary stage of the application
     */
    public void setupWindowStateMonitoring(javafx.stage.Stage stage) {
        if (visualizerController != null) {
            visualizerController.setupWindowStateMonitoring(stage);
        }
        
        // Force refresh UI components to ensure they're properly rendered
        if (stage != null) {
            stage.iconifiedProperty().addListener((obs, wasMinimized, isMinimized) -> {
                if (!isMinimized) {
                    Platform.runLater(() -> {
                        if (songsTableView != null) {
                            songsTableView.refresh();
                        }
                        if (playlistsListView != null) {
                            playlistsListView.refresh();
                        }
                    }); // End of Platform.runLater
                } // End of else (window restored)
            }); // End of iconifiedProperty listener
        } // End of if (stage != null)
    }


    


    private void handleMissingFiles() {
        // Check if error dialogs are suppressed (e.g., during library scanning)
        if (audioPlayerService.isErrorDialogsSuppressed()) {
            System.out.println("Error dialogs suppressed - skipping missing files dialog");
            return;
        }
        
        javafx.application.Platform.runLater(() -> {
            if (pinboardPanel != null) {
                pinboardPanel.addActivity(ActivityFeedItem.ActivityType.FILES_MISSING, 
                    "Music files not found");
            }
            boolean shouldClear = MissingFilesDialog.show(playPauseButton.getScene().getWindow());
            if (shouldClear) {
                // Clear library and show folder selection
                musicLibraryManager.clearLibrary();
                handleSelectMusicFolder();
            }
        });
    }

    private void refreshPlaylistCells() {
        playlistsListView.refresh();
    }

    /**
     * Sets up the now playing bar at the bottom of the playlists list.
     */
    private void setupNowPlayingBar() {
        nowPlayingBar = new NowPlayingBar();
        
        // Get the parent VBox and replace control bar with a StackPane
        VBox bottomVBox = (VBox) controlBar.getParent();
        int index = bottomVBox.getChildren().indexOf(controlBar);
        bottomVBox.getChildren().remove(controlBar);
        
        // Create StackPane to overlay now playing on control bar
        StackPane stackPane = new StackPane();
        stackPane.getChildren().addAll(controlBar, nowPlayingBar);
        StackPane.setAlignment(nowPlayingBar, Pos.CENTER_LEFT);
        StackPane.setAlignment(controlBar, Pos.CENTER);
        
        bottomVBox.getChildren().add(index, stackPane);
        
        // Set size constraints
        nowPlayingBar.setPrefWidth(250);
        nowPlayingBar.setMaxWidth(350);
        
        // Update now playing bar on song change / play state
        audioPlayerService.currentSongProperty().addListener((obs, oldS, newS) -> nowPlayingBar.update(newS, audioPlayerService.isPlaying()));
        audioPlayerService.playingProperty().addListener((obs, ov, nv) -> nowPlayingBar.update(audioPlayerService.getCurrentSong(), nv));
    }
    
    private void setupFavoriteColumn() {
        // Set up the favorite column with a custom cell factory
        favoriteColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleBooleanProperty(cellData.getValue().isFavorite()));
        
        favoriteColumn.setCellFactory(column -> new TableCell<Song, Boolean>() {
            private final Button favoriteButton = new Button();
            private final Image favIcon = new Image(getClass().getResourceAsStream("/images/icons/fav.png"));
            private final ImageView imageView = new ImageView(favIcon);
            
            {
                // Set up button appearance
                favoriteButton.setGraphic(imageView);
                favoriteButton.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; " +
                                      "-fx-padding: 0; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
                favoriteButton.setPrefWidth(24);
                favoriteButton.setPrefHeight(24);
                imageView.setFitWidth(20);
                imageView.setFitHeight(20);
                imageView.setPreserveRatio(true);
                
                // Handle button click
                favoriteButton.setOnAction(event -> {
                    Song song = getTableView().getItems().get(getIndex());
                    if (song != null) {
                        boolean isFavorite = favoritesService.toggleFavorite(song);
                        updateButtonAppearance(isFavorite);
                        
                        // Update pinboard if showing favorites
                        if (pinboardPanel != null) {
                            pinboardPanel.addActivity(ActivityFeedItem.ActivityType.PLAYLIST_SAVED,
                                isFavorite ? "Added to favorites: " + song.getTitle() 
                                           : "Removed from favorites: " + song.getTitle());
                        }
                        
                        // Refresh table to update favorite icon states
                        songsTableView.refresh();
                    }
                });
            }
            
            @Override
            protected void updateItem(Boolean isFavorite, boolean empty) {
                super.updateItem(isFavorite, empty);
                
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Song song = getTableRow().getItem();
                    updateButtonAppearance(song.isFavorite());
                    setGraphic(favoriteButton);
                    setAlignment(Pos.CENTER);
                }
            }
            
            private void updateButtonAppearance(boolean isFavorite) {
                if (isFavorite) {
                    imageView.setOpacity(1.0);
                } else {
                    imageView.setOpacity(0.3);
                }
            }
        });
    }

            /**
             * Handles the mini player functionality.
             * Creates and shows a mini player window, or hides it if already showing.
             */
            @FXML
            private void handleMiniPlayer() {
                if (miniPlayerWindow == null) {
                    // Get the main stage from any UI component
                    Stage mainStage = (Stage) playPauseButton.getScene().getWindow();
                    
                    // Create the mini player window
                    miniPlayerWindow = new MiniPlayerWindow(audioPlayerService, mainStage, settingsService, favoritesService, playlistManager, albumRepository);
                    
                    // Listen for show song in library events from mini player
                    mainStage.addEventHandler(MiniPlayerWindow.ShowSongInLibraryEvent.SHOW_SONG_IN_LIBRARY, event -> {
                        Song songToShow = event.getSong();
                        if (songToShow != null) {
                            // Show all songs view
                            showAllSongs();
                            
                            // Find and select the song in the table
                            for (int i = 0; i < songsTableView.getItems().size(); i++) {
                                if (songsTableView.getItems().get(i).equals(songToShow)) {
                                    songsTableView.getSelectionModel().select(i);
                                    songsTableView.scrollTo(i);
                                    break;
                                }
                            }
                        }
                    });
                    
                    // When mini player is closed, set reference to null
                    miniPlayerWindow.getStage().setOnHidden(e -> {
                        // Don't null the reference, just hide it
                    });
                }
                
                if (miniPlayerWindow.isShowing()) {
                    // If already showing, hide it
                    miniPlayerWindow.hide();
                } else {
                    // Show the mini player
                    miniPlayerWindow.show();
                    
                    // Optionally minimize the main window
                    Stage mainStage = (Stage) playPauseButton.getScene().getWindow();
                    mainStage.setIconified(true);
                }
            }
            
    /**
     * Updates the album art display for the current song.
     * Uses AlbumArtLoader utility to load album art from metadata.
     */
    private void updateAlbumArt(Song song) {
        if (song == null) {
            // Clear album art and labels when no song
            if (albumArtImageView != null) {
                albumArtImageView.setImage(null);
            }
            if (albumArtImageView2 != null) {
                albumArtImageView2.setImage(null);
            }
            if (songTitleLabel != null) {
                songTitleLabel.setText("");
            }
            if (songArtistLabel != null) {
                songArtistLabel.setText("");
            }
            return;
        }
        
        // Update song info labels
        if (songTitleLabel != null) {
            songTitleLabel.setText(song.getTitle());
        }
        if (songArtistLabel != null) {
            songArtistLabel.setText(song.getArtist());
        }
        
        // Load album art asynchronously
        if (albumArtContainer != null && albumArtImageView != null && albumArtImageView2 != null) {
            AlbumArtLoader.loadAlbumArt(song)
                .thenAcceptAsync(image -> transitionToImage(image), Platform::runLater);
        }
    }
    
    /**
     * Transitions to a new album art image with a fade effect.
     * Uses two ImageViews to create smooth crossfade transitions.
     */
    private void transitionToImage(Image newImage) {
        if (albumArtImageView == null || albumArtImageView2 == null || albumArtContainer == null) {
            return;
        }
        
        // Determine which ImageView is currently visible
        ImageView currentView = albumArtImageView.getOpacity() > 0 ? albumArtImageView : albumArtImageView2;
        ImageView nextView = currentView == albumArtImageView ? albumArtImageView2 : albumArtImageView;
        
        // Set the new image on the hidden view
        nextView.setImage(newImage);
        
        // Create fade out transition for current view
        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), currentView);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        
        // Create fade in transition for next view
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), nextView);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        // Play both transitions
        fadeOut.play();
        fadeIn.play();
    }
    
    @FXML
    private void handleCheckForUpdates() {
        // Disable the button/menu item while checking
        // Note: We don't have direct access to the menu item here, but we can show progress
        
        // Show initial status
        if (pinboardPanel != null) {
            pinboardPanel.addActivity(ActivityFeedItem.ActivityType.SCAN_COMPLETE,
                "Checking for updates...");
        }
        
        // Call checkForUpdates on the updateService
        updateService.checkForUpdates()
            .thenAccept(updateInfo -> {
                // This runs on a background thread, so use Platform.runLater for UI updates
                Platform.runLater(() -> {
                    if (updateInfo != null) {
                        // Update is available - show the UpdateDialog
                        UpdateDialog updateDialog = new UpdateDialog(updateService, updateInfo);
                        updateDialog.initOwner(playPauseButton.getScene().getWindow());
                        
                        // Show the dialog and wait for it to close
                        updateDialog.showAndWait();
                        
                        // Log the activity
                        if (pinboardPanel != null) {
                            pinboardPanel.addActivity(ActivityFeedItem.ActivityType.SCAN_COMPLETE,
                                "Update available: version " + updateInfo.getVersion());
                        }
                    } else {
                        // No updates found
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Check for Updates");
                        alert.setHeaderText("No Updates Available");
                        alert.setContentText("You are running the latest version of SiMP3 (" +
                            updateService.getCurrentVersion() + ")");
                        alert.initOwner(playPauseButton.getScene().getWindow());
                        alert.showAndWait();
                        
                        if (pinboardPanel != null) {
                            pinboardPanel.addActivity(ActivityFeedItem.ActivityType.SCAN_COMPLETE,
                                "No updates available - running latest version");
                        }
                    }
                });
            })
            .exceptionally(throwable -> {
                // Handle errors
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Update Check Failed");
                    alert.setHeaderText("Could not check for updates");
                    alert.setContentText("An error occurred while checking for updates. " +
                        "Please check your internet connection and try again.\n\n" +
                        "Error: " + throwable.getMessage());
                    alert.initOwner(playPauseButton.getScene().getWindow());
                    alert.showAndWait();
                    
                    if (pinboardPanel != null) {
                        pinboardPanel.addActivity(ActivityFeedItem.ActivityType.FILES_MISSING,
                            "Update check failed: " + throwable.getMessage());
                    }
                });
                return null;
            });
    }

    private void syncAlbumsWithRepository() {
        // Ensure that all albums detected by the library engine are persisted so that
        // users can immediately edit them and have their changes stick across sessions.

        List<Album> persisted = albumRepository.findAll();
        List<Album> engineAlbums = libraryService.getAllAlbums();

        for (Album engineAlbum : engineAlbums) {
            boolean exists = persisted.stream().anyMatch(persistedAlbum -> {
                // Match by title first
                if (persistedAlbum.getTitle() != null && engineAlbum.getTitle() != null &&
                    persistedAlbum.getTitle().equalsIgnoreCase(engineAlbum.getTitle())) {
                    return true;
                }

                // Fallback: If any song file path overlaps, consider it the same album
                if (persistedAlbum.getSongs() != null && engineAlbum.getSongs() != null) {
                    return persistedAlbum.getSongs().stream().anyMatch(ps ->
                        engineAlbum.getSongs().stream().anyMatch(es -> es.getFilePath() != null && ps.getFilePath() != null &&
                            ps.getFilePath().equals(es.getFilePath())));
                }
                return false;
            });

            if (!exists) {
                // Assign an ID via repository save (it will auto-generate one)
                albumRepository.save(engineAlbum);
            }
        }
    }
    
    // Implementation of IControllerCommunication interface methods
    
    @Override
    public void onListeningStatsUpdated() {
        updateListeningStats();
    }
    
    @Override
    public void onPlaybackStateChanged() {
        songsTableView.refresh();
        refreshPlaylistCells();
        if (nowPlayingBar != null) {
            nowPlayingBar.update(audioPlayerService.getCurrentSong(), audioPlayerService.isPlaying());
        }
    }
    
    @Override
    public void onCurrentSongChanged(Song newSong) {
        // Update album art and song info - this is already handled by the AudioController
        // But we can add any additional UI updates here if needed
        songsTableView.refresh();
    }
    
    @Override
    public void onPlaylistUpdated(ObservableList<Song> playlist) {
        // Update the main controller's playlist reference
        // This is already handled in the existing code
    }
    
    @Override
    public void requestPlaySong(Song song) {
        if (playbackController != null) {
            playbackController.playSelectedSong(song);
        }
    }
    
    @Override
    public void requestRefreshSongTable() {
        if (songsTableView != null) {
            songsTableView.refresh();
        }
    }
    
    /**
     * Show an information dialog.
     */
    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Show an error dialog.
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    @Override
    public void onVisualizerUpdateRequired() {
        // Force visualizer state update when song is changed via double-click
        if (visualizerController != null) {
            Platform.runLater(() -> {
                visualizerController.updateMainVisualizerState();
            });
        }
    }
}
