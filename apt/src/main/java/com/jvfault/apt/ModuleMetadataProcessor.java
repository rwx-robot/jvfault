package com.jvfault.apt;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.Writer;
import java.util.Set;

/**
 * 模块元数据注解处理器：编译期扫描 @Module 并生成
 * META-INF/jvfault/modules.txt 资源（每行一个模块类名），
 * 供运行时零反射快速发现模块。
 *
 * @since v0.9.0 (2023)
 * @author jvfault team
 */
@SupportedAnnotationTypes("com.jvfault.core.annotation.Module")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ModuleMetadataProcessor extends AbstractProcessor {

    static final String RESOURCE = "META-INF/jvfault/modules.txt";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        for (Object element : roundEnv.getElementsAnnotatedWith(
                processingEnv.getElementUtils().getTypeElement("com.jvfault.core.annotation.Module"))) {
            String binaryName = ((TypeElement) element).getQualifiedName().toString();
            try {
                FileObject resource = processingEnv.getFiler()
                        .createResource(StandardLocation.CLASS_OUTPUT, "", RESOURCE, (TypeElement) element);
                try (Writer writer = resource.openWriter()) {
                    writer.write(binaryName);
                    writer.write("\n");
                }
            } catch (Exception e) {
                processingEnv.getMessager().printMessage(
                        javax.tools.Diagnostic.Kind.WARNING, "模块元数据写入失败: " + e.getMessage());
            }
        }
        return true;
    }
}
