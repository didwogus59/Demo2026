"use client";

import { useState, useEffect, useCallback } from "react";

interface UserResponse {
  id: number;
  name: string;
}

interface CouponResponse {
  id: number;
  title: string;
  totalQuantity: number;
  remainingQuantity: number;
  createdAt: string;
}

interface CouponIssueResultResponse {
  couponId: number;
  userId: number;
  status: "SUCCESS" | "DUPLICATED" | "SOLD_OUT" | "COUPON_NOT_FOUND";
  remainingQuantity: number;
  message: string;
}

interface CouponIssueHistoryResponse {
  id: string;
  couponId: number;
  userId: number;
  issuedAt: string;
}

export default function Home() {
  // --- 선착순 쿠폰 상태 ---
  const [couponIdInput, setCouponIdInput] = useState("100");
  const [couponTitleInput, setCouponTitleInput] = useState("선착순 10명 오픈특가 쿠폰");
  const [couponTotalQtyInput, setCouponTotalQtyInput] = useState("10");
  const [creatingCoupon, setCreatingCoupon] = useState(false);

  const [issueCouponId, setIssueCouponId] = useState("100");
  const [issueUserId, setIssueUserId] = useState("1");
  const [burstCount, setBurstCount] = useState("15");
  const [issuingCoupon, setIssuingCoupon] = useState(false);
  const [issueLogs, setIssueLogs] = useState<CouponIssueResultResponse[]>([]);
  const [couponError, setCouponError] = useState<string | null>(null);

  const [coupons, setCoupons] = useState<CouponResponse[]>([]);
  const [issuedHistory, setIssuedHistory] = useState<CouponIssueHistoryResponse[]>([]);
  const [loadingCoupons, setLoadingCoupons] = useState(false);

  // --- 기존 유저 / Kafka 상태 ---
  const [userIdInput, setUserIdInput] = useState("1");
  const [userResult, setUserResult] = useState<UserResponse | null>(null);
  const [userError, setUserError] = useState<string | null>(null);
  const [loadingUser, setLoadingUser] = useState(false);

  const [kafkaResponse, setKafkaResponse] = useState<string | null>(null);
  const [loadingKafka, setLoadingKafka] = useState(false);

  // 쿠폰 목록 및 발급 내역 조회
  const fetchCouponData = useCallback(async (targetCouponId?: string) => {
    setLoadingCoupons(true);
    try {
      const couponsRes = await fetch("/api/coupons", { cache: "no-store" });
      if (couponsRes.ok) {
        const list: CouponResponse[] = await couponsRes.json();
        setCoupons(list);
      }

      const cid = targetCouponId ?? issueCouponId;
      if (cid.trim()) {
        const historyRes = await fetch(
          `/api/coupons/${encodeURIComponent(cid)}/issues`,
          { cache: "no-store" }
        );
        if (historyRes.ok) {
          const history: CouponIssueHistoryResponse[] = await historyRes.json();
          setIssuedHistory(history);
        }
      }
    } catch {
      // 백엔드가 꺼져 있을 때는 조용히 무시
    } finally {
      setLoadingCoupons(false);
    }
  }, [issueCouponId]);

  useEffect(() => {
    const timer = setTimeout(() => {
      fetchCouponData();
    }, 0);
    return () => clearTimeout(timer);
  }, [fetchCouponData]);

  // 1. 쿠폰 생성 & Redis 재고 초기화 (POST /api/coupons)
  const handleCreateCoupon = async () => {
    if (!couponIdInput.trim() || !couponTitleInput.trim() || !couponTotalQtyInput.trim()) {
      return;
    }
    setCreatingCoupon(true);
    setCouponError(null);
    try {
      const res = await fetch("/api/coupons", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          id: Number(couponIdInput),
          title: couponTitleInput,
          totalQuantity: Number(couponTotalQtyInput),
        }),
      });
      if (!res.ok) {
        throw new Error(`쿠폰 생성 실패 (HTTP ${res.status})`);
      }
      setIssueCouponId(couponIdInput);
      setIssueLogs([]);
      await fetchCouponData(couponIdInput);
    } catch (err) {
      setCouponError(err instanceof Error ? err.message : "쿠폰 생성 중 오류 발생");
    } finally {
      setCreatingCoupon(false);
    }
  };

  // 2. 단일 유저 선착순 쿠폰 발급 (POST /api/coupons/{id}/issue)
  const handleIssueSingle = async () => {
    if (!issueCouponId.trim() || !issueUserId.trim()) return;
    setIssuingCoupon(true);
    setCouponError(null);
    try {
      const res = await fetch(
        `/api/coupons/${encodeURIComponent(issueCouponId)}/issue`,
        {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ userId: Number(issueUserId) }),
        }
      );
      const data: CouponIssueResultResponse = await res.json();
      setIssueLogs((prev) => [data, ...prev.slice(0, 29)]);
      setTimeout(() => fetchCouponData(issueCouponId), 300);
    } catch (err) {
      setCouponError(err instanceof Error ? err.message : "쿠폰 발급 요청 실패");
    } finally {
      setIssuingCoupon(false);
    }
  };

  // 3. 동시 다발(Burst) 선착순 요청 시뮬레이션 (Promise.all)
  const handleBurstSimulation = async () => {
    const count = Math.min(Math.max(Number(burstCount) || 10, 1), 100);
    if (!issueCouponId.trim()) return;
    setIssuingCoupon(true);
    setCouponError(null);
    try {
      const requests = Array.from({ length: count }, (_, idx) => {
        const simulatedUserId = idx + 1;
        return fetch(
          `/api/coupons/${encodeURIComponent(issueCouponId)}/issue`,
          {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ userId: simulatedUserId }),
          }
        ).then((r) => r.json() as Promise<CouponIssueResultResponse>);
      });

      const results = await Promise.all(requests);
      setIssueLogs(results);
      // Kafka Consumer가 비동기로 MongoDB에 반영할 시간을 고려해 갱신
      setTimeout(() => fetchCouponData(issueCouponId), 400);
    } catch (err) {
      setCouponError(
        err instanceof Error ? err.message : "동시 요청 시뮬레이션 중 오류 발생"
      );
    } finally {
      setIssuingCoupon(false);
    }
  };

  // GET /users/{id} 호출
  const handleGetUser = async () => {
    if (!userIdInput.trim()) return;
    setLoadingUser(true);
    setUserError(null);
    try {
      const res = await fetch(`/api/users/${encodeURIComponent(userIdInput)}`);
      if (!res.ok) {
        throw new Error(`조회 실패 (HTTP ${res.status})`);
      }
      const data: UserResponse = await res.json();
      setUserResult(data);
    } catch (err) {
      setUserResult(null);
      setUserError(err instanceof Error ? err.message : "요청 중 오류 발생");
    } finally {
      setLoadingUser(false);
    }
  };

  // GET /users/new/{id} 호출 (유저 생성)
  const handleCreateUser = async () => {
    if (!userIdInput.trim()) return;
    setLoadingUser(true);
    setUserError(null);
    try {
      const res = await fetch(
        `/api/users/new/${encodeURIComponent(userIdInput)}`
      );
      if (!res.ok) {
        throw new Error(`생성 실패 (HTTP ${res.status})`);
      }
      const data: UserResponse = await res.json();
      setUserResult(data);
    } catch (err) {
      setUserResult(null);
      setUserError(err instanceof Error ? err.message : "요청 중 오류 발생");
    } finally {
      setLoadingUser(false);
    }
  };

  // GET /Kafka/get 호출
  const handleTestKafka = async () => {
    setLoadingKafka(true);
    try {
      const res = await fetch("/api/Kafka/get");
      if (!res.ok) {
        throw new Error(`호출 실패 (HTTP ${res.status})`);
      }
      const text = await res.text();
      setKafkaResponse(text);
    } catch (err) {
      setKafkaResponse(err instanceof Error ? err.message : "호출 오류");
    } finally {
      setLoadingKafka(false);
    }
  };

  const statusBadgeClass = (status: CouponIssueResultResponse["status"]) => {
    switch (status) {
      case "SUCCESS":
        return "bg-emerald-100 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-300";
      case "DUPLICATED":
        return "bg-amber-100 text-amber-800 dark:bg-amber-950 dark:text-amber-300";
      case "SOLD_OUT":
        return "bg-red-100 text-red-800 dark:bg-red-950 dark:text-red-300";
      default:
        return "bg-zinc-200 text-zinc-700 dark:bg-zinc-800 dark:text-zinc-300";
    }
  };

  return (
    <div className="mx-auto max-w-5xl space-y-8">
      {/* 상단 타이틀 배너 */}
      <section className="rounded-2xl border border-zinc-200 bg-white p-6 shadow-xs dark:border-zinc-800 dark:bg-zinc-900">
        <h1 className="text-2xl font-bold tracking-tight">
          FirstDemo 대시보드
        </h1>
        <p className="mt-1 text-sm text-zinc-600 dark:text-zinc-400">
          Spring Boot 헥사고날 아키텍처 기반 선착순 쿠폰 발급 시스템 (Redis Lua Script + Kafka + MongoDB)
        </p>

        <div className="mt-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
          <div className="rounded-xl border border-zinc-100 bg-zinc-50 p-4 dark:border-zinc-800 dark:bg-zinc-800/50">
            <span className="text-xs font-medium text-rose-600 dark:text-rose-400">
              Step 1 &middot; Redis Lua Script
            </span>
            <p className="mt-1 font-semibold">원자적 동시성 제어</p>
            <p className="mt-0.5 text-xs text-zinc-500">
              중복 발급(SISMEMBER) + 재고 차감(DECR) 단일 연산 처리
            </p>
          </div>
          <div className="rounded-xl border border-zinc-100 bg-zinc-50 p-4 dark:border-zinc-800 dark:bg-zinc-800/50">
            <span className="text-xs font-medium text-amber-600 dark:text-amber-400">
              Step 2 &middot; Apache Kafka
            </span>
            <p className="mt-1 font-semibold">비동기 이벤트 스트리밍</p>
            <p className="mt-0.5 text-xs text-zinc-500">
              coupon-issue-events 토픽 발행으로 DB 쓰기 병목 제거
            </p>
          </div>
          <div className="rounded-xl border border-zinc-100 bg-zinc-50 p-4 dark:border-zinc-800 dark:bg-zinc-800/50">
            <span className="text-xs font-medium text-emerald-600 dark:text-emerald-400">
              Step 3 &middot; MongoDB
            </span>
            <p className="mt-1 font-semibold">최종 영속화 &amp; 멱등성</p>
            <p className="mt-0.5 text-xs text-zinc-500">
              (couponId, userId) 복합 유니크 인덱스로 중복 저장 방지
            </p>
          </div>
        </div>
      </section>

      {/* 선착순 쿠폰 발급 메인 섹션 */}
      <section
        id="coupons"
        className="space-y-6 rounded-2xl border border-zinc-200 bg-white p-6 shadow-xs dark:border-zinc-800 dark:bg-zinc-900"
      >
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div>
            <h2 className="text-xl font-bold">
              선착순 쿠폰 발급 시스템 (Flash Coupon)
            </h2>
            <p className="mt-0.5 text-xs text-zinc-500 dark:text-zinc-400">
              쿠폰 캠페인을 생성한 뒤 단일 발급 또는 동시 다발(Burst) 요청을 실행해 초과 발급 및 중복 발급이 차단되는지 검증하세요.
            </p>
          </div>
          <button
            type="button"
            onClick={() => fetchCouponData()}
            disabled={loadingCoupons}
            className="rounded-lg border border-zinc-300 px-3 py-1.5 text-xs font-medium transition hover:bg-zinc-100 dark:border-zinc-700 dark:hover:bg-zinc-800"
          >
            {loadingCoupons ? "갱신 중..." : "현황 새로고침"}
          </button>
        </div>

        {couponError && (
          <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-xs text-red-700 dark:border-red-900/50 dark:bg-red-950/50 dark:text-red-300">
            {couponError}
          </div>
        )}

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          {/* 좌측: 1) 쿠폰 캠페인 생성 & 2) 발급 테스트 */}
          <div className="space-y-5">
            {/* 1. 캠페인 생성 */}
            <div className="rounded-xl border border-zinc-200 p-4 dark:border-zinc-800">
              <h3 className="text-sm font-semibold">
                1. 쿠폰 캠페인 생성 (Redis 재고 초기화)
              </h3>
              <div className="mt-3 grid grid-cols-1 gap-3 sm:grid-cols-3">
                <div>
                  <label className="block text-[11px] font-medium text-zinc-500">
                    Coupon ID
                  </label>
                  <input
                    type="number"
                    value={couponIdInput}
                    onChange={(e) => setCouponIdInput(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-zinc-300 bg-white px-3 py-1.5 text-sm outline-none focus:border-zinc-900 dark:border-zinc-700 dark:bg-zinc-950"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-medium text-zinc-500">
                    총 수량 (Stock)
                  </label>
                  <input
                    type="number"
                    value={couponTotalQtyInput}
                    onChange={(e) => setCouponTotalQtyInput(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-zinc-300 bg-white px-3 py-1.5 text-sm outline-none focus:border-zinc-900 dark:border-zinc-700 dark:bg-zinc-950"
                  />
                </div>
                <div className="sm:col-span-3">
                  <label className="block text-[11px] font-medium text-zinc-500">
                    쿠폰 이름
                  </label>
                  <div className="mt-1 flex gap-2">
                    <input
                      type="text"
                      value={couponTitleInput}
                      onChange={(e) => setCouponTitleInput(e.target.value)}
                      className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-1.5 text-sm outline-none focus:border-zinc-900 dark:border-zinc-700 dark:bg-zinc-950"
                    />
                    <button
                      type="button"
                      onClick={handleCreateCoupon}
                      disabled={creatingCoupon}
                      className="shrink-0 rounded-lg bg-zinc-900 px-4 py-1.5 text-xs font-medium text-white transition hover:bg-zinc-700 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900"
                    >
                      {creatingCoupon ? "생성 중..." : "생성 / 초기화"}
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {/* 2. 쿠폰 발급 요청 & 동시 요청 시뮬레이터 */}
            <div className="rounded-xl border border-zinc-200 p-4 dark:border-zinc-800">
              <h3 className="text-sm font-semibold">
                2. 선착순 쿠폰 발급 &amp; 동시성 시뮬레이션
              </h3>
              <div className="mt-3 grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-[11px] font-medium text-zinc-500">
                    대상 Coupon ID
                  </label>
                  <input
                    type="number"
                    value={issueCouponId}
                    onChange={(e) => setIssueCouponId(e.target.value)}
                    className="mt-1 w-full rounded-lg border border-zinc-300 bg-white px-3 py-1.5 text-sm outline-none focus:border-zinc-900 dark:border-zinc-700 dark:bg-zinc-950"
                  />
                </div>
                <div>
                  <label className="block text-[11px] font-medium text-zinc-500">
                    요청 User ID (단일 테스트)
                  </label>
                  <div className="mt-1 flex gap-2">
                    <input
                      type="number"
                      value={issueUserId}
                      onChange={(e) => setIssueUserId(e.target.value)}
                      className="w-full rounded-lg border border-zinc-300 bg-white px-3 py-1.5 text-sm outline-none focus:border-zinc-900 dark:border-zinc-700 dark:bg-zinc-950"
                    />
                    <button
                      type="button"
                      onClick={handleIssueSingle}
                      disabled={issuingCoupon}
                      className="shrink-0 rounded-lg bg-blue-600 px-3.5 py-1.5 text-xs font-medium text-white transition hover:bg-blue-500 disabled:opacity-50"
                    >
                      단일 발급
                    </button>
                  </div>
                </div>
              </div>

              <div className="mt-4 border-t border-zinc-100 pt-3 dark:border-zinc-800">
                <label className="block text-[11px] font-medium text-zinc-500">
                  동시 다발(Burst) 요청 시뮬레이션 (User ID 1 ~ N 동시 요청)
                </label>
                <div className="mt-1 flex gap-2">
                  <input
                    type="number"
                    value={burstCount}
                    onChange={(e) => setBurstCount(e.target.value)}
                    min={1}
                    max={100}
                    className="w-28 rounded-lg border border-zinc-300 bg-white px-3 py-1.5 text-sm outline-none focus:border-zinc-900 dark:border-zinc-700 dark:bg-zinc-950"
                  />
                  <button
                    type="button"
                    onClick={handleBurstSimulation}
                    disabled={issuingCoupon}
                    className="flex-1 rounded-lg bg-emerald-600 px-4 py-1.5 text-xs font-medium text-white transition hover:bg-emerald-500 disabled:opacity-50"
                  >
                    {issuingCoupon
                      ? "요청 처리 중..."
                      : `${burstCount}명 동시 선착순 발급 테스트`}
                  </button>
                </div>
              </div>
            </div>
          </div>

          {/* 우측: 실시간 Redis 잔여 수량 & MongoDB 비동기 발급 내역 */}
          <div className="space-y-4">
            {/* 쿠폰 잔여 수량 카드 */}
            <div className="rounded-xl border border-zinc-200 bg-zinc-50/60 p-4 dark:border-zinc-800 dark:bg-zinc-950">
              <p className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                활성 쿠폰 및 Redis 실시간 잔여 수량
              </p>
              {coupons.length === 0 ? (
                <p className="mt-2 text-xs text-zinc-400">
                  생성된 쿠폰이 없습니다. 좌측에서 쿠폰을 먼저 생성해 보세요.
                </p>
              ) : (
                <div className="mt-3 space-y-2.5">
                  {coupons.map((c) => {
                    const ratio =
                      c.totalQuantity > 0
                        ? Math.max(
                            0,
                            Math.min(
                              100,
                              Math.round(
                                (c.remainingQuantity / c.totalQuantity) * 100
                              )
                            )
                          )
                        : 0;
                    return (
                      <div
                        key={c.id}
                        className="rounded-lg border border-zinc-200 bg-white p-3 dark:border-zinc-800 dark:bg-zinc-900"
                      >
                        <div className="flex items-center justify-between text-xs">
                          <span className="font-semibold">
                            #{c.id} {c.title}
                          </span>
                          <span className="font-mono font-semibold text-emerald-600 dark:text-emerald-400">
                            잔여 {c.remainingQuantity} / {c.totalQuantity}
                          </span>
                        </div>
                        <div className="mt-2 h-2 w-full overflow-hidden rounded-full bg-zinc-200 dark:bg-zinc-800">
                          <div
                            className="h-full bg-emerald-500 transition-all"
                            style={{ width: `${ratio}%` }}
                          />
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* 최근 응답 로그 & MongoDB 영속화 내역 */}
            <div className="grid grid-cols-1 gap-3 sm:grid-cols-2">
              <div className="rounded-xl border border-zinc-200 bg-zinc-50/60 p-3.5 dark:border-zinc-800 dark:bg-zinc-950">
                <p className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Redis Lua 응답 결과 ({issueLogs.length}건)
                </p>
                <div className="mt-2 max-h-48 space-y-1.5 overflow-y-auto pr-1 text-xs">
                  {issueLogs.length === 0 ? (
                    <p className="text-zinc-400">발급 요청 기록 없음</p>
                  ) : (
                    issueLogs.map((log, i) => (
                      <div
                        key={`${log.userId}-${i}`}
                        className="flex items-center justify-between rounded border border-zinc-200/80 bg-white px-2.5 py-1.5 dark:border-zinc-800 dark:bg-zinc-900"
                      >
                        <span className="font-mono">User #{log.userId}</span>
                        <span
                          className={`rounded px-1.5 py-0.5 text-[10px] font-semibold ${statusBadgeClass(
                            log.status
                          )}`}
                        >
                          {log.status} (잔여:{log.remainingQuantity})
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </div>

              <div className="rounded-xl border border-zinc-200 bg-zinc-50/60 p-3.5 dark:border-zinc-800 dark:bg-zinc-950">
                <p className="text-xs font-semibold text-zinc-700 dark:text-zinc-300">
                  Kafka &rarr; MongoDB 적재 완료 ({issuedHistory.length}건)
                </p>
                <div className="mt-2 max-h-48 space-y-1.5 overflow-y-auto pr-1 text-xs">
                  {issuedHistory.length === 0 ? (
                    <p className="text-zinc-400">적재된 발급 내역 없음</p>
                  ) : (
                    issuedHistory.map((item) => (
                      <div
                        key={item.id}
                        className="flex items-center justify-between rounded border border-zinc-200/80 bg-white px-2.5 py-1.5 dark:border-zinc-800 dark:bg-zinc-900"
                      >
                        <span className="font-mono font-medium">
                          User #{item.userId}
                        </span>
                        <span className="font-mono text-[10px] text-zinc-400">
                          Coupon #{item.couponId}
                        </span>
                      </div>
                    ))
                  )}
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 기존 유저 관리 및 Kafka 기본 테스트 그리드 */}
      <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
        {/* 유저 조회 / 생성 카드 */}
        <section
          id="users"
          className="flex flex-col justify-between rounded-2xl border border-zinc-200 bg-white p-6 shadow-xs dark:border-zinc-800 dark:bg-zinc-900"
        >
          <div>
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">유저 관리 (User UseCase)</h2>
              <span className="rounded-md bg-blue-50 px-2.5 py-1 text-xs font-medium text-blue-700 dark:bg-blue-950 dark:text-blue-300">
                MongoDB
              </span>
            </div>
            <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
              ID를 입력하여 유저를 생성(
              <code className="font-mono">/users/new/&#123;id&#125;</code>)하거나
              조회(<code className="font-mono">/users/&#123;id&#125;</code>
              )합니다.
            </p>

            <div className="mt-5 space-y-3">
              <label className="block text-xs font-medium text-zinc-700 dark:text-zinc-300">
                User ID (Long)
              </label>
              <div className="flex gap-2">
                <input
                  type="number"
                  value={userIdInput}
                  onChange={(e) => setUserIdInput(e.target.value)}
                  placeholder="예: 1"
                  className="w-full rounded-lg border border-zinc-300 bg-white px-3.5 py-2 text-sm outline-none transition focus:border-zinc-900 dark:border-zinc-700 dark:bg-zinc-950 dark:focus:border-zinc-100"
                />
                <button
                  type="button"
                  onClick={handleGetUser}
                  disabled={loadingUser}
                  className="shrink-0 rounded-lg bg-zinc-900 px-4 py-2 text-sm font-medium text-white transition hover:bg-zinc-700 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
                >
                  조회
                </button>
                <button
                  type="button"
                  onClick={handleCreateUser}
                  disabled={loadingUser}
                  className="shrink-0 rounded-lg border border-zinc-300 bg-white px-4 py-2 text-sm font-medium text-zinc-800 transition hover:bg-zinc-100 disabled:opacity-50 dark:border-zinc-700 dark:bg-zinc-800 dark:text-zinc-100 dark:hover:bg-zinc-700"
                >
                  신규 생성
                </button>
              </div>
            </div>
          </div>

          {/* 결과 출력 박스 */}
          <div className="mt-6 rounded-xl border border-zinc-200/80 bg-zinc-50 p-4 dark:border-zinc-800 dark:bg-zinc-950">
            <p className="text-xs font-medium text-zinc-400">응답 결과</p>
            {loadingUser ? (
              <p className="mt-2 text-sm text-zinc-500">요청 처리 중...</p>
            ) : userError ? (
              <p className="mt-2 text-sm text-red-600 dark:text-red-400">
                {userError}
              </p>
            ) : userResult ? (
              <div className="mt-2 space-y-1 text-sm">
                <p>
                  <span className="font-medium text-zinc-500">ID:</span>{" "}
                  <span className="font-mono font-semibold">
                    {userResult.id}
                  </span>
                </p>
                <p>
                  <span className="font-medium text-zinc-500">Name:</span>{" "}
                  <span className="font-semibold">{userResult.name}</span>
                </p>
              </div>
            ) : (
              <p className="mt-2 text-sm text-zinc-400">
                아직 호출된 데이터가 없습니다.
              </p>
            )}
          </div>
        </section>

        {/* Kafka 컨트롤러 테스트 카드 */}
        <section
          id="kafka"
          className="flex flex-col justify-between rounded-2xl border border-zinc-200 bg-white p-6 shadow-xs dark:border-zinc-800 dark:bg-zinc-900"
        >
          <div>
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-semibold">Kafka 엔드포인트 테스트</h2>
              <span className="rounded-md bg-amber-50 px-2.5 py-1 text-xs font-medium text-amber-700 dark:bg-amber-950 dark:text-amber-300">
                KafkaController
              </span>
            </div>
            <p className="mt-1 text-xs text-zinc-500 dark:text-zinc-400">
              백엔드의 <code className="font-mono">GET /Kafka/get</code>{" "}
              엔드포인트 연결 상태를 확인합니다.
            </p>

            <div className="mt-5">
              <button
                type="button"
                onClick={handleTestKafka}
                disabled={loadingKafka}
                className="w-full rounded-lg bg-zinc-900 px-4 py-2.5 text-sm font-medium text-white transition hover:bg-zinc-700 disabled:opacity-50 dark:bg-zinc-100 dark:text-zinc-900 dark:hover:bg-zinc-300"
              >
                {loadingKafka ? "호출 중..." : "/Kafka/get 호출하기"}
              </button>
            </div>
          </div>

          <div className="mt-6 rounded-xl border border-zinc-200/80 bg-zinc-50 p-4 dark:border-zinc-800 dark:bg-zinc-950">
            <p className="text-xs font-medium text-zinc-400">응답 메시지</p>
            {kafkaResponse ? (
              <p className="mt-2 font-mono text-sm font-semibold text-emerald-600 dark:text-emerald-400">
                &quot;{kafkaResponse}&quot;
              </p>
            ) : (
              <p className="mt-2 text-sm text-zinc-400">
                버튼을 눌러 응답을 확인하세요.
              </p>
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
