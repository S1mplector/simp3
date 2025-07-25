package com.musicplayer.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import com.musicplayer.data.models.Song;

/**
 * Utility class for scanning music folders and extracting metadata from audio files.
 */
public class MusicScanner {
    
    private static final String[] SUPPORTED_EXTENSIONS = {
        ".mp3", ".m4a", ".flac", ".wav", ".ogg", ".opus", 
        ".aac", ".wma", ".mp4", ".m4b", ".aif", ".aiff"
    };
    
    /**
     * Scans a directory recursively for music files and extracts their metadata.
     * 
     * @param directory The directory to scan
     * @return List of Song objects with extracted metadata
     */
    public static List<Song> scanDirectory(File directory) {
        List<Song> songs = new ArrayList<>();
        
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            System.err.println("Invalid directory provided for scanning: " + directory);
            return songs;
        }
        
        System.out.println("Starting recursive scan of: " + directory.getAbsolutePath());
        scanDirectoryRecursive(directory, songs, 0);
        System.out.println("Scan completed. Found " + songs.size() + " music files total.");
        return songs;
    }
    
    private static void scanDirectoryRecursive(File directory, List<Song> songs, int depth) {
        File[] files = directory.listFiles();
        if (files == null) {
            System.err.println("Could not read directory: " + directory.getAbsolutePath());
            return;
        }
        
        String indent = "  ".repeat(depth);
        System.out.println(indent + "Scanning: " + directory.getName() + " (" + files.length + " items)");
        
        for (File file : files) {
            // Skip hidden files and system directories
            if (file.isHidden() || file.getName().startsWith(".")) {
                continue;
            }
            
            if (file.isDirectory()) {
                // Recursively scan subdirectories
                System.out.println(indent + "  📁 Entering subdirectory: " + file.getName());
                scanDirectoryRecursive(file, songs, depth + 1);
            } else if (file.isFile() && isSupportedAudioFile(file)) {
                // Extract metadata from audio file
                System.out.println(indent + "  🎵 Found music file: " + file.getName());
                Song song = extractMetadata(file);
                if (song != null) {
                    songs.add(song);
                } else {
                    System.err.println(indent + "    ❌ Failed to extract metadata from: " + file.getName());
                }
            }
        }
    }
    
    private static boolean isSupportedAudioFile(File file) {
        String fileName = file.getName().toLowerCase();
        return Arrays.stream(SUPPORTED_EXTENSIONS)
                .anyMatch(fileName::endsWith);
    }
    
    private static Song extractMetadata(File file) {
        try {
            AudioFile audioFile = AudioFileIO.read(file);
            Tag tag = audioFile.getTag();
            
            Song song = new Song();
            song.setFilePath(file.getAbsolutePath());
            
            // Extract basic metadata
            if (tag != null) {
                song.setTitle(getTagValue(tag, FieldKey.TITLE, file.getName()));
                song.setArtist(getTagValue(tag, FieldKey.ARTIST, "Unknown Artist"));
                song.setAlbum(getTagValue(tag, FieldKey.ALBUM, "Unknown Album"));
                song.setGenre(getTagValue(tag, FieldKey.GENRE, "Unknown"));
                
                // Parse track number
                String trackStr = tag.getFirst(FieldKey.TRACK);
                if (!trackStr.isEmpty()) {
                    try {
                        // Handle track numbers like "1/12" or just "1"
                        String trackNumber = trackStr.split("/")[0];
                        song.setTrackNumber(Integer.parseInt(trackNumber));
                    } catch (NumberFormatException e) {
                        song.setTrackNumber(0);
                    }
                }
            } else {
                // Fallback if no tags are available
                song.setTitle(getFileNameWithoutExtension(file));
                song.setArtist("Unknown Artist");
                song.setAlbum("Unknown Album");
                song.setGenre("Unknown");
                song.setTrackNumber(0);
            }
            
            // Get duration from audio file
            if (audioFile.getAudioHeader() != null) {
                song.setDuration(audioFile.getAudioHeader().getTrackLength());
            }
            
            return song;
            
        } catch (Exception e) {
            System.err.println("Error reading metadata from file: " + file.getAbsolutePath());
            System.err.println("Error: " + e.getMessage());
            
            // Create a basic song object with file information
            Song song = new Song();
            song.setFilePath(file.getAbsolutePath());
            song.setTitle(getFileNameWithoutExtension(file));
            song.setArtist("Unknown Artist");
            song.setAlbum("Unknown Album");
            song.setGenre("Unknown");
            song.setDuration(0);
            song.setTrackNumber(0);
            
            return song;
        }
    }
    
    private static String getTagValue(Tag tag, FieldKey field, String defaultValue) {
        try {
            String value = tag.getFirst(field);
            return (value != null && !value.trim().isEmpty()) ? value.trim() : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private static String getFileNameWithoutExtension(File file) {
        String fileName = file.getName();
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex > 0) ? fileName.substring(0, lastDotIndex) : fileName;
    }
}
