# Nebula Compute Console

AI 算力租赁与交付平台前端原型，使用 React 18 + TypeScript + Vite 构建。

## 启动

```bash
cd frontend
npm install
npm run dev
```

默认地址：http://localhost:5173/

## 页面

- `/` 总览：集群利用率、实例、订单、Agent 洞察
- `/market` GPU 市场：架构筛选、区域筛选、租期配置与下单弹窗
- `/orders` 订单中心：状态筛选、订单列表、履约时间线抽屉
- `/instances` 我的实例：运行状态、利用率、SSH endpoint、诊断入口
- `/billing` 账单与用量：余额、预算、资源成本拆分、账单
- `/support` 售后智能体：对话、上下文、工具权限预览

当前数据为前端 mock，后续接入 Java 后端 API。
