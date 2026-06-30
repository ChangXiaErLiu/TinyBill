# 静态代码分析工具

本项目使用 [Detekt](https://detekt.dev/) 进行 Kotlin 静态代码分析。

## 配置文件

- 主配置: [config/detekt/detekt.yml](../config/detekt/detekt.yml)
- 集成位置: [app/build.gradle.kts](../app/build.gradle.kts)

## 已启用的关键规则

### complexity（复杂度）
- `LongMethod` - 方法超过 80 行告警
- `LongParameterList` - 参数超过 8 个告警
- `ComplexCondition` - 复杂条件表达式
- `NestedBlockDepth` - 嵌套层级超过 5

### potential-bugs（潜在 Bug）
- `CastToNullableType` - 禁止强转为可空类型
- `ImplicitDefaultLocale` - 必须显式指定 Locale
- `UnreachableCode` - 死代码检测

### performance（性能）
- `ArrayPrimitive` - 优先使用基本类型数组
- `ForEachOnRange` - 范围循环不要用 forEach
- `SpreadOperator` - 警惕展开运算符性能

### style（风格）
- `MagicNumber` - 业务常量应提取为命名常量
- `MaxLineLength` - 行长 ≤ 140 字符
- `ReturnCount` - 单方法返回点 ≤ 4
- `UnusedImports` / `UnusedPrivateMember` - 自动清理

## 本地运行

需要先有 gradle wrapper，然后：

```bash
# 全项目扫描
./gradlew detekt

# 仅 Android 模块
./gradlew :app:detekt

# 应用自动修复
./gradlew detekt -PautoCorrect=true
```

报告输出位置：`app/build/reports/detekt/detekt.html` 与 `detekt.xml`

## CI 集成建议

```yaml
# .github/workflows/detekt.yml
- name: Run detekt
  run: ./gradlew detekt
- name: Upload reports
  uses: actions/upload-artifact@v3
  with:
    name: detekt-reports
    path: app/build/reports/detekt/
```

`ignoreFailures = true` 表示 detekt 错误不会中断构建；
发布前可临时改为 `false` 强制清零技术债。
