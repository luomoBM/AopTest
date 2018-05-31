package com.app.plugin

import com.android.build.api.transform.*
import com.android.utils.FileUtils
import com.google.common.collect.Sets
import javassist.ClassPool
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Created by crizquan on 2018/5/31.
 * <p>
 * desc:
 * */
class JavassistPlugin extends Transform implements Plugin<Project> {
    public static final String TAG = "JavassistPlugin"

    private Project project

    @Override
    void apply(Project project) {
        this.project = project
        def log = project.logger
        log.error "========================"
        log.error "Javassist开始修改Class!"
        log.error "========================"
        project.android.registerTransform(this)
    }


    @Override
    String getName() {
        return "JavassistPlugin";
    }

    @Override
    Set<QualifiedContent.ContentType> getInputTypes() {
        return Sets.immutableEnumSet(QualifiedContent.DefaultContentType.CLASSES)
    }

    @Override
    Set<QualifiedContent.Scope> getScopes() {
        return Sets.immutableEnumSet(QualifiedContent.Scope.PROJECT/*, QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
                QualifiedContent.Scope.SUB_PROJECTS, QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
                QualifiedContent.Scope.EXTERNAL_LIBRARIES*/)
    }

    @Override
    boolean isIncremental() {
        return false;
    }

    @Override
    void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        def inputs = transformInvocation.getInputs()


        def outputProvider = transformInvocation.getOutputProvider()


        def startTime = System.currentTimeMillis();
        inputs.each { TransformInput input ->
            input.directoryInputs.each { DirectoryInput directoryInput ->
                //文件夹里面包含的是我们手写的类以及R.class、BuildConfig.class以及R$XXX.class等
                EventInject.injectCode(directoryInput.file.getAbsolutePath(), project)
                // 获取output目录
                def dest = outputProvider.getContentLocation(directoryInput.name,
                       directoryInput.contentTypes, directoryInput.scopes,
                        Format.DIRECTORY)
//                 将input的目录复制到output指定目录
                FileUtils.copyDirectory(directoryInput.file, dest)
            }
        }
        ClassPool.getDefault().clearImportedPackages();
        project.logger.error("JavassistPlugin cast :" + (System.currentTimeMillis() - startTime) / 1000 + " secs");

    }
}