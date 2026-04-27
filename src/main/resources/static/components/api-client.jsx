// ═══════════════════════════════════════════════════════════
// Connectfin API Client
// Spring Boot 내장 서빙 (같은 origin) 전용. CORS 불필요.
// ═══════════════════════════════════════════════════════════

const ConnectfinAPI = (() => {
  const TOKEN_KEY = 'connectfin_token';

  // ── 토큰 관리 ──
  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
  }

  function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
  }

  // ── 공통 헤더 생성 ──
  function authHeaders() {
    const token = getToken();
    const headers = {};
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return headers;
  }

  // ── JSON API 호출 ──
  // 응답 분기:
  //   - ApiResponse 래퍼 (success 필드 있음) → data 반환
  //   - PageResponse 등 직접 반환 (success 필드 없음) → 응답 전체 반환
  //   - 202 Accepted → 정상 (비동기 API: ArtistPost v3 좋아요, v2 댓글)
  async function api(path, options = {}) {
    const { method = 'GET', body, headers: extraHeaders = {} } = options;

    const headers = {
      ...authHeaders(),
      ...extraHeaders,
    };

    // GET이 아니고 body가 string이면 JSON
    if (body && typeof body === 'string') {
      headers['Content-Type'] = 'application/json';
    }

    const response = await fetch(path, {
      method,
      headers,
      body: body || undefined,
    });

    // 204 No Content
    if (response.status === 204) return null;

    // 응답을 텍스트로 먼저 읽고 → JSON 파싱 시도
    // (JSON이 아니거나 깨진 경우 raw 텍스트를 에러 메시지에 포함시켜 디버깅 용이)
    const rawText = await response.text();
    let json;
    try {
      json = rawText ? JSON.parse(rawText) : null;
    } catch (parseErr) {
      console.error(`[ConnectfinAPI] JSON parse failed for ${path}:`, {
        status: response.status,
        contentType: response.headers.get('content-type'),
        rawText: rawText.slice(0, 500),
      });
      const err = new Error(
        `서버 응답을 JSON으로 파싱할 수 없습니다 (HTTP ${response.status}): ${rawText.slice(0, 200)}`
      );
      err.status = response.status;
      err.rawText = rawText;
      throw err;
    }

    // 에러 응답 (4xx, 5xx)
    if (!response.ok && response.status !== 202) {
      const errMsg = json?.error?.message || json?.message || `HTTP ${response.status}`;
      const err = new Error(errMsg);
      err.status = response.status;
      err.code = json?.error?.code;
      err.response = json;
      throw err;
    }

    // ApiResponse 래퍼 판별
    if (json.success !== undefined) {
      if (!json.success) {
        const errMsg = json.error?.message || 'Unknown error';
        const err = new Error(errMsg);
        err.code = json.error?.code;
        err.response = json;
        throw err;
      }
      return json.data;
    }

    // PageResponse 등 래퍼 없는 직접 반환
    return json;
  }

  // ── Multipart API 호출 ──
  // Content-Type을 설정하지 않는다 — 브라우저가 boundary를 자동 생성해야 한다.
  // FormData 객체를 그대로 body로 전달한다.
  async function apiMultipart(path, formData, method = 'POST') {
    const response = await fetch(path, {
      method,
      headers: authHeaders(),  // Content-Type 의도적 미설정
      body: formData,
    });

    if (response.status === 204) return null;

    const json = await response.json();

    if (!response.ok) {
      const errMsg = json.error?.message || json.message || `HTTP ${response.status}`;
      const err = new Error(errMsg);
      err.status = response.status;
      err.code = json.error?.code;
      err.response = json;
      throw err;
    }

    if (json.success !== undefined) {
      if (!json.success) {
        const errMsg = json.error?.message || 'Unknown error';
        const err = new Error(errMsg);
        err.code = json.error?.code;
        err.response = json;
        throw err;
      }
      return json.data;
    }

    return json;
  }

  // ── 포맷 유틸 ──

  // ISO 날짜 → 상대 시간 또는 "MM.DD. HH:mm"
  function formatTime(isoString) {
    if (!isoString) return '';
    const date = new Date(isoString);
    const now = new Date();
    const diffMs = now - date;
    const diffMin = Math.floor(diffMs / 60000);
    const diffHour = Math.floor(diffMs / 3600000);
    const diffDay = Math.floor(diffMs / 86400000);

    if (diffMin < 1) return '방금';
    if (diffMin < 60) return `${diffMin}분 전`;
    if (diffHour < 24) return `${diffHour}h`;
    if (diffDay < 7) return `${diffDay}일 전`;

    const mm = String(date.getMonth() + 1).padStart(2, '0');
    const dd = String(date.getDate()).padStart(2, '0');
    const hh = String(date.getHours()).padStart(2, '0');
    const mi = String(date.getMinutes()).padStart(2, '0');
    return `${mm}.${dd}. ${hh}:${mi}`;
  }

  // 숫자 → "1.3K", "42K"
  function formatCount(n) {
    if (n == null) return '0';
    if (n < 1000) return String(n);
    if (n < 10000) return (n / 1000).toFixed(1).replace(/\.0$/, '') + 'K';
    if (n < 1000000) return Math.floor(n / 1000) + 'K';
    return (n / 1000000).toFixed(1).replace(/\.0$/, '') + 'M';
  }

  // ── STOMP ──
  let stompClient = null;
  let stompSubscriptions = {};

  function connectStomp(onConnected, onError) {
    // 이미 연결돼 있으면 바로 콜백 (중복 연결 방지)
    if (stompClient && stompClient.connected) {
      if (onConnected) onConnected();
      return stompClient;
    }

    const token = getToken();
    if (!token) {
      if (onError) onError(new Error('No token'));
      return;
    }

    // @stomp/stompjs 7.x UMD → 전역 StompJs 객체
    const client = new StompJs.Client({
      webSocketFactory: () => new SockJS('/ws-stomp'),
      connectHeaders: {
        'Authorization': 'Bearer ' + token,
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
    });

    client.onConnect = () => {
      stompClient = client;
      if (onConnected) onConnected();
    };

    client.onStompError = (frame) => {
      console.error('STOMP error:', frame.headers?.message);
      if (onError) onError(new Error(frame.headers?.message || 'STOMP error'));
    };

    client.onWebSocketClose = () => {
      // reconnectDelay가 설정돼 있으므로 자동 재연결 시도
    };

    client.activate();
    return client;
  }

  // 라이브 채팅 구독 — 서버가 300ms마다 배치(List) 전송
  function subscribeLive(liveId, onBatch) {
    if (!stompClient || !stompClient.connected) return null;
    const subKey = `live:${liveId}`;
    if (stompSubscriptions[subKey]) stompSubscriptions[subKey].unsubscribe();
    const sub = stompClient.subscribe(`/sub/live/${liveId}`, (message) => {
      try {
        const batch = JSON.parse(message.body);
        if (onBatch) onBatch(Array.isArray(batch) ? batch : [batch]);
      } catch (e) {
        console.error('Live chat parse error:', e);
      }
    });
    stompSubscriptions[subKey] = sub;
    return sub;
  }

  // 라이브 채팅 전송 — payload는 String (메시지 텍스트만)
  function sendLiveChat(liveId, message) {
    if (!stompClient || !stompClient.connected) return;
    stompClient.publish({
      destination: `/pub/live/${liveId}/chat`,
      body: message,
    });
  }

  // DM 구독 — 단건 메시지 수신
  function subscribeDm(roomId, onMessage) {
    if (!stompClient || !stompClient.connected) return null;
    const subKey = `dm:${roomId}`;
    if (stompSubscriptions[subKey]) stompSubscriptions[subKey].unsubscribe();
    const sub = stompClient.subscribe(`/sub/dm/${roomId}`, (message) => {
      try {
        const msg = JSON.parse(message.body);
        if (onMessage) onMessage(msg);
      } catch (e) {
        console.error('DM parse error:', e);
      }
    });
    stompSubscriptions[subKey] = sub;
    return sub;
  }

  // DM 전송 — payload는 String (메시지 텍스트만)
  function sendDm(roomId, message) {
    if (!stompClient || !stompClient.connected) return;
    stompClient.publish({
      destination: `/pub/dm/${roomId}`,
      body: message,
    });
  }

  function unsubscribe(key) {
    if (stompSubscriptions[key]) {
      stompSubscriptions[key].unsubscribe();
      delete stompSubscriptions[key];
    }
  }

  function disconnectStomp() {
    Object.keys(stompSubscriptions).forEach(key => {
      stompSubscriptions[key].unsubscribe();
    });
    stompSubscriptions = {};
    if (stompClient) {
      stompClient.deactivate();
      stompClient = null;
    }
  }

  // ── Public API ──
  return {
    getToken,
    setToken,
    clearToken,
    api,
    apiMultipart,
    formatTime,
    formatCount,
    // STOMP
    connectStomp,
    subscribeLive,
    sendLiveChat,
    subscribeDm,
    sendDm,
    unsubscribe,
    disconnectStomp,
    // Artist store sync
    loadArtists,
  };
})();

