package com.app.plugin

import groovy.io.FileType
import javassist.ClassPool
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.bytecode.AnnotationsAttribute
import javassist.bytecode.DuplicateMemberException
import javassist.bytecode.annotation.IntegerMemberValue
import org.gradle.api.Project

import java.lang.annotation.Annotation
/**
 * Created by BM on 2018/5/23.
 * <p>
 * desc:
 */
class EventInject {
    static
    final String ERROR_INJECT = "非Activity或Fragment中使用@EventRegister必须和@EventUnRegister一起使用，才能自动生成注册和反注册代码"
    static final String PACKAGE_NAME_APP = "com"
    static ClassPool mPool = ClassPool.getDefault()

    static void injectCode(String filePath, Project project) {
        mPool.insertClassPath(filePath)
        mPool.insertClassPath(project.android.bootClasspath[0].toString())
        mPool.importPackage(EventHelper.ANNO_RECEIVER)
        mPool.importPackage(EventHelper.ANNO_REGISTER)
        mPool.importPackage(EventHelper.ANNO_UN_REGISTER)
        mPool.importPackage("android.os.Bundle")
        mPool.importPackage("com.lm.aoptest.event.EventCenterV2")
        mPool.importPackage("com.lm.aoptest.event.IEventCaller")
        mPool.importPackage("android.os.Message")
        mPool.importPackage("android.support.*")
        def dir = new File(filePath)
        if (!dir.exists() || !dir.isDirectory()) {
            return
        }
        dir.eachFileRecurse(FileType.FILES) { file ->
            def path = file.getAbsolutePath()
            if (path.endsWith(".class") && !path.contains('R$') && !path.contains('$')
                    && !path.contains('R.class') && !path.contains("BuildConfig.class")) {
                def index = path.indexOf(PACKAGE_NAME_APP)
                def myPackageFile = index != -1
                if (myPackageFile && injectMyPackageCode(path, index, project, filePath)) {
                    //empty
                }
            }
        }
    }

    private
    static boolean injectMyPackageCode(String classFilePath, int index, Project project, String writePath) {
        def classStr = Util.getClassFileName(classFilePath, index)
//        project.logger.debug("JavassistPlugin classFilePath = " + classFilePath + " classStr = " + classStr)
        def ctClass = mPool.get(classStr)
        def isNeedInject = false
        def info = new EventInfo()
        info.setCtClz(ctClass)
        for (CtMethod method : ctClass.getDeclaredMethods()) {
            def methodSimpleName = Util.getMethodSimpleName(method.getName())

            try {
                for (Annotation annotation : method.getAnnotations()) {
                    project.logger.debug("annotation = " + annotation)

                    if (EventHelper.ANNO_RECEIVER == annotation.annotationType().canonicalName) {
                        info.getEventMethods().put(method, annotation)
                        isNeedInject = true
                    } else if (EventHelper.ANNO_REGISTER == annotation.annotationType().canonicalName) {
                        info.setOnRegistMethod(method)
                    } else if (EventHelper.ANNO_UN_REGISTER == annotation.annotationType().canonicalName) {
                        info.setOnUnRegistMethod(method)
                    }

                    if (EventHelper.IS_ACTIVITY_METHOD.contains(methodSimpleName) &&
                            "android.annotation.CallSuper" == annotation.annotationType().canonicalName) info.setIsActivity(true)
                }
            } catch (Exception e) {
                project.logger.debug(e.getCause() + "=-=" + e)
            }

            if (EventHelper.ON_CREATE.contains(methodSimpleName)) info.setOnCreateMethod(method)
            if (EventHelper.ON_DESTROY.contains(methodSimpleName)) info.setOnDestroyMethod(method)
        }

        if ((info.getOnRegistMethod() == null && info.getOnUnRegistMethod() != null) ||
                (info.getOnRegistMethod() != null && info.getOnUnRegistMethod() == null)) {
            assert false: ERROR_INJECT
        }

        if (isNeedInject) {
            injectCodeNoJudge(classFilePath, info, project, writePath)
        }
        ctClass.detach()
        return isNeedInject
    }

    //--------------------------------------------------------------------------------------------------------

    private
    static void injectCodeNoJudge(String classFilePath, EventInfo info, Project project, String writePath) {
        if (info.getCtClz().isFrozen()) info.getCtClz().defrost()

        if (info.getOnCreateMethod() != null) {
            insertAfterRegistEventStr(project, info, info.getOnCreateMethod(), false)
        } else if (info.getOnRegistMethod() != null) {
            insertAfterRegistEventStr(project, info, info.getOnRegistMethod(), false)
        } else {
            insertAfterRegistEventStr(project, info, null, info.getIsActivity())
        }

        if (info.getOnDestroyMethod() != null) {
            insertAfterUnRegistEventStr(project, info, info.getOnDestroyMethod(), false)
        } else if (info.getOnUnRegistMethod() != null) {
            insertAfterUnRegistEventStr(project, info, info.getOnUnRegistMethod(), false)
        } else {
            insertAfterUnRegistEventStr(project, info, null, info.getIsActivity())
        }

        insertCallEventImpl(project, info)
        info.getCtClz().writeFile(writePath)
    }

