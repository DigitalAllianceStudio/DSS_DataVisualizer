package TMS320F2802x;

import java.util.ArrayList;
import java.io.IOException;
import com.ti.ccstudio.scripting.environment.*;
import com.ti.debug.engine.scripting.*;

// 注意：请勿修改函数的返回值类型、函数名、参数列表，否则上位机无法正常调用被修改的函数，
// 仅根据需要修改函数体中的代码即可，大部分情况下只需要根据需求改动 execFlashProgram, execDssInit 这两个函数。

public class DssScript {
    // 获取类文件所处路径
    private static String sClassFilePath = DssScript.class.getProtectionDomain().getCodeSource().getLocation().getPath()
            .substring(1) + DssScript.class.getPackage().getName() + "/";

    private ScriptingEnvironment env = null;
    private DebugServer server = null;
    private DebugSession session = null;

    // 存储表达式
    public ArrayList<String> exprArrayList;

    // 存储表达式的计算结果
    public String[] exprResultArray;

    /**
     * 执行烧录程序
     *
     * @throws ScriptingException
     * @throws IOException
     */
    public void execFlashProgram(String sAppSymbolFile) throws ScriptingException, IOException {
        env.setScriptTimeout(30000); // 根据实际情况调整超时时间
        // session.flash.performOperation("Erase");

        env.traceWrite("信息: 开始烧录程序...");
        session.memory.loadProgram(sAppSymbolFile);
        session.memory.verifyProgram(sAppSymbolFile);
        env.traceWrite("信息: 烧录完毕！");

        env.traceWrite("信息: 开始计算校验和...");
        session.flash.performOperation("CalculateChecksum");

        env.setScriptTimeout(3000);

        // 加载符号文件
        session.symbol.load(sAppSymbolFile);
        session.symbol.getSymbolFileName();
    }

    public void execDssInit(String sConfigurationFile, String sAppSymbolFile, String sLogStylesheetFile) throws ScriptingException {
        env = ScriptingEnvironment.instance();
        env.setScriptTimeout(10000); // 不同芯片 DSS 初始化时间不同，需要根据实际情况调整超时时间

        System.out.println("脚本文件所处路径: " + sClassFilePath);
        // 创建 Log 日志文件
        env.traceBegin(sClassFilePath + "Logs.xml", sLogStylesheetFile);
        env.traceSetConsoleLevel(TraceLevel.INFO);
        env.traceSetFileLevel(TraceLevel.INFO);

        server = (DebugServer) env.getServer("DebugServer.1");
        server.setConfig(sConfigurationFile);
        // 打开会话（3种方式，根据情况选择其中一种即可）
        session = server.openSession("*", "*");
        // session = server.openSession("*", "C28xx_CPU1");
        // session = server.openSession("Texas Instruments XDS100v3 USB Debug Probe_0", "C28xx_CPU1");

        // session.options.printOptions(".*"); // 打印可设置的仿真器选项，不同仿真器选项可能有所不同，需要根据实际情况调整
        session.options.setBoolean("AutoRunToLabelOnRestart", true);
        session.options.setBoolean("AutoRunToLabelOnReset", true);
        session.options.setBoolean("AutoResetOnConnect", true);

        env.traceWrite("信息: 当前工作目录: " + env.getCurrentDirectory());
        env.traceWrite("信息: 开始连接到目标...");
        session.target.connect();

        // env.traceWrite("信息: 打印可选的 Flash 设置...");
        // session.flash.options.printOptionById("FlashProgrammerNode");
        // env.traceWrite("信息: 列出当前芯片所支持的 Flash 操作...");
        // session.flash.listSupportedOperations();
        // NOTE: 不同芯片系列所定义的 Flash 操作字段可能不同，需要根据实际情况调整，可通过上方两个函数打印出所有可选的 Flash 设置字段
        // 如果不使用上位机的烧录功能，可注释下面的 Flash 操作代码，否则请根据芯片系列调整 Flash 操作字段
        session.flash.options.setString("FlashOperations", "Erase, Program, Verify"); // Erase, Program, Verify; Program, Verify; Load RAM Only; Verify Only
        session.flash.options.setBoolean("FlashSectorA", true);
        session.flash.options.setBoolean("FlashSectorB", true);
        session.flash.options.setBoolean("FlashSectorC", true);
        session.flash.options.setBoolean("FlashSectorD", true);

        // 如果芯片没有使能 CSM 加密，请注释下面几行代码，如果使能了 CSM 加密，请在下方填写密码，否则无法连接芯片
        // session.flash.options.setString("FlashKey0","FFFF");
        // session.flash.options.setString("FlashKey1","FFFF");
        // session.flash.options.setString("FlashKey2","FFFF");
        // session.flash.options.setString("FlashKey3","FFFF");
        // session.flash.options.setString("FlashKey4","1234");
        // session.flash.options.setString("FlashKey5","5678");
        // session.flash.options.setString("FlashKey6","1331");
        // session.flash.options.setString("FlashKey7","7447");
        // session.flash.performOperation("Unlock");

        // 加载符号文件
        session.symbol.load(sAppSymbolFile);
        session.symbol.getSymbolFileName();
    }

