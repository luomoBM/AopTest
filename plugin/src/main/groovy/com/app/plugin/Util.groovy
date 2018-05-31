package com.app.plugin

/**
 * Created by BM on 2018/5/23.
 * <p>
 * desc:
 */
class Util {

    static String getClassFileName(String classFilePath, int index) {
        def end = classFilePath.length() - ".class".length()
        return classFilePath.substring(index, end).replace(File.separator, ".")
    }

    static String getMethodSimpleName(String methodName) {
        def start = methodName.lastIndexOf(".") + 1
        return methodName.substring(start, methodName.length())
    }
}
