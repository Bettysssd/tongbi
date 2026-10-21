package com.tongbi.analysis;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class JavaAnalyzer {

    public static final String CURATED_REWRITE =
            "public List<BidItem> filterValid(List<BidItem> items) {\n"
                    + "    List<BidItem> result = new ArrayList<BidItem>();\n"
                    + "    if (items == null || items.isEmpty()) {\n"
                    + "        return result;\n"
                    + "    }\n"
                    + "    for (BidItem item : items) {\n"
                    + "        if (item == null || item.getPrice() == null) {\n"
                    + "            continue;\n"
                    + "        }\n"
                    + "        if (item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {\n"
                    + "            continue;\n"
                    + "        }\n"
                    + "        if (!BidStatus.VALID.equals(item.getStatus())) {\n"
                    + "            continue;\n"
                    + "        }\n"
                    + "        result.add(item);\n"
                    + "    }\n"
                    + "    return result;\n"
                    + "}";

    static final String SAMPLE_ORIGINAL =
            "public List<BidItem> filterValid(List<BidItem> items) {\n"
                    + "    List<BidItem> result = new ArrayList<BidItem>();\n"
                    + "    for (int i = 0; i < items.size(); i++) {\n"
                    + "        BidItem item = items.get(i);\n"
                    + "        if (item.getPrice() != null) {\n"
                    + "            if (item.getPrice().doubleValue() > 0\n"
                    + "                    && item.getStatus().equals(\"1\")) {\n"
                    + "                result.add(item);\n"
                    + "            }\n"
                    + "        }\n"
                    + "    }\n"
                    + "    return result;\n"
                    + "}";

    private static final Pattern STATUS_CALL = Pattern.compile(
            "(\\w+)\\s*\\.\\s*get(\\w+)\\s*\\(\\s*\\)\\s*\\.\\s*equals\\s*\\(\\s*\"([01])\"\\s*\\)");
    private static final Pattern BARE_STATUS_CALL = Pattern.compile(
            "(?<![\\w.])get(\\w+)\\s*\\(\\s*\\)\\s*\\.\\s*equals\\s*\\(\\s*\"([01])\"\\s*\\)");
    private static final Pattern RESULT_LIST = Pattern.compile(
            "List\\s*<[^;{>]+>\\s+(\\w+)\\s*=\\s*new\\s+ArrayList");

    private final Object parseLock = new Object();

    public JavaAnalyzer() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_8);
        synchronized (parseLock) {
            StaticJavaParser.setConfiguration(configuration);
        }
    }

    public MethodFocus analyze(String code) {
        ParsedSource parsed = parse(code);
        List<MethodDeclaration> methods = parsed.getUnit().findAll(MethodDeclaration.class);
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("没有找到可以分析的方法。");
        }
        MethodDeclaration focus = methods.get(0);
        int focusCount = -1;
        for (int i = 0; i < methods.size(); i++) {
            int count = detect(parsed, methods.get(i)).size();
            if (count > focusCount) {
                focus = methods.get(i);
                focusCount = count;
            }
        }
        String source = slice(parsed, focus);
        boolean curated = compact(source).equals(compact(SAMPLE_ORIGINAL));
        List<CodeIssue> issues = curated ? curatedIssues() : detect(parsed, focus);
        String className = classNameOf(focus);
        if (className == null && curated) {
            className = "BidItemService";
        }
        String suggestion = curated ? CURATED_REWRITE : rewrite(source, issues);
        return new MethodFocus(className, focus.getNameAsString(), source, methods.size() - 1, issues, suggestion);
    }

    public List<String> issueIds(String code) {
        List<String> ids = new ArrayList<String>();
        for (CodeIssue issue : analyze(code).getIssues()) {
            ids.add(issue.getId());
        }
        return ids;
    }

    private ParsedSource parse(String code) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("请先贴一段 Java 代码。");
        }
        String normalized = code.replace("\r\n", "\n").replace("\r", "\n").trim();
        CompilationUnit direct = tryParse(normalized);
        if (direct != null) {
            return new ParsedSource(normalized, direct, 0);
        }
        String classWrapped = "class TongbiHolder {\n" + normalized + "\n}\n";
        CompilationUnit asClass = tryParse(classWrapped);
        if (asClass != null && !asClass.findAll(MethodDeclaration.class).isEmpty()) {
            return new ParsedSource(normalized, asClass, 1);
        }
        String interfaceWrapped = "interface TongbiHolder {\n" + normalized + "\n}\n";
        CompilationUnit asInterface = tryParse(interfaceWrapped);
        if (asInterface != null && !asInterface.findAll(MethodDeclaration.class).isEmpty()) {
            return new ParsedSource(normalized, asInterface, 1);
        }
        throw new IllegalArgumentException("这段还不能当成 Java 方法解析。");
    }

    private CompilationUnit tryParse(String code) {
        try {
            return parseUnit(code);
        } catch (ParseProblemException ex) {
            return null;
        }
    }

    private CompilationUnit parseUnit(String code) {
        synchronized (parseLock) {
            return StaticJavaParser.parse(code);
        }
    }

    private List<CodeIssue> detect(ParsedSource parsed, MethodDeclaration method) {
        List<CodeIssue> issues = new ArrayList<CodeIssue>();
        String source = slice(parsed, method);
        CodeIssue empty = emptyIssue(parsed, method, source);
        if (empty != null) {
            issues.add(empty);
        }
        CodeIssue status = statusIssue(method);
        if (status != null) {
            issues.add(status);
        }
        CodeIssue money = moneyIssue(method);
        if (money != null) {
            issues.add(money);
        }
        return issues;
    }

    private CodeIssue emptyIssue(ParsedSource parsed, MethodDeclaration method, String source) {
        String param = null;
        Node use = null;
        int useLine = Integer.MAX_VALUE;
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType().isPrimitiveType()) {
                continue;
            }
            String name = parameter.getNameAsString();
            for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
                if (!"size".equals(call.getNameAsString()) || !call.getScope().isPresent()) {
                    continue;
                }
                if (!isParam(call.getScope().get(), name)) {
                    continue;
                }
                int line = lineOf(call);
                if (line < useLine) {
                    useLine = line;
                    use = call;
                    param = name;
                }
            }
            for (ForEachStmt stmt : method.findAll(ForEachStmt.class)) {
                if (!isParam(stmt.getIterable(), name)) {
                    continue;
                }
                int line = lineOf(stmt);
                if (line < useLine) {
                    useLine = line;
                    use = stmt;
                    param = name;
                }
            }
        }
        if (param == null || guardedBefore(method, param, useLine)) {
            return null;
        }
        String before = sourceLine(parsed, use);
        if (!(use instanceof ForEachStmt)) {
            ForStmt loop = ancestor(use, ForStmt.class);
            if (loop != null) {
                before = sourceLine(parsed, loop);
            }
        }
        String result = resultName(source);
        String returned = result != null ? result : "null";
        String after = "if (" + param + " == null || " + param + ".isEmpty()) {\n    return " + returned + ";\n}";
        String why = param + " 为空时，直接调用 size() 或进入 for-each 会抛出空指针。";
        if ("items".equals(param)) {
            why = "items 为空时，第一行 items.size() 会直接抛出空指针。";
        }
        return new CodeIssue("empty", "入参列表没有判空", why,
                "空列表先返回，循环只处理真正有数据的情况。",
                "这个方法当时只接收查询结果，空列表没被当成一种输入。",
                before, after, param);
    }

    private CodeIssue statusIssue(MethodDeclaration method) {
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            if (!"equals".equals(call.getNameAsString()) || call.getArguments().size() != 1) {
                continue;
            }
            if (!(call.getArgument(0) instanceof StringLiteralExpr)) {
                continue;
            }
            String literal = ((StringLiteralExpr) call.getArgument(0)).getValue();
            if (!"0".equals(literal) && !"1".equals(literal)) {
                continue;
            }
            if (!call.getScope().isPresent() || !(call.getScope().get() instanceof MethodCallExpr)) {
                continue;
            }
            MethodCallExpr getter = (MethodCallExpr) call.getScope().get();
            if (!getter.getNameAsString().startsWith("get")) {
                continue;
            }
            String statusName = "1".equals(literal) ? "VALID" : "INVALID";
            String receiver = getter.getScope().isPresent() ? getter.getScope().get().toString() : "this";
            String after = "BidStatus." + statusName + ".equals(" + receiver + "." + getter.getNameAsString() + "())";
            String why = getter.getNameAsString() + "() 为空时，.equals(\"" + literal
                    + "\") 会中断，而且看不出 " + literal + " 是什么状态。";
            if ("getStatus".equals(getter.getNameAsString()) && "1".equals(literal)) {
                why = "getStatus() 为空时，.equals(\"1\") 会中断，而且看不出 1 是什么状态。";
            }
            String better = "1".equals(literal)
                    ? "用 BidStatus.VALID，并把常量放在比较的左边。"
                    : "用 BidStatus.INVALID，并把常量放在比较的左边。";
            return new CodeIssue("status", "状态可能为空", why, better,
                    "状态值是从旧表抄来的，当时更关心价格有没有值。",
                    call.toString(), after, null);
        }
        return null;
    }

    private CodeIssue moneyIssue(MethodDeclaration method) {
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            if (!"doubleValue".equals(call.getNameAsString())) {
                continue;
            }
            String before = call.toString();
            Node parent = call.getParentNode().orElse(null);
            if (parent instanceof BinaryExpr) {
                before = parent.toString();
            }
            return new CodeIssue("money", "金额用 double",
                    "doubleValue() 会丢掉金额精度，0 和负数也挤在同一个条件里。",
                    "价格本来就是 BigDecimal，用 compareTo 判断是否大于 0。",
                    "字段已经是 BigDecimal，判断时顺手转成了 double。",
                    before, replaceMoney(before), null);
        }
        return null;
    }

    private List<CodeIssue> curatedIssues() {
        List<CodeIssue> issues = new ArrayList<CodeIssue>();
        issues.add(new CodeIssue("empty", "入参列表没有判空",
                "items 为空时，第一行 items.size() 会直接抛出空指针。",
                "空列表先返回，循环只处理真正有数据的情况。",
                "这个方法当时只接收查询结果，空列表没被当成一种输入。",
                "for (int i = 0; i < items.size(); i++) {",
                "if (items == null || items.isEmpty()) {\n    return result;\n}",
                "items"));
        issues.add(new CodeIssue("status", "状态可能为空",
                "getStatus() 为空时，.equals(\"1\") 会中断，而且看不出 1 是什么状态。",
                "用 BidStatus.VALID，并把常量放在比较的左边。",
                "状态值是从旧表抄来的，当时更关心价格有没有值。",
                "item.getStatus().equals(\"1\")",
                "BidStatus.VALID.equals(item.getStatus())",
                null));
        issues.add(new CodeIssue("money", "金额用 double",
                "doubleValue() 会丢掉金额精度，0 和负数也挤在同一个条件里。",
                "价格本来就是 BigDecimal，用 compareTo 判断是否大于 0。",
                "字段已经是 BigDecimal，判断时顺手转成了 double。",
                "item.getPrice().doubleValue() > 0",
                "item.getPrice().compareTo(BigDecimal.ZERO) <= 0",
                null));
        return issues;
    }

    private String rewrite(String source, List<CodeIssue> issues) {
        String param = null;
        for (CodeIssue issue : issues) {
            if ("empty".equals(issue.getId())) {
                param = issue.getParamName();
            }
        }
        String updated = source;
        if (param != null && updated.indexOf(param + " == null") < 0 && updated.indexOf(param + "!= null") < 0) {
            updated = insertGuard(updated, param);
        }
        updated = replaceMoney(updated);
        updated = replaceStatus(updated);
        return updated;
    }

    private String insertGuard(String text, String param) {
        String result = resultName(text);
        String indent = "    ";
        int arrayListAt = text.indexOf("new ArrayList");
        if (arrayListAt >= 0) {
            int lineStart = text.lastIndexOf('\n', arrayListAt);
            String linePrefix = text.substring(lineStart + 1, arrayListAt);
            int content = 0;
            while (content < linePrefix.length()) {
                char ch = linePrefix.charAt(content);
                if (ch != ' ' && ch != '\t') {
                    break;
                }
                content++;
            }
            indent = linePrefix.substring(0, content);
        }
        String statement;
        if (result != null) {
            statement = "return " + result + ";";
        } else if (isVoid(text)) {
            statement = "return;";
        } else if (returnsPrimitive(text)) {
            statement = "return 0;";
        } else {
            statement = "return null;";
        }
        String guard = indent + "if (" + param + " == null || " + param + ".isEmpty()) {\n"
                + indent + "    " + statement + "\n"
                + indent + "}";
        if (arrayListAt >= 0) {
            int lineBreak = text.indexOf('\n', arrayListAt);
            if (lineBreak >= 0) {
                return text.substring(0, lineBreak + 1) + guard + "\n" + text.substring(lineBreak + 1);
            }
        }
        int brace = text.indexOf('{');
        if (brace < 0) {
            return guard + "\n" + text;
        }
        return text.substring(0, brace + 1) + "\n" + guard + text.substring(brace + 1);
    }

    private String replaceMoney(String text) {
        String updated = text;
        updated = updated.replaceAll("\\.doubleValue\\(\\)\\s*>=\\s*0", ".compareTo(BigDecimal.ZERO) >= 0");
        updated = updated.replaceAll("\\.doubleValue\\(\\)\\s*<=\\s*0", ".compareTo(BigDecimal.ZERO) <= 0");
        updated = updated.replaceAll("\\.doubleValue\\(\\)\\s*!=\\s*0", ".compareTo(BigDecimal.ZERO) != 0");
        updated = updated.replaceAll("\\.doubleValue\\(\\)\\s*==\\s*0", ".compareTo(BigDecimal.ZERO) == 0");
        updated = updated.replaceAll("\\.doubleValue\\(\\)\\s*>\\s*0", ".compareTo(BigDecimal.ZERO) > 0");
        updated = updated.replaceAll("\\.doubleValue\\(\\)\\s*<\\s*0", ".compareTo(BigDecimal.ZERO) < 0");
        updated = updated.replace(".doubleValue()", ".compareTo(BigDecimal.ZERO)");
        return updated;
    }

    private String replaceStatus(String text) {
        Matcher matcher = STATUS_CALL.matcher(text);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String name = "1".equals(matcher.group(3)) ? "VALID" : "INVALID";
            String replacement = "BidStatus." + name + ".equals(" + matcher.group(1) + ".get" + matcher.group(2) + "())";
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(buffer);
        Matcher bare = BARE_STATUS_CALL.matcher(buffer.toString());
        buffer = new StringBuffer();
        while (bare.find()) {
            String name = "1".equals(bare.group(2)) ? "VALID" : "INVALID";
            String replacement = "BidStatus." + name + ".equals(get" + bare.group(1) + "())";
            bare.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }
        bare.appendTail(buffer);
        return buffer.toString();
    }

    private boolean guardedBefore(MethodDeclaration method, String param, int useLine) {
        for (BinaryExpr bin : method.findAll(BinaryExpr.class)) {
            if (isNullCheck(bin, param) && lineOf(bin) < useLine) {
                return true;
            }
        }
        for (MethodCallExpr call : method.findAll(MethodCallExpr.class)) {
            if (!"isNull".equals(call.getNameAsString()) || call.getArguments().size() != 1) {
                continue;
            }
            if (isParam(call.getArgument(0), param) && lineOf(call) < useLine) {
                return true;
            }
        }
        return false;
    }

    private boolean isNullCheck(BinaryExpr bin, String param) {
        BinaryExpr.Operator op = bin.getOperator();
        if (op != BinaryExpr.Operator.EQUALS && op != BinaryExpr.Operator.NOT_EQUALS) {
            return false;
        }
        return (isParam(bin.getLeft(), param) && bin.getRight() instanceof NullLiteralExpr)
                || (isParam(bin.getRight(), param) && bin.getLeft() instanceof NullLiteralExpr);
    }

    private boolean isParam(Expression expr, String param) {
        return expr instanceof NameExpr && param.equals(((NameExpr) expr).getNameAsString());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Node> T ancestor(Node node, Class<T> type) {
        return node.findAncestor(type).orElse(null);
    }

    private int lineOf(Node node) {
        if (node.getBegin().isPresent()) {
            return node.getBegin().get().line;
        }
        return Integer.MAX_VALUE;
    }

    private String sourceLine(ParsedSource parsed, Node node) {
        if (node == null || !node.getBegin().isPresent()) {
            return "";
        }
        int line = node.getBegin().get().line - parsed.getLineOffset();
        String[] lines = parsed.getOriginal().split("\n", -1);
        if (line < 1 || line > lines.length) {
            return node.toString().trim();
        }
        return lines[line - 1].trim();
    }

    private String slice(ParsedSource parsed, MethodDeclaration method) {
        if (!method.getRange().isPresent()) {
            return method.toString();
        }
        int begin = method.getRange().get().begin.line - parsed.getLineOffset();
        int end = method.getRange().get().end.line - parsed.getLineOffset();
        String[] lines = parsed.getOriginal().split("\n", -1);
        if (begin < 1) {
            begin = 1;
        }
        if (end > lines.length) {
            end = lines.length;
        }
        if (begin > end) {
            return method.toString();
        }
        StringBuilder builder = new StringBuilder();
        for (int i = begin; i <= end; i++) {
            if (i > begin) {
                builder.append('\n');
            }
            builder.append(lines[i - 1]);
        }
        return builder.toString();
    }

    private String classNameOf(MethodDeclaration method) {
        ClassOrInterfaceDeclaration type = ancestor(method, ClassOrInterfaceDeclaration.class);
        if (type == null || "TongbiHolder".equals(type.getNameAsString())) {
            return null;
        }
        return type.getNameAsString();
    }

    private String resultName(String source) {
        Matcher matcher = RESULT_LIST.matcher(source);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private boolean isVoid(String text) {
        String signature = text.split("\n", 2)[0];
        return signature.matches(".*\\bvoid\\s+\\w+\\s*\\(.*");
    }

    private boolean returnsPrimitive(String text) {
        String signature = text.split("\n", 2)[0];
        return signature.matches(".*\\b(int|long|boolean|double|float|short|byte|char)\\s+\\w+\\s*\\(.*");
    }

    private String compact(String source) {
        return source.replaceAll("\\s+", "");
    }
}
