# 同笔

把一段 Java 方法贴进来。同笔只用规则指出三件事：入参列表没判空、状态用 `equals("1")` / `equals("0")`、金额用 `doubleValue()`。然后给一版仍使用 for 循环和 ArrayList 的改写，并说明为什么要换、为什么更好、常见原因是什么。

这不是模型打分。正确性、可读性、风格都由固定规则算出来。示例方法 `BidItemService.filterValid` 原分是 47，建议稿重评是 85。

## 目录

- `backend`：Spring Boot 2.7.18，Java 1.8，JavaParser 3.24.4
- `frontend`：Vue 3 + Vite
- `scripts`：本地启动脚本

会话放在内存里，重启后端就没了。一次只聚焦问题最多的那个方法；类里还有别的方法时，页面会写明。

## 启动

详细步骤见 [启动.md](启动.md)。

后端需要本机 JDK 8 和 Maven：

```bat
scripts\start-backend.cmd
```

前端需要 Node 20，然后打开 http://localhost:5173 ：

```bat
scripts\start-frontend.cmd
```

后端监听 8080。前端把 `/api` 代理过去。

## 评分

- 正确性从 90 起：空列表 -24，金额 -22，状态 -22
- 可读性从 74 起：有状态问题再 -24
- 风格从 70 起：for 循环 +12，ArrayList +6，出现 `stream(` -28
- 总分 = 正确性 × 0.45 + 可读性 × 0.3 + 风格 × 0.25，四舍五入到 0–100

对照台左边是原方法。右边的改写在点击「按现在的改写重新评分」之前不计入分数。问题单另算：全未解决 52，三条都标成已改善是 100。已忽略按该条分值的 6/16 计入。

## 测试

```bat
cd backend
mvn test
```
