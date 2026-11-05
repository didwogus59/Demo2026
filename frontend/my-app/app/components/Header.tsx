"use client";

import Link from "next/link";
import { useEffect, useState, useCallback } from "react";

type ServerStatus = "checking" | "online" | "offline";

export default function Header() {
  const [status, setStatus] = useState<ServerStatus>("checking");

  const checkBackendHealth = useCallback(async () => {
    try {
      const controller = new AbortController();
      const timeoutId = setTimeout(() => controller.abort(), 2500);

      const res = await fetch("/api/Kafka/get", {
        method: "GET",
        cache: "no-store",
        signal: controller.signal,
      });
      clearTimeout(timeoutId);

      if (res.ok) {
        setStatus("online");
      } else {
        setStatus("offline");
      }
    } catch {
      setStatus("offline");
    }
  }, []);

  useEffect(() => {
    const initialTimeout = setTimeout(checkBackendHealth, 0);
    const interval = setInterval(checkBackendHealth, 5000);
    return () => {
      clearTimeout(initialTimeout);
      clearInterval(interval);
    };
  }, [checkBackendHealth]);

  const statusConfig = {
    checking: {
      dotClass: "bg-amber-400 animate-pulse",
      label: "Backend :8080 (확인 중)",
    },
    online: {
      dotClass: "bg-emerald-500",
      label: "Backend :8080 (ON)",
    },
    offline: {
      dotClass: "bg-red-500",
      label: "Backend :8080 (OFF)",
    },
  }[status];

  return (
    <header className="sticky top-0 z-30 flex h-16 w-full items-center justify-between border-b border-zinc-200 bg-white/80 px-6 backdrop-blur dark:border-zinc-800 dark:bg-zinc-950/80">
      <div className="flex items-center gap-3">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-zinc-900 text-sm font-bold text-white dark:bg-zinc-100 dark:text-zinc-900">
          FD
        </div>
        <Link
          href="/"
          className="text-lg font-semibold tracking-tight text-zinc-900 dark:text-zinc-100"
        >
          FirstDemo
        </Link>
        <span className="rounded-full bg-zinc-100 px-2.5 py-0.5 text-xs font-medium text-zinc-600 dark:bg-zinc-800 dark:text-zinc-400">
          Hexagonal + Next.js
        </span>
      </div>

      <nav className="flex items-center gap-6 text-sm font-medium text-zinc-600 dark:text-zinc-400">
        <a
          href="#coupons"
          className="transition-colors hover:text-zinc-900 dark:hover:text-zinc-100"
        >
          Coupons
        </a>
        <a
          href="#users"
          className="transition-colors hover:text-zinc-900 dark:hover:text-zinc-100"
        >
          Users
        </a>
        <a
          href="#kafka"
          className="transition-colors hover:text-zinc-900 dark:hover:text-zinc-100"
        >
          Kafka
        </a>
        <button
          type="button"
          onClick={checkBackendHealth}
          title="클릭하여 백엔드 상태 다시 확인"
          className="flex cursor-pointer items-center gap-2 rounded-full border border-zinc-200 px-3 py-1 text-xs transition hover:bg-zinc-100 dark:border-zinc-800 dark:hover:bg-zinc-900"
        >
          <span className={`h-2 w-2 rounded-full ${statusConfig.dotClass}`} />
          <span>{statusConfig.label}</span>
        </button>
      </nav>
    </header>
  );
}
