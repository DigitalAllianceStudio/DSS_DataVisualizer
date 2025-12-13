package TMS320F28379D;

import java.util.ArrayList;
import java.io.IOException;
import com.ti.ccstudio.scripting.environment.*;
import com.ti.debug.engine.scripting.*;

// 注意：请勿修改函数的返回值类型、函数名、参数列表，否则上位机无法正常调用被修改的函数，
// 仅根据需要修改函数体中的代码即可，大部分情况下只需要根据需求改动 execFlashProgram, execDssInit 这两个函数。

// @formatter:off
// 编译
// javac -encoding UTF-8 -classpath ".;C:/ti/ccs1281/ccs/ccs_base/DebugServer/packages/ti/dss/java/dss.jar" ./DssScriptFiles/TMS320F28379D/DssScript.java
// 运行
// java -classpath ".;C:/ti/ccs1281/ccs/ccs_base/DebugServer/packages/ti/dss/java/dss.jar;./DssScriptFiles" TMS320F28379D/DssScript
// @formatter:on

public class DssScript {
    // 获取类文件所处路径
    private static String sClassFilePath = DssScript.class.getProtectionDomain().getCodeSource().getLocation().getPath()
            .substring(1) + DssScript.class.getPackage().getName() + "/";

    private ScriptingEnvironment env = null;
    private DebugServer server = null;
    private DebugSession session1 = null;
    private DebugSession session2 = null;

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
        // session1.flash.performOperation("Erase");
        // session2.flash.performOperation("Erase");

        env.traceWrite("信息: 开始烧录程序...");
        session1.memory.loadProgram(sAppSymbolFile);
        session1.memory.verifyProgram(sAppSymbolFile);
        session2.memory.loadProgram(sAppSymbolFile);
        session2.memory.verifyProgram(sAppSymbolFile);
        env.traceWrite("信息: 烧录完毕！");

        env.traceWrite("信息: 开始计算校验和...");
        session1.flash.performOperation("CalculateChecksum");
        session2.flash.performOperation("CalculateChecksum");

        env.setScriptTimeout(3000);

        // 加载符号文件
        session1.symbol.load(sAppSymbolFile);
        session1.symbol.getSymbolFileName();
        session2.symbol.load(sAppSymbolFile);
        session2.symbol.getSymbolFileName();
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

        session1 = server.openSession("*", "C28xx_CPU1");
        session2 = server.openSession("*", "C28xx_CPU2");

        // session1.options.printOptions(".*"); // 打印可设置的仿真器选项，不同仿真器选项可能有所不同，需要根据实际情况调整
        session1.options.setBoolean("AutoRunToLabelOnRestart", true);
        session1.options.setBoolean("AutoRunToLabelOnReset", false);
        session1.options.setBoolean("AutoResetOnConnect", true);
        session2.options.setBoolean("AutoRunToLabelOnRestart", true);
        session2.options.setBoolean("AutoRunToLabelOnReset", false);
        session2.options.setBoolean("AutoResetOnConnect", true);

        env.traceWrite("信息: 当前工作目录: " + env.getCurrentDirectory());
        env.traceWrite("信息: 开始连接到目标...");
        session1.target.connect();
        session2.target.connect();

        // env.traceWrite("信息: 打印可选的 Flash 设置...");
        // session1.flash.options.printOptionById("FlashProgrammerNode");
        // env.traceWrite("信息: 列出当前芯片所支持的 Flash 操作...");
        // session1.flash.listSupportedOperations();
        // NOTE: 不同芯片系列所定义的 Flash 操作字段可能不同，需要根据实际情况调整，可通过上方两个函数打印出所有可选的 Flash 设置字段
        // 如果不使用上位机的烧录功能，可注释下面的 Flash 操作代码，否则请根据芯片系列调整 Flash 操作字段
        // session1.flash.options.setString("FlashOperations", "Erase, Program, Verify"); // Erase, Program, Verify; Program, Verify; Load RAM Only; Verify Only
        // session1.flash.options.setBoolean("FlashSectorA", true);
        // session1.flash.options.setBoolean("FlashSectorB", true);
        // session1.flash.options.setBoolean("FlashSectorC", true);
        // session1.flash.options.setBoolean("FlashSectorD", true);

        // 如果芯片没有使能 CSM 加密，请注释下面几行代码，如果使能了 CSM 加密，请在下方填写密码，否则无法连接芯片
        // session1.flash.options.setString("FlashKey7","1234");
        // session1.flash.options.setString("FlashKey6","5678");
        // session1.flash.options.setString("FlashKey5","AABB");
        // session1.flash.options.setString("FlashKey4","CCDD");
        // session1.flash.performOperation("Unlock");

        // 加载符号文件
        session1.symbol.load(sAppSymbolFile);
        session1.symbol.getSymbolFileName();
        session2.symbol.load(sAppSymbolFile);
        session2.symbol.getSymbolFileName();
    }

    public void execDssDeinit() throws ScriptingException {
        env.traceWrite("信息: 断开连接...");
        session1.target.disconnect();
        session1.terminate();
        session2.target.disconnect();
        session2.terminate();
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
            // 注意：当前不支持同时对多个 CPU 核执行表达式，只能对单个目标执行表达式，所以只能二选一

            exprResultArray[i] = session1.expression.evaluateToString(exprArrayList.get(i));
            // exprResultArray[i] = session2.expression.evaluateToString(exprArrayList.get(i));
        }
    }

    /**
     * 执行单个表达式（例如用于读取寄存器值、通过符号获取变量的值、赋值等操作）
     * @throws ScriptingException
     */
    public String execExpressionEvaluate(String sExpression) throws ScriptingException {
        // 注意：当前不支持同时对多个 CPU 核执行表达式，只能对单个目标执行表达式，所以只能二选一

        return session1.expression.evaluateToString(sExpression);
        // return session2.expression.evaluateToString(sExpression);
    }

    public void execDssGo() throws ScriptingException {
        // session1.target.runAsynch();
        // session2.target.runAsynch();
        server.simultaneous.runAsynch(); // Run all CPUs asynchronously
    }

    public void execDssHalt() throws ScriptingException {
        // session1.target.halt();
        // session2.target.halt();
        server.simultaneous.halt(); // Halt all CPUs
    }

    public void execDssReset() throws ScriptingException {
        session1.target.reset();
        session1.target.restart(); // 需要先 session.symbol.load() 加载 .out 符号文件，或者 session.memory.loadProgram() 时使用 .out 符号文件
        session2.target.reset();
        session2.target.restart(); // 需要先 session.symbol.load() 加载 .out 符号文件，或者 session.memory.loadProgram() 时使用 .out 符号文件
    }

    /**
     * main 函数可用于独立测试，不依赖 Qt C++ 上位机，直接在终端中执行该 .class 即可测试
     */
    public static void main(String[] args) {
        DssScript dss = new DssScript();

        try {
            dss.execDssInit("D:/TestProj/TMS320F28379D.ccxml", "D:/TestProj/APP.out", "D:/DSS_DataVisualizer/LogFileStylesheet.xsl");
            dss.execFlashProgram("D:/TestProj/APP.out");
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
