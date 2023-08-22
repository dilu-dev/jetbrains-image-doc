package com.dilu.plugin.image.doc.utils

import sun.misc.Unsafe
import java.lang.reflect.Field
import java.lang.reflect.Modifier

object ReflectUtils {
    /**
     * 获取字段
     *
     * @param clazz 类
     * @param fieldName 字段名
     * @return 字段
     * @throws NoSuchFieldException 没有该字段
     */
    @Throws(NoSuchFieldException::class)
    fun getField(clazz: Class<*>, fieldName: String): Field {
        // Try getting a public field
        try {
            return clazz.getField(fieldName)
        } catch (e: NoSuchFieldException) {
            var currentClass: Class<*>? = clazz
            do {
                try {
                    return currentClass!!.getDeclaredField(fieldName)
                } catch (ignore: NoSuchFieldException) {
                }
                currentClass = currentClass!!.superclass
            } while (currentClass != null)
            throw e
        }
    }

    /**
     * 设置字段值
     *
     * @param field 字段
     * @param value 值
     * @throws IllegalAccessException
     */
    @Throws(IllegalAccessException::class)
    fun setStaticFieldValue(field: Field, value: Any) {
        field.isAccessible = true
        removeFinalModifier(field)
        field[null] = value
    }

    /**
     * 移除final修饰符
     *
     * @param field 字段
     */
    private fun removeFinalModifier(field: Field): Boolean {
        if (field.modifiers and Modifier.FINAL != Modifier.FINAL) {
            return true
        }
        return try {
            val modifiersField = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            true
        } catch (ignore: Exception) {
            false
        }
    }

    /**
     *
     * @param field
     * @param value
     * @throws IllegalAccessException
     * @throws NoSuchFieldException
     */
    @Throws(IllegalAccessException::class, NoSuchFieldException::class)
    fun setStaticFieldValueByUnsafe(field: Field, value: Any) {
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as Unsafe
        val fieldBase = unsafe.staticFieldBase(field)
        val fieldOffset = unsafe.staticFieldOffset(field)
        unsafe.putObject(fieldBase, fieldOffset, value)
    }

    @Throws(IllegalAccessException::class, NoSuchFieldException::class)
    fun setNoneStaticFieldValueByUnsafe(obj: Any, field: Field, value: Any) {
        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
        unsafeField.isAccessible = true
        val unsafe = unsafeField.get(null) as Unsafe
        val fieldOffset = unsafe.objectFieldOffset(field)
        unsafe.putObject(obj, fieldOffset, value)
    }
}