package com.scu.suhong.dynamic_definition;

import org.junit.Test;
import util.FileHelper;

public class AssetCompilerTest {
    String testJavaClassName = "TestClass";
    String testJavaFileName = testJavaClassName + ".java";

    @Test
    public void compile() {
        AssetCompiler assetCompiler = new AssetCompiler();
        writeJavaFile();
        assetCompiler.compile(testJavaFileName);
    }

    @Test
    public void runBooleanMethod() {
        AssetCompiler assetCompiler = new AssetCompiler();
        writeJavaFile();
        assetCompiler.compile(testJavaFileName);
        try {
            assert assetCompiler.runBooleanMethod(testJavaClassName, "returnOK");
            assert !assetCompiler.runBooleanMethod(testJavaClassName, "returnFalse");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void writeJavaFile(){
        String content = "public class " + testJavaClassName + " {\n";
        content += "  public boolean returnOK(){\n";
        content += "    return true;\n";
        content += "  }\n";

        content += "  public boolean returnFalse(){\n";
        content += "    return false;\n";
        content += "  }\n";

        content += "}\n";
        FileHelper.createFile(testJavaFileName, content);
    }

    @Test
    public void checkClassName() {
        assert AssetCompiler.checkClassName("A", "class A");
        assert AssetCompiler.checkClassName("A", "class A {");
        assert AssetCompiler.checkClassName("A", "class A{");
        assert AssetCompiler.checkClassName("A", "class A{int");
        assert AssetCompiler.checkClassName("A", "class A { int");
        assert !AssetCompiler.checkClassName("A", "class AC");
        assert !AssetCompiler.checkClassName("A", "class CA");

        assert AssetCompiler.checkClassName("A", "public class A");
        assert AssetCompiler.checkClassName("A", "public class A {");
        assert AssetCompiler.checkClassName("A", "public class A{");
        assert AssetCompiler.checkClassName("A", "public class A { int");
        assert !AssetCompiler.checkClassName("A", "public class AC");
        assert !AssetCompiler.checkClassName("A", "public class CA");

        assert AssetCompiler.checkClassName("A", " public class A");
        assert AssetCompiler.checkClassName("A", " public class A {");
        assert AssetCompiler.checkClassName("A", " public class A{");
        assert AssetCompiler.checkClassName("A", " public class A { int");
        assert !AssetCompiler.checkClassName("A", " public class AC");
        assert !AssetCompiler.checkClassName("A", "  public class CA");

        assert AssetCompiler.checkClassName("A", " public class A extends B");
        assert AssetCompiler.checkClassName("A", " public class A  extends B {");
        assert AssetCompiler.checkClassName("A", " public class A  extends B{");
        assert AssetCompiler.checkClassName("A", " public class A  extends B { int");
        assert !AssetCompiler.checkClassName("A", " public class AC  extends B");
        assert !AssetCompiler.checkClassName("A", "  public class CA  extends B");

        String className = "AssetT1";
        String code = "";
        code += "  public class " + className + " extends DynamicalAsset{\n";
        code += "    @Override\n";
        code += "    public boolean check() {\n";
        code += "        return false;\n";
        code += "    }\n";
        code += "  }\n";

        assert AssetCompiler.checkClassName(className, code);
    }

    @Test
    public void checkCompile() {
        String  code = "";
        String className = "AssetT11";
        code += "public class "+ className + " extends DynamicalAsset{\n";
        code += "    @Override\n";
        code += "    public boolean check(){\n";
        code += "        return true;\n";
        code += "    }\n";
        code += "}\n";

        FileHelper.deleteFile(className + ".java");

        DynamicalAsset dynamicalAsset = null;
        AssetCompiler assetCompiler = new AssetCompiler();
        assetCompiler.setGivenGas(1000);
        try {
            dynamicalAsset = (DynamicalAsset) assetCompiler.compileAndGetInstance(className, code);
        } catch (ClassCastException e){
            e.printStackTrace();
        }

        assert null != dynamicalAsset;
        assert dynamicalAsset.check();
    }

    @Test
    public void getPackageNameIfAny() {
        String packageName = "com.scu.suhong";
        String className = "TestClass";
        String fullClassName = packageName + "." +className;

        assert packageName.equals(AssetCompiler.getPackageName(fullClassName, DynamicalAsset.class.getPackage().getName()));
        assert DynamicalAsset.class.getPackage().getName().equals(AssetCompiler.getPackageName(className, DynamicalAsset.class.getPackage().getName()));

        assert className.equals(AssetCompiler.getClassName(fullClassName));
        assert className.equals(AssetCompiler.getClassName(fullClassName));
    }
}