    private static void insertCallEventImpl(Project project, EventInfo info) {
        info.getCtClz().addInterface(info.getCtClz().getClassPool().get("com.lm.aoptest.event.IEventCaller"))
//        info.getCtClz().setInterfaces()
        def switchStr = "    public void call(Message msg) {\n" +
                "        switch (msg.what){\n"
        for (Map.Entry<CtMethod, Annotation> entry : info.getEventMethods().entrySet()) {
            def method = entry.getKey()
            def methodInfo = method.getMethodInfo()
            def mAnno = entry.getValue()

            AnnotationsAttribute attribute = methodInfo.getAttribute(AnnotationsAttribute.visibleTag);
            //获取注解属性
            javassist.bytecode.annotation.Annotation annotation = attribute.getAnnotation(mAnno.annotationType().canonicalName);
            //获取注解
            int tag = ((IntegerMemberValue) annotation.getMemberValue("value")).getValue()

            def parameterTypes = method.getParameterTypes()
            assert parameterTypes.length <= 1
            def isBaseType = false
            def packageName = ""
            if (parameterTypes.length == 1) {
                String parameterName = parameterTypes[0].name
                switch (parameterName) {
                    case "boolean": parameterName = "Boolean"; isBaseType = true; break;
                    case "byte": parameterName = "Byte"; isBaseType = true; break;
                    case "char": parameterName = "Character"; isBaseType = true; break;
                    case "float": parameterName = "Float"; isBaseType = true; break;
                    case "int": parameterName = "Integer"; isBaseType = true; break;
                    case "long": parameterName = "Long"; isBaseType = true; break;
                    case "short": parameterName = "Short"; isBaseType = true; break;
                    case "double": parameterName = "Double"; isBaseType = true; break;
                }
                project.logger.quiet "name:" + parameterName
                packageName = isBaseType ? "java.lang." + parameterName : parameterName;
                info.getCtClz().getClassPool().importPackage(packageName)
            }
            String paramStr = isBaseType ? ("((" + packageName + ")msg.obj)." +
                    parameterTypes[0].name + "Value()") : ("(" + packageName + ")msg.obj")
            switchStr += "case " + tag + ":" + method.getName() +
                    "(" + (parameterTypes.length == 1 ? paramStr : "") + ");\n break;\n"
        }
        String methodStr = switchStr + "}\n}"
        project.logger.quiet methodStr
//        info.getCtClz().getClassPool().insertClassPath(new ClassClassPath(SwipeRefreshLayout.class))
//        project.logger.quiet info.getCtClz().getInterfaces()
        CtMethod dispatchEventMethod = CtMethod.make(methodStr, info.getCtClz())
        try {
            info.getCtClz().addMethod(dispatchEventMethod)
        } catch (DuplicateMemberException e) {
        }
    }

    private
    static void insertAfterUnRegistEventStr(Project project, EventInfo info, CtMethod method, boolean isActivity) {
        if (method != null) {
            def injectSentence = EventHelper.getInsetUnRegistEventStr()
            method.insertAfter(injectSentence)
            project.logger.quiet method.toString() + "\n"  + injectSentence
            return
        }
        def methodStr = isActivity ? EventHelper.getInsetActivityUnRegistMethodStr() :
                EventHelper.getInsetFragmentUnRegistMethodStr()
        project.logger.quiet methodStr
        CtMethod mInitEventMethod = CtNewMethod.make(methodStr, info.getCtClz())
        info.getCtClz().addMethod(mInitEventMethod)
    }

    private
    static void insertAfterRegistEventStr(Project project, EventInfo info, CtMethod method, boolean isActivity) {
        for (Map.Entry<CtMethod, Annotation> entry : info.getEventMethods().entrySet()) {
            project.logger.debug("insertAfterRegistEventStr entry key =  " + entry.key + ", value = ", entry.value)
            def methodInfo = entry.getKey().getMethodInfo()
            def mAnno = entry.getValue()

            AnnotationsAttribute attribute = methodInfo.getAttribute(AnnotationsAttribute.visibleTag);
            //获取注解属性
            javassist.bytecode.annotation.Annotation annotation = attribute.getAnnotation(mAnno.annotationType().canonicalName);
            //获取注解
            int tag = ((IntegerMemberValue) annotation.getMemberValue("value")).getValue()
            int thread = 0
            if (annotation.getMemberValue("thread") != null)
                thread = ((IntegerMemberValue) annotation.getMemberValue("thread")).getValue();
            if (method != null) {
                def injectSentence = EventHelper.getInsetRegistEventStr(
                        tag,
                        thread)
                method.insertAfter(injectSentence)
                project.logger.quiet method.toString() + "\n" + injectSentence
                return
            }
            def methodStr = isActivity ? EventHelper.getInsetActivityRegistMethodStr(tag, thread) :
                    EventHelper.getInsetFragmentRegistMethodStr(tag, thread)
            project.logger.quiet methodStr
            CtMethod mInitEventMethod = CtNewMethod.make(methodStr, info.getCtClz())
            info.getCtClz().addMethod(mInitEventMethod)
        }
    }
}