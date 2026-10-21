# FitNutri 页面功能演示：30 秒英文口语稿与中文解释

> 适用版本：c5fcdac；按主要页面及功能拆分。每段约 60–75 个英文单词，正常演示语速约 30–40 秒，不包含等待、输入和页面切换时间。中文供理解，不需要现场逐段读出。

## 使用方式

- 建议主线：开场 → 登录 → Dashboard → 新增入口 → 饮食 → Review → History → Profile。约 4 分钟口述，另留操作时间。
- 菜谱、Workout、统计和校验按老师关注点加讲；全部 18 段约 9–12 分钟口述，现场操作需要额外时间。
- 新账户才展示初始问卷；管理员另用管理员账户。提前准备含菜谱、训练和不同日期记录的合成演示数据。
- 本稿依据页面路由和实现整理，本次没有运行现场演示或重新验证服务配置。
- AI 未配置时使用这句替代开场：**“This feature is implemented, but the live AI service is not configured in this demonstration environment. I will explain the workflow and show how the application handles its availability.”**
- 不必现场制造网络故障或并发冲突；可以讲解机制。输入校验用空必填或零数量等简单案例即可。

## 01. 开场：项目概览

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 停留在 Dashboard，指向导航和主要数据区域。 |
| English script | Today, I will demonstrate FitNutri, a web application for recording food intake and exercise. The main workflow is simple: users enter their information, review the details, and confirm the record. They can then check their daily overview and history. I will focus on how the pages work together, and show a few checks that help prevent incorrect or incomplete records. |
| 中文解释 | 介绍项目是饮食和运动记录应用。先交代录入、检查、确认、查看结果这条主线，再说明会展示输入校验。 |

## 02. 登录与注册

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 打开 /login；先尝试空表单，再使用演示账户登录。 |
| English script | This is the login and registration page. Users can create a personal account and sign in to access their own information. If required fields are missing or the email format is incorrect, the form shows a clear message. After login, the application directs users according to their account and setup status. Administrator access is controlled separately from normal user access. |
| 中文解释 | 说明注册登录、必填和邮箱格式检查，以及登录后根据账户和问卷状态进入不同页面。管理员与普通用户权限分开。 |

## 03. 初始问卷

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 打开 /onboarding；指向基本资料、健康问题和同意项，展示一个必填提示。 |
| English script | For a new user, this questionnaire collects the basic information needed to use the application. It includes body information and health-related questions. Required answers are checked before setup can be completed. The health answers and consent also determine whether general health Q and A is available. Users who cannot use that feature can still record their food and exercise. |
| 中文解释 | 问卷建立基本资料；必填内容需要完整。健康问答资格受回答和同意状态影响，但不影响基础饮食、运动记录。 |

## 04. 每日总览 Dashboard

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 打开 /dashboard；指向日期、能量、营养、运动分钟和记录列表。 |
| English script | This dashboard brings the selected day’s information together. Users can see recorded energy intake, exercise expenditure, nutrients, and activity time. The food and exercise sections show the saved entries for that date. Users can change the date or refresh the information. Missing estimates are shown clearly, so the page does not present an unknown value as a confirmed zero. |
| 中文解释 | 总览把选定日期的数据集中展示。重点解释“未知”与“零”不同；页面显示的是已保存的数据，并非模型随意给出的健康判断。 |

## 05. 新增记录入口

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 打开 /add；切换 Food、Exercise，指向 Manual、Voice、Image。 |
| English script | This page lets users choose what they want to record and how they want to enter it. They can select food or exercise, then choose an available input method. Manual entry is the basic route. Voice and food image options depend on the configured services. When a service is unavailable, the page explains the reason instead of showing a fake successful result. |
| 中文解释 | 入口统一选择记录类型和输入方式。图片仅用于饮食；AI 服务不可用时如实显示原因。现场没有配置时，就展示禁用状态，不声称调用成功。 |

## 06. 手动饮食录入

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 进入 Food 手动录入；搜索参考项、填写克数和餐次，再加入待确认列表。 |
| English script | For manual food entry, I search the food catalogue and select a reference item. I then enter the amount and choose the meal. The application checks the required inputs and requests a nutrition preview from the server. I can add several foods before continuing. This keeps the food selection, quantity, and calculated nutrition connected, rather than relying on values typed independently by the user. |
| 中文解释 | 选择真实参考食物，再填写数量与餐次。营养预览由后端计算；多种食物可以一起进入最终确认，减少数量和营养值不一致的问题。 |

