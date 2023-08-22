package com.dilu.plugin.image.doc.utils

import java.io.File
import java.util.*

object FileUtils {

    fun getFileNameWithoutExtension(file: File): String {
        return getFileNameWithoutExtension(file.name)
    }

    fun getFileNameWithoutExtension(fileNameWithExt: String): String {
        val lastIndexOf = fileNameWithExt.lastIndexOf('.')
        return if (lastIndexOf > 0) {
            fileNameWithExt.substring(0, lastIndexOf)
        } else fileNameWithExt
    }

    fun isImage(fileName: String): Boolean {
        val imageExtensions = arrayOf("jpg", "jpeg", "png", "bmp", "gif")
        val lastDotIndex = fileName.lastIndexOf('.')
        val fileExtension = fileName.substring(lastDotIndex + 1)
        return imageExtensions.contains(fileExtension.toLowerCase(Locale.getDefault()))
    }

    fun getFileExtension(file: File): String {
        return getFileExtension(file.name)
    }

    fun getFileExtension(fileNameWithExt: String): String {
        val lastIndexOf = fileNameWithExt.lastIndexOf('.')
        return if (lastIndexOf > 0) {
            fileNameWithExt.substring(lastIndexOf + 1)
        } else ""
    }
}