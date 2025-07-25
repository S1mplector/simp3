# Mini Player Feature

## Overview

The Mini Player is a compact, floating window that provides essential playback controls while allowing users to continue working in other applications. It's designed to be unobtrusive yet functional, offering quick access to music controls without switching back to the main application.

## Features

### Core Functionality
- **Compact Design**: Small footprint (450x90 pixels) that doesn't obstruct your work
- **Always-on-Top Option**: Keep the player visible above all other windows
- **Drag & Drop**: Click and drag anywhere on the player to reposition it
- **Auto-Show on Minimize**: Automatically appears when the main window is minimized while music is playing

### Playback Controls
- Play/Pause button
- Previous/Next track navigation
- Progress slider with seek functionality
- Volume control slider
- Current time and total duration display

### Song Information
- Album artwork display (60x60 pixels)
- Song title with ellipsis for long titles
- Artist and album information
- Real-time updates when tracks change

### Window Controls
- Pin/Unpin button for always-on-top toggle
- Close button to hide the mini player
- Double-click anywhere to restore the main window

## Usage

### Opening the Mini Player
1. **Via Button**: Click the mini player button in the main window's top toolbar
2. **Via Keyboard**: Press `Ctrl+M` to toggle the mini player
3. **Auto-Show**: Minimize the main window while music is playing

### Context Menu
Right-click anywhere on the mini player to access:
- Show Main Window
- Toggle Always on Top
- Close Mini Player

### Keyboard Shortcuts
- `Ctrl+M`: Toggle mini player visibility
- `Double-click`: Restore main window and hide mini player

## Audio Visualizer

### Overview
The mini player includes an integrated audio visualizer that provides real-time visual feedback of the music being played. The visualizer displays frequency spectrum data as animated bars radiating from the center in a circular pattern.

### Features
- **Circular Spectrum Display**: 32 frequency bars arranged in a radial pattern
- **Smooth Animations**: 60 FPS rendering with configurable rotation effects
- **Dynamic Colors**: Gradient colors that respond to audio intensity
- **Glow Effects**: Optional glow effect for enhanced visual appeal
- **Format Support**: Works with MP3 and M4A audio formats (JavaFX limitation)

### Usage
- **Toggle Visualizer**: Right-click on the album art area and select "Toggle Visualizer"
- **Keyboard Shortcut**: Press 'V' while the mini player is focused to toggle the visualizer
- **Automatic Activation**: The visualizer automatically appears when playing supported formats

### Supported Audio Formats
Due to JavaFX MediaPlayer limitations, the visualizer only works with:
- MP3 (.mp3)
- M4A (.m4a, .mp4)
- AAC (.aac)

For other formats (WAV, FLAC, OGG), the visualizer will remain hidden as spectrum data is not available.

### Visual Design
- **Center Circle**: Pulsating gradient circle at the center
- **Frequency Bars**: Height represents amplitude, radiating outward
- **Color Scheme**: Green gradient by default (customizable via VisualizerConfig)
- **Rotation**: Slow clockwise rotation for dynamic effect

## Technical Implementation

### Architecture
- **MiniPlayerWindow.java**: Main class handling the mini player UI and logic
- **mini-player.css**: Dedicated stylesheet for mini player styling
- **Integration**: Seamlessly integrated with AudioPlayerService for real-time updates

### Key Components
1. **Window Management**
   - Undecorated stage for custom appearance
   - Always-on-top functionality
   - Position persistence (top-right by default)

2. **Data Binding**
   - Bidirectional volume binding
   - Progress tracking with seek support
   - Real-time song information updates

3. **User Experience**
   - Smooth hover effects on controls
   - Tooltip hints for all buttons
   - Responsive to window state changes

### Future Enhancements
- Album artwork loading from metadata
- Playlist navigation
- Customizable themes
- Position memory between sessions
- Keyboard shortcuts within mini player
- Notification integration

## Design Decisions

1. **Size**: Kept compact at 450x90 pixels to minimize screen real estate usage
2. **Position**: Default to top-right corner where it's accessible but unobtrusive
3. **Controls**: Only essential controls to maintain simplicity
4. **Styling**: Consistent with main application theme while being visually distinct

## Known Limitations

1. Album artwork currently shows placeholder only
2. No playlist information displayed
3. Volume control is basic (no mute button)
4. Position resets on application restart

## User Feedback

The mini player is designed to enhance the user experience by providing:
- Quick access to controls without context switching
- Minimal visual distraction
- Familiar playback controls
- Smooth integration with the main application