package com.openselena.omniget.download

import java.io.File

internal fun collectDownloadedFiles(output: String, directory: File): List<File> {
    val printed = output.lineSequence()
        .map(String::trim)
        .filter { it.startsWith("/") && File(it).isFile }
        .toList()
    val scanned = directory.listFiles()
        .orEmpty()
        .filter {
            it.isFile &&
                !it.name.endsWith(".part") &&
                !it.name.endsWith(".ytdl")
        }
        .map(File::getAbsolutePath)
    return (printed + scanned)
        .distinct()
        .map(::File)
        .filter(File::isFile)
        .sortedByDescending(File::length)
}
