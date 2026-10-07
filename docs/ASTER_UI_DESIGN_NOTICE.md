# Aster UI design attribution / Aster 界面设计来源说明

## 中文

SterSU 的侧栏导航及相关页面布局设计参考了开源项目
[Aster](https://github.com/LyraVoid/Aster)。感谢 LyraVoid 及其贡献者。

- 参考版本：[`86488f8b204853caadce370b5646f9b35e31d0ce`](https://github.com/LyraVoid/Aster/commit/86488f8b204853caadce370b5646f9b35e31d0ce)，提交日期 2026-09-25。
- 查阅内容：README、`app/src/main/java/me/bmax/apatch/ui/shell/GlobalLayout.kt` 和 `app/src/main/java/me/bmax/apatch/ui/shell/AsterAppShell.kt`。
- 设计参考范围：导航与内容区域的分离、统一的页面顶部结构，以及清晰的设置层级。
- SterSU 的具体适配：黑白玻璃侧栏、左侧/右侧切换、中部组件定制、导航按钮排序与自定义图标，以及 Material 内容区域和手机/平板布局。
- 2026-10-07 的统一适配：侧栏导航标签与焦点反馈、大字体/短屏滚动、明暗及 AMOLED 中性表面、主页状态卡、设备设置卡、固定搜索页头、二级页内容宽度与重启菜单。保留自定义强调色、真实状态和原有操作确认，不改动其他界面风格。

本次侧栏改动未导入 Aster 的源码文件、图片资源、新依赖或 Root 后端，
而是在 SterSU 现有管理器实现中进行设计适配。此说明仅描述本次改动的来源范围，
不替代已有的 FolkPatch 代码引用声明及其他第三方来源说明。

SterSU 是独立项目，并非 Aster 官方发行版；本声明不代表双方存在隶属、
合作、授权品牌使用或背书关系。项目名称和相关标识归各自权利人所有。

Aster 仓库提供 GNU GPL 第 3 版许可证文本，详见
[参考版本的 LICENSE](https://github.com/LyraVoid/Aster/blob/86488f8b204853caadce370b5646f9b35e31d0ce/LICENSE)。
本声明不变更 SterSU 的既有许可证划分，也不替代源码中的版权、许可证及来源声明。
后续如实际引入上游代码或资源，应另行保留对应文件的版权和许可信息并记录修改。

## English

SterSU's sidebar navigation and related page layouts take design inspiration
from [Aster](https://github.com/LyraVoid/Aster). Thanks to LyraVoid and the
project's contributors.

- Reference commit: [`86488f8b204853caadce370b5646f9b35e31d0ce`](https://github.com/LyraVoid/Aster/commit/86488f8b204853caadce370b5646f9b35e31d0ce), dated 2026-09-25.
- Reviewed material: the README, `app/src/main/java/me/bmax/apatch/ui/shell/GlobalLayout.kt`, and `app/src/main/java/me/bmax/apatch/ui/shell/AsterAppShell.kt`.
- Design scope: separating navigation from content, consistent page chrome,
  and a clear settings hierarchy.
- SterSU adaptations: a monochrome glass sidebar, left/right placement,
  center-widget customization, navigation order and custom icons, Material
  content, and phone/tablet layouts.
- The 2026-10-07 consistency pass covers labeled navigation and focus feedback,
  large-text/short-window scrolling, neutral light/dark/AMOLED surfaces, home
  status cards, device settings, pinned search headers, secondary-page widths,
  and the reboot menu. User accents, real status, and existing action
  confirmations are retained; other interface styles are not redesigned.

This sidebar update does not import Aster source files, image assets, new
dependencies, or its root backend. It adapts design ideas within SterSU's
existing Manager implementation. This statement is limited to the present
sidebar work; existing FolkPatch code attribution and other third-party
notices remain in place.

SterSU is an independent project, not an official Aster release. No affiliation,
partnership, trademark authorization, or endorsement is implied. Project names
and marks belong to their respective owners.

Aster publishes the GNU GPL version 3 license text; see the
[license at the reference commit](https://github.com/LyraVoid/Aster/blob/86488f8b204853caadce370b5646f9b35e31d0ce/LICENSE).
This acknowledgement does not change SterSU's existing license split or replace
source-level copyright, license, and provenance notices. Any future import of
upstream code or assets must preserve the applicable notices and document local
modifications separately.
