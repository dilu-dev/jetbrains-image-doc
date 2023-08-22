package com.dilu.plugin.image.doc

import com.dilu.plugin.image.doc.utils.Utils
import com.dilu.plugin.image.doc.utils.FileUtils
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import org.apache.commons.io.IOUtils
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.awt.image.MultiResolutionImage
import java.awt.image.RenderedImage
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO

class ImageDocPasteProvider : PasteProvider {
    private val logger = logger<ImageDocPasteProvider>()

    override fun isPastePossible(dataContext: DataContext): Boolean {
        return isPasteEnabled(dataContext)
    }

    override fun isPasteEnabled(dataContext: DataContext): Boolean {
        return dataContext.getData(CommonDataKeys.VIRTUAL_FILE) != null
                && dataContext.getData(CommonDataKeys.PSI_FILE) != null
                && dataContext.getData(CommonDataKeys.EDITOR) != null
                && dataContext.getData(CommonDataKeys.PROJECT) != null
                && isImageFlavorAvailable()
    }

    /**
     * 执行粘贴操作
     */
    override fun performPaste(dataContext: DataContext) {
        val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return
        val psiFile = dataContext.getData(CommonDataKeys.PSI_FILE) ?: return
        val editor = dataContext.getData(CommonDataKeys.EDITOR) ?: return
        val imageDocDir = Utils.getImageDocDir(project) ?: return

        val pasteImage = getImageToPaste()
        if (pasteImage == null) {
            logger.error("Failed to get data from the clipboard. Nothing to paste. Aborting operation.")
            return
        }
        if (pasteImage.name == null) {
            pasteImage.name = FileUtils.getFileNameWithoutExtension(psiFile.name)
        }

        runWriteAction {
            // 计算可用的文件名
            var fileExt = "png"
            if (pasteImage.imageFile != null) {
                fileExt = FileUtils.getFileExtension(pasteImage.imageFile!!)
            }
            val nextAvailableName = VfsUtil.getNextAvailableName(imageDocDir, pasteImage.name!!, fileExt)

            // 保存文件
            if (!saveImage(imageDocDir, nextAvailableName, pasteImage)) return@runWriteAction

            // 找到合适的大小
            val (width, height) = calcFitnessSize(pasteImage)

            // 在源码中插入图片
            val path = ImageDocConstant.IMAGE_DOC_DIR_NAME + "/" + nextAvailableName
            var text = "<img src=\"$path\" height=\"$height\" width=\"$width\" />"
            val psiElement = getCursorPsiElement(psiFile, editor)
            if (!isInComments(psiElement)) {
                text = "// $text"
            }
            editor.document.insertString(editor.caretModel.offset, text)
        }
    }

    /**
     * 保存粘贴板中的图片到文件
     */
    private fun saveImage(
        imageDocDir: VirtualFile,
        nextAvailableName: String,
        pasteImage: PasteImage
    ): Boolean {
        val imageFile = try {
            imageDocDir.createChildData(this, nextAvailableName)
        } catch (ioException: IOException) {
            logger.error("Failed to create a pasted image file due to I/O error. Aborting operation.", ioException)
            return false
        }

        try {
            imageFile.getOutputStream(this)
                .use { output ->
                    if (pasteImage.imageFile != null && pasteImage.imageFile!!.exists()) {
                        pasteImage.imageFile!!.inputStream().use { input ->
                            IOUtils.copy(input, output)
                        }
                    } else {
                        ImageIO.write(pasteImage.image, "png", output)
                    }
                }
        } catch (ioException: IOException) {
            logger.error("Failed to save a pasted image to a file due to I/O error. Aborting operation", ioException)
            try {
                imageFile.delete(this)
            } catch (ignore: IOException) {
                // just skip it
            }
            return false
        }
        return true
    }

    /**
     * 计算合适的图片大小
     */
    private fun calcFitnessSize(pasteImage: PasteImage): Pair<Int, Int> {
        var width = pasteImage.image.width
        var height = pasteImage.image.height
        if (height > 300) {
            height = 300
            width = (width * 300.0 / pasteImage.image.height).toInt()
        }
        if (width > 1000) {
            width = 1000
            height = (height * 1000.0 / pasteImage.image.width).toInt()
        }
        return Pair(width, height)
    }

    /**
     * 获取粘贴板中的图片信息
     */
    private fun getImageToPaste(): PasteImage? {
        val pasteContents = CopyPasteManager.getInstance().contents ?: return null

        // 先检查粘贴板中，是否存在图片文件的复制
        try {
            val files = pasteContents.getTransferData(DataFlavor.javaFileListFlavor)
            for (file in files as List<*>) {
                if (file is java.io.File) {
                    // 判断文件是否为图片
                    val image: BufferedImage? = try {
                        ImageIO.read(file).toBufferedImage()
                    } catch (e: IOException) {
                        null
                    }
                    if (image != null && image.width > 0 && image.height > 0) {
                        return PasteImage(image, file, FileUtils.getFileNameWithoutExtension(file))
                    }
                }
            }
        } catch (ignore: UnsupportedFlavorException) {
        }

        // 再检查粘贴板中是否存在图片截图复制
        val image = try {
            pasteContents.getTransferData(DataFlavor.imageFlavor)
        } catch (ioException: IOException) {
            logger.error(
                "Failed to get data from the clipboard. Data is no longer available. Aborting operation.",
                ioException
            )
            return null
        }.let {
            when (it) {
                is MultiResolutionImage -> it.resolutionVariants.firstOrNull()?.toBufferedImage()
                is BufferedImage -> it
                is Image -> it.toBufferedImage()
                else -> null
            }
        }
        return image?.let { PasteImage(it, null, null) }
    }

    /**
     * 判断光标是否在注释中
     */
    private fun isInComments(psiElement: PsiElement?): Boolean {
        var element = psiElement
        while (element != null) {
            if (element is PsiComment) {
                return true
            }
            element = element.parent
        }
        return false
    }

    /**
     * 获取光标所在的 PsiElement
     */
    private fun getCursorPsiElement(psiFile: PsiFile, editor: Editor): PsiElement? {
        var offset = editor.caretModel.offset
        val lineNumber = editor.document.getLineNumber(offset)
        val lineStartOffset = editor.document.getLineStartOffset(lineNumber)

        while (offset >= lineStartOffset) {
            val psiElement = psiFile.viewProvider.findElementAt(offset)
            if (psiElement is PsiWhiteSpace) {
                offset -= 1
                continue
            }
            return psiElement
        }
        return null
    }


    // 判断粘贴板内容是否为图片
    private fun isImageFlavorAvailable(): Boolean {
        if (CopyPasteManager.getInstance().areDataFlavorsAvailable(DataFlavor.imageFlavor)) {
            return true
        }
        try {
            val pasteContents = CopyPasteManager.getInstance().contents ?: return false
            val files = pasteContents.getTransferData(DataFlavor.javaFileListFlavor)
            for (file in files as List<*>) {
                if (file is java.io.File) {
                    // 判断文件是否为图片
                    if (FileUtils.isImage(file.name)) {
                        return true
                    }
                }
            }
        } catch (ignore: UnsupportedFlavorException) {
        }
        return false
    }
}

class PasteImage(var image: RenderedImage, var imageFile: File?, var name: String?)

public fun Image.toBufferedImage() = let { img ->
    when (img) {
        is BufferedImage -> img
        else -> {
            // Create a buffered image with transparency
            val bufferedImage = BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB)

            // Draw the image on to the buffered image
            val bGr = bufferedImage.createGraphics()
            bGr.drawImage(img, 0, 0, null)
            bGr.dispose()

            bufferedImage
        }
    }
}