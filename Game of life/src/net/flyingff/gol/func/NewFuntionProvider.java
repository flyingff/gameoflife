package net.flyingff.gol.func;

import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class NewFuntionProvider {
	private static String code = "import net.flyingff.gol.func.CalcFunction;\n\npublic class GenerateFunction implements CalcFunction{\n\tpublic int nextState(int curr, int[] live) {\n\t\treturn 0;\n\t}\n}";
	private static JTextArea ta = new JTextArea(); static {
		ta.enableInputMethods(false);
		ta.setTabSize(4);
	}
	private static JScrollPane jsp = new JScrollPane(ta);
	public static CalcFunction prompt(String str) {
		return _prompt(str);
	}
	public static CalcFunction prompt(){
		ta.setText(code);
		jsp.setPreferredSize(new Dimension(400, 320));
		if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(null,jsp, "Please Input Function Code", JOptionPane.OK_CANCEL_OPTION)) {
			return _prompt(ta.getText());
		}
		return null;
	}
	public static CalcFunction _prompt(String str) {
		CalcFunction func = null;
		DynamicEngine de = DynamicEngine.getInstance();
		try {
			func = (CalcFunction)de.javaCodeToObject("GenerateFunction", str);
			code = str;
		} catch (Exception e) {e.printStackTrace();	}
		return func;
	}

	public static void main(String[] args) {
		CalcFunction cf;
		for(;;)
		if (null != (cf = prompt())){
			System.out.println(cf.nextState(3, new int[7]));
		} else {
			System.exit(0);
		}
	}
}

// //////////////////////
// ///////////////////////
// ///////////////////////

class DynamicEngine {
	private static DynamicEngine ourInstance = new DynamicEngine();

	public static DynamicEngine getInstance() {
		return ourInstance;
	}

	private URLClassLoader parentClassLoader;
	private String classpath;

	private DynamicEngine() {
		this.parentClassLoader = (URLClassLoader) this.getClass()
				.getClassLoader();
		this.buildClassPath();
	}

	private void buildClassPath() {
		this.classpath = null;
		StringBuilder sb = new StringBuilder();
		//sb.append("\"");
		try {
			for (URL url : this.parentClassLoader.getURLs()) {
				String p = URLDecoder.decode(url.getFile(), "UTF-8");
				sb.append(p).append(File.pathSeparator);
			}
		//sb.append("\"");
		this.classpath = sb.toString();
		} catch(Exception e) {e.printStackTrace();}
	}

	public Object javaCodeToObject(String fullClassName, String javaCode)
			throws IllegalAccessException, InstantiationException {
		//long start = System.currentTimeMillis();
		Object instance = null;
		JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();
		ClassFileManager fileManager = new ClassFileManager(
				compiler.getStandardFileManager(diagnostics, null, null));

		List<JavaFileObject> jfiles = new ArrayList<JavaFileObject>();
		jfiles.add(new CharSequenceJavaFileObject(fullClassName, javaCode));

		List<String> options = new ArrayList<String>();
		options.add("-encoding");
		options.add("UTF-8");
		options.add("-classpath");
		options.add(this.classpath);
		//System.out.println("Classpath = " + this.classpath);

		JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
				diagnostics, options, null, jfiles);
		boolean success = task.call();

		if (success) {
			JavaClassObject jco = fileManager.getJavaClassObject();
			DynamicClassLoader dynamicClassLoader = new DynamicClassLoader(
					this.parentClassLoader);
			Class<?> clazz = dynamicClassLoader.loadClass(fullClassName, jco);
			instance = clazz.newInstance();
			try {
				dynamicClassLoader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			String error = "";
			for (Diagnostic<?> diagnostic : diagnostics.getDiagnostics()) {
				error = error + compilePrint(diagnostic);
			}
		}
		//long end = System.currentTimeMillis();
		//System.out.println("javaCodeToObject use:" + (end - start) + "ms");
		return instance;
	}

	private String compilePrint(Diagnostic<?> diagnostic) {
		System.out.println("Code:" + diagnostic.getCode());
		System.out.println("Kind:" + diagnostic.getKind());
		System.out.println("Position:" + diagnostic.getPosition());
		System.out.println("Start Position:" + diagnostic.getStartPosition());
		System.out.println("End Position:" + diagnostic.getEndPosition());
		System.out.println("Source:" + diagnostic.getSource());
		System.out.println("Message:" + diagnostic.getMessage(null));
		System.out.println("LineNumber:" + diagnostic.getLineNumber());
		System.out.println("ColumnNumber:" + diagnostic.getColumnNumber());
		StringBuffer res = new StringBuffer();
		res.append("Code:[" + diagnostic.getCode() + "]\n");
		res.append("Kind:[" + diagnostic.getKind() + "]\n");
		res.append("Position:[" + diagnostic.getPosition() + "]\n");
		res.append("Start Position:[" + diagnostic.getStartPosition() + "]\n");
		res.append("End Position:[" + diagnostic.getEndPosition() + "]\n");
		res.append("Source:[" + diagnostic.getSource() + "]\n");
		res.append("Message:[" + diagnostic.getMessage(null) + "]\n");
		res.append("LineNumber:[" + diagnostic.getLineNumber() + "]\n");
		res.append("ColumnNumber:[" + diagnostic.getColumnNumber() + "]\n");
		return res.toString();
	}
}

class CharSequenceJavaFileObject extends SimpleJavaFileObject {

	private CharSequence content;

	public CharSequenceJavaFileObject(String className, CharSequence content) {
		super(URI.create("string:///" + className.replace('.', '/')
				+ JavaFileObject.Kind.SOURCE.extension),
				JavaFileObject.Kind.SOURCE);
		this.content = content;
	}

	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) {
		return content;
	}
}

class ClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {
	public JavaClassObject getJavaClassObject() {
		return jclassObject;
	}

	private JavaClassObject jclassObject;

	public ClassFileManager(StandardJavaFileManager standardManager) {
		super(standardManager);
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location,
			String className, JavaFileObject.Kind kind, FileObject sibling)
			throws IOException {
		jclassObject = new JavaClassObject(className, kind);
		return jclassObject;
	}
}

class JavaClassObject extends SimpleJavaFileObject {

	protected final ByteArrayOutputStream bos = new ByteArrayOutputStream();

	public JavaClassObject(String name, JavaFileObject.Kind kind) {
		super(
				URI.create("string:///" + name.replace('.', '/')
						+ kind.extension), kind);
	}

	public byte[] getBytes() {
		return bos.toByteArray();
	}

	@Override
	public OutputStream openOutputStream() throws IOException {
		return bos;
	}
}

class DynamicClassLoader extends URLClassLoader {
	public DynamicClassLoader(ClassLoader parent) {
		super(new URL[0], parent);
	}

	public Class<?> findClassByClassName(String className)
			throws ClassNotFoundException {
		return this.findClass(className);
	}

	public Class<?> loadClass(String fullName, JavaClassObject jco) {
		byte[] classData = jco.getBytes();
		return this.defineClass(fullName, classData, 0, classData.length);
	}
}