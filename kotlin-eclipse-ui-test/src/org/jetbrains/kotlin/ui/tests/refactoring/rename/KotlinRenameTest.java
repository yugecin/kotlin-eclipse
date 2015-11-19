package org.jetbrains.kotlin.ui.tests.refactoring.rename;

import org.junit.Ignore;
import org.junit.Test;

public class KotlinRenameTest extends KotlinRenameTestCase {
    @Test
    public void testSimple() {
        doTest("testData/refactoring/rename/simple/info.test");
    }
    
    @Ignore("Disable because of blocking UI")
    @Test
    public void testAutomaticRenamer() {
        doTest("testData/refactoring/rename/automaticRenamer/simple.test");
    }
    
    @Test
    public void testRenameJavaClass() {
        doTest("testData/refactoring/rename/renameJavaClass/renameJavaClass.test");
    }
    
    @Test
    public void testRenameJavaClassSamePackage() {
        doTest("testData/refactoring/rename/renameJavaClassSamePackage/renameJavaClassSamePackage.test");
    }
    
    @Test
    public void testRenameJavaInterface() {
        doTest("testData/refactoring/rename/renameJavaInterface/renameJavaInterface.test");
    }
    
    @Ignore("Temporary disable as a bug")
    @Test
    public void testRenameJavaKotlinOverridenMethod() {
        doTest("testData/refactoring/rename/renameJavaMethod/kotlinOverridenMethod.test");
    }
    
    @Ignore("Disable because of blocking UI")
    @Test
    public void testRenameKotlinClass() {
        doTest("testData/refactoring/rename/renameKotlinClass/kotlinClass.test");
    }
    
    @Ignore("Disable because of blocking UI")
    @Test
    public void testRenameKotlinMethod() {
        doTest("testData/refactoring/rename/renameKotlinMethod/renameKotlinMethod.test");
    }
    
    @Ignore("Disable because of blocking UI")
    @Test
    public void testRenameKotlinTopLevelFun() {
        doTest("testData/refactoring/rename/renameKotlinTopLevelFun/renameKotlinTopLevelFun.test");
    }
    
    @Test
    public void testRenameJavaStaticMethod() {
        doTest("testData/refactoring/rename/renameJavaStaticMethod/renameJavaStaticMethod.test");
    }
}