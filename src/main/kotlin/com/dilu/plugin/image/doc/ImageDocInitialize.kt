package com.dilu.plugin.image.doc

import com.dilu.plugin.image.doc.utils.ReflectUtils
import com.intellij.codeInsight.documentation.render.CachingDataReader
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.util.ReflectionUtil
import org.jetbrains.annotations.NotNull
import java.io.File
import java.net.URL
import java.util.concurrent.ConcurrentMap

class ImageDocInitialize : EditorFactoryListener {
    private var logger = Logger.getInstance(ImageDocInitialize::class.java)
    private var initialized = false

    override fun editorCreated(@NotNull event: EditorFactoryEvent) {
        init()
    }

    private fun init() {
        if (initialized) {
            return
        }
        try {
            val documentImageCachingDataReader = CachingDataReader.getInstance()
            val myCacheField = ReflectionUtil.findField(
                CachingDataReader::class.java, ConcurrentMap::class.java, "myCache"
            )
            val myCache = ReflectionUtil.getFieldValue<ConcurrentMap<URL, File>>(
                myCacheField,
                documentImageCachingDataReader
            ) ?: return
            val field = ReflectUtils.getField(CachingDataReader::class.java, "myCache")
            ReflectUtils.setNoneStaticFieldValueByUnsafe(documentImageCachingDataReader, field, ImageDocCache(myCache))
        } catch (e: Exception) {
            logger.error("Failed to init ImageDoc", e)
        } finally {
            initialized = true
        }
    }
}
