package com.dilu.plugin.image.doc;

import com.dilu.plugin.image.doc.utils.Utils;
import com.intellij.lang.documentation.DocumentationProviderEx;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import javax.imageio.ImageIO

class ImageDocDocumentationProvider : DocumentationProviderEx() {
    override fun getLocalImageForElement(element: PsiElement, imageSpec: String): Image? {
        val localImageDoc = Utils.getLocalImageDoc(imageSpec) ?: return null
        return ImageIO.read(localImageDoc).toBufferedImage()
    }
}
