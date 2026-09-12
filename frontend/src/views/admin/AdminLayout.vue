<template>
  <div class="admin">
    <aside>
      <h2>运营后台</h2>
      <p>本期无需登录</p>
      <nav>
        <RouterLink to="/admin/stations">车站管理</RouterLink>
        <RouterLink to="/admin/trains">车次经停</RouterLink>
        <RouterLink to="/admin/fares">票价管理</RouterLink>
        <RouterLink to="/admin/runs">开售库存</RouterLink>
      </nav>
      <RouterLink class="back" to="/">返回前台</RouterLink>
    </aside>
    <section class="content">
      <RouterView />
    </section>
  </div>
</template>

<style scoped>
.admin { min-height: 100vh; display: grid; grid-template-columns: 220px 1fr; background: #f4f6f8; }
aside {
  background: #163a61;
  color: #fff;
  padding: 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
h2 { margin: 0; }
p { margin: 0; color: #9db4cc; font-size: 12px; }
nav { display: grid; gap: 6px; }
nav a, .back {
  padding: 10px 12px;
  border-radius: 8px;
  color: #d5e4f3;
}
nav a.router-link-active, nav a:hover { background: rgba(255,255,255,.12); color: #fff; }
.back { margin-top: auto; color: #9db4cc; }
.content { padding: 24px; }
@media (max-width: 800px) {
  .admin { grid-template-columns: 1fr; }
}
</style>