// ──────────────────────────────────────────────────────────────
// 백엔드 ArtistSearchResponse → 프론트 ARTISTS 항목 변환
// (이름·이미지만 백엔드 값으로 덮고, 색·장르 같은 visual prop은 mock 값을 보존)
// ──────────────────────────────────────────────────────────────
function mergeArtistFromBackend(backend, existing) {
  return {
    // 기본 visual mock (없으면 deterministic 색상 생성)
    color1: existing?.color1 || hslFromId(backend.id, 60),
    color2: existing?.color2 || hslFromId(backend.id, 80),
    genre: existing?.genre || 'ARTIST',
    members: existing?.members || 1,
    live: existing?.live || false,
    viewers: existing?.viewers || 0,
    followers: existing?.followers || '—',
    stage: existing?.stage || backend.name,
    // 백엔드 우선 필드
    id: backend.id,
    name: backend.name,
    slug: backend.slug,
    profileImageUrl: backend.profileImageUrl,
    fromBackend: true,
  };
}

function hslFromId(id, lightness = 60) {
  const hue = (Number(id) * 47) % 360;
  return `hsl(${hue}, 70%, ${lightness}%)`;
}

// 백엔드의 모든 아티스트를 가져와 window.ARTISTS에 머지한다.
// - 이미 있는 mock 아티스트는 visual prop을 보존
// - 백엔드에만 있는 신규 아티스트는 새로 push
// - mock에만 있고 백엔드에 없는 아티스트는 dev 모드에서 살려둠 (백엔드 미구동 시 화면 보존)
async function loadArtists() {
  try {
    // v3 검색을 keyword 없이 호출 — 전체 아티스트 페이징
    let allBackend = [];
    let page = 1;
    while (page <= 5) { // 안전장치: 최대 50개
      const res = await ConnectfinAPI.api(`/api/member/v3/artists/search?page=${page}`);
      const list = res?.content || [];
      if (list.length === 0) break;
      allBackend = allBackend.concat(list);
      if (!res.hasNext) break;
      page += 1;
    }
    if (!Array.isArray(window.ARTISTS)) return;

    // 머지: 백엔드 id 기준
    const merged = [];
    const seenIds = new Set();
    for (const b of allBackend) {
      const existing = window.ARTISTS.find(a => a.id === b.id);
      merged.push(mergeArtistFromBackend(b, existing));
      seenIds.add(b.id);
    }
    // 백엔드에 없는 mock은 뒤에 보존 (오프라인 데모 호환)
    for (const a of window.ARTISTS) {
      if (!seenIds.has(a.id)) merged.push(a);
    }

    // 배열 mutate (다른 모듈이 같은 참조를 들고 있을 수 있으므로)
    window.ARTISTS.length = 0;
    window.ARTISTS.push(...merged);

    // 갱신 알림
    window.dispatchEvent(new CustomEvent('connectfin:artists-changed', { detail: merged }));
    console.log(`[Connectfin] ARTISTS 동기화: 백엔드 ${allBackend.length} + mock 보존 ${merged.length - allBackend.length} = ${merged.length}`);
  } catch (err) {
    console.warn('[Connectfin] loadArtists 실패 (mock으로 동작):', err?.message || err);
  }
}

// 전역 노출 — 다른 JSX 컴포넌트에서 window.ConnectfinAPI.api(...) 로 접근
Object.assign(window, { ConnectfinAPI });
