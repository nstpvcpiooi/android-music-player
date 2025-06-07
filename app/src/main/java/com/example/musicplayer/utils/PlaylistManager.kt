package com.example.musicplayer.utils

import com.example.musicplayer.model.MusicPlaylist

/**
 * Singleton class to manage playlist data across the application
 * This replaces the static musicPlaylist that was previously in PlaylistActivity
 */
object PlaylistManager {
    var musicPlaylist: MusicPlaylist = MusicPlaylist()
}
