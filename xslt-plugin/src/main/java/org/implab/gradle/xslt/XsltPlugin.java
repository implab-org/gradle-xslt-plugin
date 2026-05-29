package org.implab.gradle.xslt;

import javax.inject.Inject;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.implab.gradle.xslt.tasks.XsltTask;


public abstract class XsltPlugin implements Plugin<Project> {

    @Inject
    public abstract Project getProject();

    @Override
    public void apply(Project target) {
        exportClass(XsltTask.class);
    }

    void exportClass(Class<?> clazz) {
        getProject().getExtensions().getExtraProperties().set(clazz.getSimpleName(), clazz);
    }
}
