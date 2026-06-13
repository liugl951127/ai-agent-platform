# Agent UI

Vue 3 + Vite + Element Plus 前端。

## 开发
```bash
npm install
npm run dev   # http://localhost:8080
```

## 构建
```bash
npm run build
```

## 目录
```
src/
├── api/         # axios 封装
├── store/       # pinia
├── router/      # vue-router
├── layout/      # Layout
├── views/       # 页面: Chat / Agents / Models / Knowledge / Tools / Workflow
└── main.js
```

## 后端代理
vite.config.js 中将 `/api` 代理到 `http://localhost:9000` (Spring Cloud Gateway)。
