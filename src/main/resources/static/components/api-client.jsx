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

    const json = await response.json();

    // 에러 응답 (4xx, 5xx)
    if (!response.ok && response.status !== 202) {
      const errMsg = json.error?.message || json.message || `HTTP ${response.status}`;
      const err = new Error(errMsg);
      err.status = response.status;
      err.code = json.error?.code;
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
    const sub = stompClient.subscribe(`/sub/live/${liveId}`, (message) => {
      try {
        const batch = JSON.parse(message.body);
        if (onBatch) onBatch(Array.isArray(batch) ? batch : [batch]);
      } catch (e) {
        console.error('Live chat parse error:', e);
      }
    });
    stompSubscriptions[`live:${liveId}`] = sub;
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
    const sub = stompClient.subscribe(`/sub/dm/${roomId}`, (message) => {
      try {
        const msg = JSON.parse(message.body);
        if (onMessage) onMessage(msg);
      } catch (e) {
        console.error('DM parse error:', e);
      }
    });
    stompSubscriptions[`dm:${roomId}`] = sub;
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
  };
})();

// 전역 노출 — 다른 JSX 컴포넌트에서 window.ConnectfinAPI.api(...) 로 접근
Object.assign(window, { ConnectfinAPI });