## 07. 菜谱与个人份量

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 在饮食编辑器选择 Dish；添加食材，展示整道菜的 1/3 份量和预览。 |
| English script | The dish feature supports meals with several ingredients. Users enter the original recipe and specify how much of the dish they ate, for example, one third. The server calculates the consumed amount and nutrition for each ingredient. The dish is then managed as one saved entry. This preserves the recipe while avoiding the need to manually divide every ingredient before recording a portion. |
| 中文解释 | 用“一道菜吃了三分之一”解释价值：保留原配方，由系统按份量计算实际摄入；历史中作为一道菜管理，避免手工逐项换算。 |

## 08. 快速运动录入

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 选择 Exercise 的 Quick 模式；填写日期、运动选项和时长，展示估算或无法估算提示。 |
| English script | For a quick exercise record, I choose an activity, enter the duration, and select the date. The server estimates energy using an applicable activity reference and the latest weight recorded on or before that date. If the required information is missing, the activity can still be recorded without an energy estimate. The application explains that limitation instead of silently using a default body weight. |
| 中文解释 | 运动按日期选择适用体重，补录旧运动时不会直接使用今天的体重。数据不足时仍可记录运动，但明确保留“无法估算”，不偷偷使用默认体重。 |

## 09. Workout 完整训练

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 切换 Workout；展示两个时段及其中的动作、组数或负重详情。 |
| English script | The workout feature records a training session with multiple timed periods and movement details. Users can include information such as sets, repetitions, or load where supported. Time and energy belong to the activity periods, while movements provide extra detail. This prevents several movements within one period from multiplying the total duration. The complete workout can also be reviewed and managed as a single entry. |
| 中文解释 | 一次训练包含多个计时时段，时段内再记录动作详情。三个动作不代表同一段时间累计三遍；组数、负重等详情也不自动等于个性化热量计算依据。 |

## 10. Review 与确认保存

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 打开 /review；指向数量、日期、餐次或时长；确认一次，返回 Dashboard 查看保存结果。 |
| English script | Before saving, this review page brings the entry details together. Users can check the date, quantities, meal, or exercise duration and return to editing if needed. Confirmation submits the reviewed information to the server. Related items are saved together, and the application uses a submission receipt to identify the result. Retrying the same submission therefore does not create another copy of the record. |
| 中文解释 | Review 是最终核对入口，确认前可返回修改。相关条目一起保存；同一提交身份重试会返回原结果。不要说成任何相同内容、跨刷新重新录入都能自动去重。 |

## 11. 历史查询与详情

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 打开 /history；设置日期和类型，展开菜谱或 Workout 详情。 |
| English script | The history page lets users find saved records by date range and record type. They can open an entry to see its details, including ingredients or workout periods. Grouped records remain together, so a dish is not displayed as several unrelated meals. The page also distinguishes older or incomplete information where needed. This makes the saved information easier to inspect and explain. |
| 中文解释 | 按日期和类型查历史，展开详细信息。菜谱和 Workout 保持整体关系，用户能看清一次记录包含哪些明细，也能识别旧数据或不完整信息。 |

## 12. 修改、删除与再次录入

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 从记录菜单展示 Edit、Delete、Log again；修改演示数据，并说明删除前会确认。 |
| English script | From a saved entry, users can edit details, delete the entry, or start a new draft using a previous selection. Grouped entries are updated together. If another update has already changed the record, the server reports a conflict rather than silently overwriting it. Deletion removes the entry from normal views and totals, while the original creation receipt is retained on the server. |
| 中文解释 | 修改和删除以整道菜或整次训练为单位，避免残留明细。旧版本修改会被拦截；Log again 创建新草稿，需要重新检查输入。后台保留创建回执不等于界面支持撤销删除。 |

## 13. 趋势、图表与数据表

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 在 Dashboard 展示 Recorded intake 图表，再打开 Data table 或相关历史范围。 |
| English script | The recorded intake chart helps users review changes across recent days. It is based on saved records and uses the selected display unit. Users can also inspect the supporting values in a data table. Missing information remains visible rather than being treated as a complete day of zero intake. Grouped food and exercise details are counted consistently, so parent entries do not duplicate their components in the totals. |
| 中文解释 | 趋势图展示记录变化，并有数据表辅助查看。不要把它讲成独立 Trends 页面；重点说明缺失日期不冒充零摄入、菜谱父子项不会重复统计。 |

