package com.akshaymathur.design.tcpserver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;

/**
 * run javac -J-Xdebug
 * -J-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005 -processor
 * com.akshaymathur.design.ServerAnnotationProcessor --processor-path ./bin src
 * /com/akshaymathur/design/*.java -d ./bin
 * for debugging this code.
 */
@SupportedAnnotationTypes("com.akshaymathur.design.tcpserver.Service")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ServerAnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
            RoundEnvironment roundEnv) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "ANNOTATION PROCESSOR RUNNING ");
        if (annotations.size() > 0) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found Classes annotated with Service.");
            Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(Servable.class);
            annotatedElements.stream()
                    .forEach(m -> processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, m.toString()));
            Element annotatedClassElement = roundEnv.getElementsAnnotatedWith(Service.class).iterator().next();
            String hostClassName = annotatedClassElement.getSimpleName().toString();
            String generatedClassName = hostClassName + "_Servable";
            try {
                JavaFileObject builderFile = processingEnv.getFiler().createSourceFile(generatedClassName);
                try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {
                    out.println(createClass(annotatedClassElement));
                    List<ExecutableElement> methods = ElementFilter
                            .methodsIn(annotatedClassElement.getEnclosedElements()).stream()
                            .filter(e -> e.getAnnotation(Servable.class) != null)
                            .collect(Collectors.toList());
                    for (ExecutableElement e : methods) {
                        out.println(constructMethod(e));
                    }
                    out.println(constructServerMethod());
                    out.println(constructMainMethod(generatedClassName));
                    // end of class
                    out.println("}");
                }
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "ERROR While creating file");
            }
        } else {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "No Classes found.");
        }
        return false;
    }

    private String createClass(Element annotatedClassElement) {
        String packageName = processingEnv.getElementUtils().getPackageOf(annotatedClassElement)
                .getQualifiedName().toString();
        String hostClassName = annotatedClassElement.getSimpleName().toString();
        String generatedClassName = hostClassName + "_Servable";
        // package
        return "package " + packageName + ";"
        // imports
                + "import java.lang.reflect.*;"
                + "import java.util.*; "
                + "import com.akshaymathur.design.tcpserver.*; "
                // class begins
                + "public final class " + generatedClassName + " extends " + hostClassName + " implements Server"
                + "{";
    }

    private String constructMethod(ExecutableElement e) {
        return "public " +
                e.getReturnType() +
                " " +
                e.getSimpleName() +
                " (" +
                constructParameters(e.getParameters()) +
                "){return super." + e.getSimpleName() + "(" + e.getParameters().toString() + ");}";
    }

    private String constructParameters(List<? extends VariableElement> elements) {
        StringBuilder sb = new StringBuilder();
        for (VariableElement e : elements) {
            sb.append(e.asType().toString() + " " + e.toString() + " ,");
        }
        return sb.toString().substring(0, sb.length() == 0 ? 0 : sb.length() - 1);
    }

    private String constructServerMethod() {
        return """
                public String callMethodWithName(String name){
                    Method[] allMethods = this.getClass().getSuperclass().getDeclaredMethods();
                    Optional<Method> foundMethod = Arrays.stream(allMethods, 0, allMethods.length)
                        .filter(m -> m.getName().toLowerCase().equals(name.toLowerCase()))
                        .limit(1).findFirst();
                    if (foundMethod.isEmpty()) {
                        System.out.println("Unable to find the method");
                    } else {
                        System.out.println("calling method " + name);
                    }
                    try{
                        return (String) foundMethod.get().invoke(this);
                    }catch (IllegalAccessException| InvocationTargetException e) {
                        System.out.println("ERROR OCCURED");
                        System.out.println(e);
                    }
                    return "null";
                }
                """;
    }

    private String constructMainMethod(String className) {
        return " public static void main(String... s) throws Exception { " +
                className + " service = new " + className + "();" +
                "TCPServer server = new TCPServer(service);" +
                "server.start();};";
    }

}