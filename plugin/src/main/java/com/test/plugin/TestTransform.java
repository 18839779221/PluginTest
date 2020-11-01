package com.test.plugin;

import com.android.build.api.transform.Format;
import com.android.build.api.transform.QualifiedContent;
import com.android.build.api.transform.Transform;
import com.android.build.api.transform.TransformInput;
import com.android.build.api.transform.TransformInvocation;
import com.android.build.gradle.internal.pipeline.TransformManager;


import org.apache.commons.io.FileUtils;
import org.gradle.api.Project;


import java.io.File;
import java.io.IOException;
import java.util.Set;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;


/**
 * @author: wanglun
 * @date: 2020/10/29
 * @desc:
 */
public class TestTransform extends Transform {


    private final Project project;

    public TestTransform(Project project) {
        this.project = project;
    }

    @Override
    public String getName() {
        return "TestTransform";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return TransformManager.SCOPE_FULL_PROJECT;
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override public void transform(TransformInvocation invocation) {
        System.out.println("TestTransform transform");
        for (TransformInput input : invocation.getInputs()) {
            //遍历jar文件 对jar不操作，但是要输出到out路径
            input.getJarInputs().parallelStream().forEach(jarInput -> {
                File src = jarInput.getFile();
                System.out.println("input.getJarInputs fileName:" + src.getName());
                File dst = invocation.getOutputProvider().getContentLocation(
                        jarInput.getName(), jarInput.getContentTypes(), jarInput.getScopes(),
                        Format.JAR);
                try {
                    FileUtils.copyFile(src, dst);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            //遍历文件
            input.getDirectoryInputs().parallelStream().forEach(directoryInput -> {
                File src = directoryInput.getFile();
                System.out.println("input.getDirectoryInputs fileName:" + src.getName());
                File dst = invocation.getOutputProvider().getContentLocation(
                        directoryInput.getName(), directoryInput.getContentTypes(),
                        directoryInput.getScopes(), Format.DIRECTORY);
                try {
                    scanFilesAndInsertCode(src.getAbsolutePath());
                    FileUtils.copyDirectory(src, dst);
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }
            });
        }
    }

    private void scanFilesAndInsertCode(String path) throws Exception {
        ClassPool classPool = ClassPool.getDefault();
        classPool.appendClassPath(path);
        classPool.importPackage("android.widget");
        CtClass ctClass = classPool.getCtClass("com.test.TestPluginClass");
        if (ctClass == null) {
            return;
        }
        if (ctClass.isFrozen()) {
            ctClass.defrost();
        }
        CtMethod ctMethod = ctClass.getDeclaredMethod("init");

        ctMethod.insertAfter("{System.out.println(\"test plugin!\");}");
        ctClass.writeFile(path);
        ctClass.detach();
        System.out.println(String.format("In Class[%s], insert after Method[%s]",ctClass.getName(),ctMethod.getName()));
        CtClass ctMainClass = classPool.getCtClass("com.test.MainActivity");
        if (ctMainClass == null) {
            return;
        }
        if (ctMainClass.isFrozen()) {
            ctMainClass.defrost();
        }
        CtMethod onCreateMethod = ctMainClass.getDeclaredMethod("init");
        String insertStr = "android.widget.Toast.makeText($0,\"hello\",android.widget.Toast.LENGTH_SHORT).show();";
        onCreateMethod.insertAfter(insertStr);
        ctMainClass.writeFile(path);
        ctMainClass.detach();
        System.out.println(String.format("In Class[%s], insert after Method[%s]",ctMainClass.getName(),onCreateMethod.getName()));
    }



}