## 14. 个人资料、目标与体重

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 打开 /profile；展示显示单位、个人目标和按日期的体重记录。 |
| English script | The profile page lets users maintain their personal information, display settings, and goals. They can choose their energy unit and manage dated weight records. A personal energy reference is optional, rather than automatically presented as a prescribed target. Weight corrections are checked for conflicts. Historical exercise records keep their stored calculation results unless the user explicitly makes a relevant correction. |
| 中文解释 | 资料页管理单位、可选目标和体重历史。个人能量参考不是医学处方；更新今天的体重，不会悄悄重算所有旧运动记录。 |

## 15. 图片与语音辅助录入

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | AI 可用时展示候选或转写；未配置时指向可用性提示，按下方替代句说明。 |
| English script | Image and voice input provide alternative starting points for recording. A food image can produce candidates for review. Voice input is transcribed into editable text, which is then parsed into suggested food or exercise details. Neither process automatically creates a confirmed record. Users still check the reference, quantity, date, and other required fields before using the same review and confirmation process as manual entry. |
| 中文解释 | 图片和语音只是辅助起点。识别、转写、解析之后仍需要用户核对和确认，不直接写入正式记录。真实服务未配置时，仅讲流程和不可用状态，不模拟成功。 |

## 16. 健康问答

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 打开 /chat；指向资格提示、问题输入和可选记录范围；未配置时解释不可用状态。 |
| English script | This page provides general health Q and A for eligible users. The application checks the user’s current answers and consent before allowing access. If a record range is selected, only a limited summary of that user’s records is included as context. The feature has scope and safety restrictions and is not a diagnosis service. Availability also depends on the configured AI provider. |
| 中文解释 | 强调资格、同意和本人有限范围汇总。问答用于一般健康信息，不作诊断；真实提供方未配置时不能现场展示假答案。 |

## 17. 管理员页面

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 切换到单独管理员演示账户；打开 /admin，搜索合成用户，指向状态和密码操作。 |
| English script | The administrator page is separate from the normal user workflow. An administrator can search accounts and manage supported account actions, such as deactivation, reactivation, and password reset. These actions require current permissions and version checks. The system also protects against unsafe changes involving the acting administrator or the last active administrator. This page manages accounts without exposing users’ private food and exercise records. |
| 中文解释 | 管理员管理账户，不查看用户私密饮食运动内容。操作有权限与版本检查，并保护当前操作者和最后一个有效管理员。演示只用合成账户，避免误停用自己。 |

## 18. 输入校验与错误处理

| 项目 | 内容 |
| --- | --- |
| 页面操作提示 | 选一个低风险例子：空必填、零克数或结束日期早于开始日期；修正后继续。 |
| English script | I will finish by showing a simple validation example. If a required value is missing or a quantity is invalid, the application explains what needs to be corrected before the record can proceed. Server-side checks also protect ownership, versions, and consistency. If a service fails, the interface reports the problem and offers the appropriate recovery path, rather than presenting a failed operation as a successful save. |
| 中文解释 | 用一个能稳定复现的输入错误收尾，说明前端提示和后端检查共同作用。版本冲突、网络响应丢失不是普通必填错误，恢复方式不同，不要混为“点重试就行”。 |

## 备用结束句

“That completes the main workflow: enter, review, confirm, and track. The key point is that the pages share the same saved records and validation rules, so users can move from recording to reviewing their history in a consistent way.”

中文：用录入、核对、确认和追踪四步收尾，强调页面共享同一套已保存数据和校验规则。

## 讲解时避免的表述

| 不建议说 | 更准确的说法 |
| --- | --- |
| The AI features are fully tested and ready. | The adapters and controlled tests are implemented; live-provider acceptance is separate. |
| The app calculates exactly how many calories you burn. | The app provides an estimate when suitable reference data and dated weight are available. |
| All duplicate records are prevented. | Retrying the same submission identity does not create another copy. |
| Missing values mean zero. | Missing or unavailable values are shown separately from known zero values. |
| Changing weight updates all past exercise results. | Historical records keep their stored results unless a relevant correction is explicitly made. |
