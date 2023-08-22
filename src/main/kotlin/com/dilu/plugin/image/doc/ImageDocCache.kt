package com.dilu.plugin.image.doc

import com.dilu.plugin.image.doc.utils.Utils
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.guessProjectDir
import org.jetbrains.annotations.NotNull
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

class ImageDocCache(private val myCache: ConcurrentMap<URL, File>) : ConcurrentHashMap<URL, File>() {
    override fun get(key: URL): File? {
        if (myCache.containsKey(key)) {
            return myCache[key]
        }
        val localImageDoc = Utils.getLocalImageDoc(key)
        if (localImageDoc != null) {
            put(key, localImageDoc)
            return localImageDoc
        }
        return null
    }

    override fun put(@NotNull key: URL, @NotNull value: File): File? {
        return myCache.put(key, value)
    }
}