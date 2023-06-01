package org.lqs1848.crack.allatori;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

public class Main {

	public static ClassPool pool = ClassPool.getDefault();
	
	static final String allatoriCodeWatermarkReplace = 
			//不加密序列化 serialVersionUID
			"if($1.indexOf(\"serialVersionUID\")!=-1){ $_ = \"serialVersionUID\"; } else " +
			//修改加密逻辑
			"if($_!=null&&!$_.isEmpty())" +
			"{"+
				"$_=RandomName.get($1,$_);" +
			"}";
	
	//这里随便替换 看你心情 爱打印什么打印什么
		static final String copyrightStatement = 
				"String s = \"\";"
				+ "s += \"################################################\\n\"; "
				+ "s += \"#                                              #\\n\"; "
				+ "s += \"#            河南效易软件科技有限责任公司            #\\n\"; "
				+ "s += \"#               拥有版权专利，盗版必究              #\\n\"; "
				+ "s += \"#                 www.carpms.com               #\\n\"; "
				+ "s += \"#                                              #\\n\"; "
				+ "s += \"################################################\\n\"; " + "$_ = s; ";
	
	
	public static void main(String[] args) throws Throwable {
		// 先引用 javassist.jar lib目录下就有
		// 还有 allatori.jar 不引入这个的话 fMethod.getReturnType() 会 ClassNotFind
		
		//把随机名称的类导入到 javassist
		pool.importPackage("org.lqs1848.crack.allatori.RandomName");
		
		execute();
	}// method main

	public static byte[] dump(CtClass cl) throws Throwable {
		byte[] b = null;
		b = cl.toBytecode();
		cl.stopPruning(true);
		return b;
	}// method dump

	public static void execute() throws Throwable {
		File f = new File("allatori_crack.jar");
		if (f.exists())
			f.delete();
		f.createNewFile();
		ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
		ZipFile zf = new ZipFile("allatori.jar");
		Enumeration<? extends ZipEntry> in = zf.entries();
		byte[] data;
		while (in.hasMoreElements()) {
			ZipEntry ze = in.nextElement();
			if (zf != null && ze != null) {
				DataInputStream dis = new DataInputStream(zf.getInputStream(ze));
				data = new byte[(int) ze.getSize()];
				dis.readFully(data);
				dis.close();

				ze = modifyEntry(new ZipEntry(ze.getName()));

				if (ze.getName().endsWith(".class")) {
					CtClass cl = pool.makeClass(new java.io.ByteArrayInputStream(data));
					CtMethod[] ms = cl.getDeclaredMethods();
					boolean needRewriteFlag = false;
					
					if(!cl.getName().startsWith("com.allatori.eclipse")) {
						for (CtMethod fMethod : cl.getMethods()) {
							if(!fMethod.isEmpty() && "String".equals(fMethod.getReturnType().getSimpleName()) 
									&& cl.getName().equals(fMethod.getDeclaringClass().getName()) 
									) {
								String code = "if($_!=null && !$_.isEmpty()){"
										+ "if($_.indexOf(\"www.allatori.com\")!=-1){"
										+ "$_ = \"https://blog.lqs1848.top\";}"
										//+ "if($_.indexOf(\"Obfuscation by\")!=-1){"
										//+ "System.out.println($_);"
										//+ "$_ = \"Obfuscation by\";}"
										+ "if($_.indexOf(\"Allatori Obfuscator\")!=-1){"
										+ "$_ = \"http://lqs1848.gitee.io\";}"
										+ "; }";
								fMethod.insertAfter(code);
								needRewriteFlag = true;
							}
						}//for
					}
					
					for (CtMethod fMethod : ms) {
						if (!fMethod.isEmpty() && fMethod.getName().equals("THIS_IS_DEMO_VERSION_NOT_FOR_COMMERCIAL_USE")) {
							if (fMethod.getLongName().endsWith("(java.lang.String)")
									&& "String".equals(fMethod.getReturnType().getSimpleName())) {
								fMethod.insertAfter(allatoriCodeWatermarkReplace);
								needRewriteFlag = true;
							} else if (fMethod.getLongName().endsWith("()")
									&& "String".equals(fMethod.getReturnType().getSimpleName())) {
								fMethod.insertAfter("if($_!=null&&!$_.isEmpty()&&$_.indexOf(\"Obfuscation by\")!=-1){\n" + copyrightStatement + "}");
								needRewriteFlag = true;
							}
						}
					} // for
					if (needRewriteFlag) {
						data = dump(cl);
					} // if
				} //
				out.putNextEntry(ze);
				out.write(data, 0, data.length);
				out.closeEntry();
			} // if
		}//while
		zf.close();

		// 把自定义的随机名称类加入到jar包中
		ZipEntry ze = modifyEntry(new ZipEntry(RandomName.class.getName().replaceAll("\\.", "/") + ".class"));
		data = pool.get(RandomName.class.getName()).toBytecode();
		out.putNextEntry(ze);
		out.write(data, 0, data.length);
		out.closeEntry();

		out.setLevel(9);
		out.close();
	}// method execute

	public static ZipEntry modifyEntry(ZipEntry ze) {
		final long time = 0;
		ze.setTime(time);
		ze = ze.setCreationTime(FileTime.fromMillis(time)).setLastAccessTime(FileTime.fromMillis(time))
				.setLastModifiedTime(FileTime.fromMillis(time));
		return ze;
	}// method modifyEntry

}// class