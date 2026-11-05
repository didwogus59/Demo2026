export default function Sidebar() {
  const menuItems = [
    { label: "대시보드", href: "#", badge: "Home" },
    { label: "선착순 쿠폰 (Redis+Kafka)", href: "#coupons", badge: "Flash Deal" },
    { label: "유저 관리 (MongoDB)", href: "#users", badge: "Port/Adapter" },
    { label: "Kafka 테스트", href: "#kafka", badge: "Messaging" },
  ];

  return (
    <aside className="hidden w-64 shrink-0 border-r border-zinc-200 bg-zinc-50/50 p-4 dark:border-zinc-800 dark:bg-zinc-900/40 md:block">
      <div className="mb-4 px-3 text-xs font-semibold uppercase tracking-wider text-zinc-400">
        Menu
      </div>
      <ul className="space-y-1">
        {menuItems.map((item) => (
          <li key={item.label}>
            <a
              href={item.href}
              className="flex items-center justify-between rounded-lg px-3 py-2.5 text-sm font-medium text-zinc-700 transition-colors hover:bg-zinc-200/60 hover:text-zinc-900 dark:text-zinc-300 dark:hover:bg-zinc-800 dark:hover:text-zinc-100"
            >
              <span>{item.label}</span>
              <span className="rounded bg-zinc-200/80 px-1.5 py-0.5 text-[10px] font-medium text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400">
                {item.badge}
              </span>
            </a>
          </li>
        ))}
      </ul>

      <div className="mt-8 rounded-xl border border-zinc-200 bg-white p-4 dark:border-zinc-800 dark:bg-zinc-900">
        <p className="text-xs font-semibold text-zinc-900 dark:text-zinc-100">
          아키텍처 구조
        </p>
        <p className="mt-1 text-xs leading-relaxed text-zinc-500 dark:text-zinc-400">
          Next.js 프론트엔드에서 <code className="rounded bg-zinc-100 px-1 dark:bg-zinc-800">/api/*</code> 프록시를 통해 Spring Boot(8080) 백엔드와 통신합니다.
        </p>
      </div>
    </aside>
  );
}

