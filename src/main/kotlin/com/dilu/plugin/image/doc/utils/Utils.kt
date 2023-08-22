package com.dilu.plugin.image.doc.utils

import com.dilu.plugin.image.doc.ImageDocConstant
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import java.io.File
import java.net.URL
import java.nio.file.Path

object Utils {
    /**
     * Get the image doc directory for the project.
     */
    fun getImageDocDir(project: Project): VirtualFile? {
        val projectDir = project.guessProjectDir() ?: return null
        val path = projectDir.path + File.separator + ".image-doc"
        FileUtil.createDirectory(File(path))
        return VirtualFileManager.getInstance().findFileByNioPath(Path.of(path));
    }

    fun getLocalImageDoc(key: String): File? {
        return getLocalImageDoc(URL(key))
    }

    fun getLocalImageDoc(key: URL): File? {
        if (!key.protocol.equals("file")) {
            return null
        }
        val project = findProject(key.path) ?: return null
        val imageDocDir = getImageDocDir(project) ?: return null
        val idx = key.path.indexOf(ImageDocConstant.IMAGE_DOC_DIR_NAME_WITH_SLASH)
        if (idx == -1) {
            return null
        }
        val name = key.path.substring(idx + ImageDocConstant.IMAGE_DOC_DIR_NAME_WITH_SLASH.length)
        val file = File(imageDocDir.path + File.separator + name)
        if (!file.exists()) {
            return null
        }
        return file
    }

    fun findProject(path: String): Project? {
        for (project in ProjectUtil.getOpenProjects()) {
            val projectDir = project.guessProjectDir() ?: continue
            if (path.startsWith(projectDir.path)) {
                return project
            }
        }
        return null
    }

}