package com.test.plugin;

import com.android.build.gradle.AppExtension;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class TestPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        AppExtension appExtension = project.getExtensions().findByType(AppExtension.class);
        assert appExtension != null;
        appExtension.registerTransform(new TestTransform(project));
    }
}