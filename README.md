# plugin-lightgallery

提供对 [lightgallery.js](https://github.com/sachinchoolur/lightgallery.js) 的集成，支持在内容页放大显示图片。

## 开发环境

需要 Java 21、Node.js 24、pnpm 10.33.0，运行环境为 Halo 2.26.0 或更高版本。

```bash
git clone git@github.com:halo-sigs/plugin-lightgallery.git

# 或者当你 fork 之后

git clone git@github.com:{your_github_id}/plugin-lightgallery.git
```

```bash
cd path/to/plugin-lightgallery
```

```bash
# macOS / Linux
./gradlew build

# Windows
./gradlew.bat build
```

启动 Docker 后，使用 DevTools 运行 Halo 开发环境：

```bash
./gradlew haloServer
# 修改代码后重新构建并加载插件
./gradlew reloadPlugin
```

构建基线参考 [Halo 官方插件模板](https://github.com/halo-dev/create-halo-plugin/tree/d713a6c14b5060e8f76a76a4579a7b6ac5896efb/template)：Halo 2.26、Java 21、Gradle 9.4、DevTools 0.8.0、Lombok 9.2.0、Node Gradle 7.1.0，以及模板的 Vite、TypeScript 和 pnpm 版本范围。实际前端版本固定在锁文件中。

本插件没有 Console UI 扩展，主题脚本使用标准 ESM，通过页面中的 `type="module"` 脚本导入并初始化，不依赖 `window.lightGallery` 或 Console 加载器。模块脚本会在文档解析后执行，无需额外监听 `DOMContentLoaded`。不引入模板的 Vue、Console bundler 或 UI 子模块。静态资源只生成到 `build/generated-resources/static` 并打入 JAR，不提交生成文件。Java 测试也会通过 Node.js 执行页面初始化脚本的行为回归。

CI 使用模板的共享工作流 v4。CD 暂保留 v1 的用户名/密码发布方式；迁移 v4 需要先为上游仓库配置 `halo-pat`，不能直接沿用旧 secrets。

## 使用方式

1. 在 [Releases](https://github.com/halo-sigs/plugin-lightgallery/releases) 下载最新的 JAR 文件。
2. 在 Halo 后台的插件管理上传 JAR 文件进行安装。
3. 进入插件设置，在「页面匹配规则」中添加需要启用灯箱的页面路径和图片所在区域。

### 页面匹配规则

每条规则由两部分组成：

- **路径匹配**：使用 Spring `PathPattern` 语法，不是正则表达式。填写页面路径，例如 `/moments`，不要填写域名、查询参数或 `#` 后的片段。语法参考 [PathPattern 文档](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/web/util/pattern/PathPattern.html)。
- **匹配区域**：使用 CSS 选择器，例如 `#content` 或 `.markdown-body`，插件会为匹配区域内的图片启用灯箱。同一选择器可以匹配多个区域。

匹配区域留空表示整个页面，Logo 等导航图片也会被包含。通常应填写主题实际使用的正文区域选择器；不同主题的正文结构可能不同。

「内容页面匹配」是旧版设置，仅为兼容保留，建议新配置使用「页面匹配规则」。

## 主题适配

此插件无需主题主动适配即可使用，其原理就是将 `lightgallery.js` 所需的依赖引入和初始化代码都自动插入到了内容页面上。因此，主题开发者无需再针对图片放大进行适配开发，如果有特殊的需求，建议共同完善此插件。

## lightGallery 2

图库升级为 lightGallery 2.9.0，缩放插件和样式随插件打包，无需主题额外加载资源。

本项目按 GPL-3.0 发布。lightGallery 2 的默认开发密钥会在浏览器控制台显示生产使用提醒；上游要求 GPLv3 兼容项目联系作者获取密钥，正式发布前需完成该事项。参见 [上游设置文档](https://www.lightgalleryjs.com/docs/settings/#licenseKey) 和 [授权说明](https://www.lightgalleryjs.com/license/)。