    public void execDssDeinit() throws ScriptingException {
        env.traceWrite("信息: 断开连接...");
        session.target.disconnect();
        session.terminate();
        server.stop();

        env.traceEnd();
    }

    /**
     * 初始化 exprResultArray 数组并设置其元素个数、清空 exprArrayList
     *
     * @param num exprResultArray 的元素个数
     */
    public void execExpressionInit(int num) {
        exprResultArray = new String[num];
        if (exprArrayList != null) {
            exprArrayList.clear();
        } else {
            exprArrayList = new ArrayList<>();
        }

        System.out.println("信息: num = " + num);
        System.out.println("信息: exprResultArray.length = " + exprResultArray.length);
    }

    /**
     * 执行批量表达式前需要先调用此函数将多个表达式添加到 ArrayList 中
     */
    public void execExpressionArrayListAdd(String sExpression) {
        exprArrayList.add(sExpression);
    }

    /**
     * 执行批量表达式（用于通过符号获取变量的值）
     * @throws ScriptingException
     */
    public void execExpressionEvaluate() throws ScriptingException {
        for (int i = 0; i < exprArrayList.size(); i++) {
            exprResultArray[i] = session.expression.evaluateToString(exprArrayList.get(i));
            // System.out.println(i + " - " + exprArrayList.get(i) + " = " + exprResultArray[i]);
        }
    }

    /**
     * 执行单个表达式（例如用于读取寄存器值、通过符号获取变量的值、赋值等操作）
     * @throws ScriptingException
     */
    public String execExpressionEvaluate(String sExpression) throws ScriptingException {
        return session.expression.evaluateToString(sExpression);
    }

    public void execDssGo() throws ScriptingException {
        session.target.runAsynch();
    }

    public void execDssHalt() throws ScriptingException {
        session.target.halt();
    }

    public void execDssReset() throws ScriptingException {
        session.target.reset();
        session.target.restart(); // 需要先 session.symbol.load() 加载 .out 符号文件，或者 session.memory.loadProgram() 时使用 .out 符号文件
    }

    // @formatter:off
    // 编译
    // javac -encoding UTF-8 -classpath ".;C:/ti/ccs1281/ccs/ccs_base/DebugServer/packages/ti/dss/java/dss.jar" ./DssScriptFiles/TMS320F2802x/DssScript.java
    // 运行
    // java -classpath ".;C:/ti/ccs1281/ccs/ccs_base/DebugServer/packages/ti/dss/java/dss.jar;./DssScriptFiles" TMS320F2802x/DssScript
    // @formatter:on
    /**
     * main 函数仅用于独立测试，不依赖 Qt C++ 上位机，也不会被上位机调用，直接在终端中执行该 .class 即可测试
     */
    public static void main(String[] args) {
        DssScript dss = new DssScript();

        try {
            dss.execDssInit("D:/CCS12/TestProjF28027/TMS320F28027.ccxml", "D:/CCS12/TestProjF28027/APP.out", "D:/CCS12/TestProjF28027/LogFileStylesheet.xsl");
            dss.execFlashProgram("D:/CCS12/TestProjF28027/APP.out");
            dss.execDssReset();
            dss.execDssGo();
            dss.execDssDeinit();
        } catch (ScriptingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
