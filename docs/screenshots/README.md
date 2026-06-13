# 📸 Screenshots & Demo

> 待补充:本目录用于存放平台运行截图 / GIF / 录屏,展示给访问者直观体验。

## 计划截图清单

| 文件 | 内容 | 状态 |
|---|---|---|
| `01-login.png` | 登录页 (渐变背景) | ⏳ TODO |
| `02-chat.png` | 智能对话页 (流式输出 + Markdown 渲染) | ⏳ TODO |
| `03-agents.png` | 智能体管理 (CRUD) | ⏳ TODO |
| `04-models.png` | 大模型管理 (多 Provider) | ⏳ TODO |
| `05-knowledge.png` | 知识库检索 (ES) | ⏳ TODO |
| `06-workflow.png` | Flowable 流程编排 | ⏳ TODO |
| `07-nacos.png` | Nacos 服务注册列表 | ⏳ TODO |
| `08-knife4j.png` | API 文档 (Knife4j UI) | ⏳ TODO |
| `demo.gif` | 从启动到对话的完整 demo 录屏 | ⏳ TODO |

## 怎么贡献截图

1. 把 `docker compose up -d` 跑起来
2. 在浏览器里跑完一个完整流程
3. 截图保存为上表文件名
4. PR 提交即可,300x600 以上分辨率最佳

## Demo 录屏工具推荐

- **macOS**: `Cmd + Shift + 5`
- **Windows**: `Win + G` (Xbox Game Bar)
- **Linux**: `peek` / `Kazam`
- **跨平台**: [OBS Studio](https://obsproject.com/)
- **转 GIF**: `ffmpeg -i demo.mov -vf "fps=15,scale=1200:-1" demo.gif`
