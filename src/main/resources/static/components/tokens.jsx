// Connectfin — design tokens, shared placeholder components, data fixtures
// Two themes: "dark" (neon) and "y2k" (pop).

const THEMES = {
  dark: {
    name: 'Dark Neon',
    bg: '#0A0B10',
    bg2: '#12141C',
    surface: '#181A24',
    surface2: '#1F2230',
    line: 'rgba(255,255,255,0.08)',
    lineStrong: 'rgba(255,255,255,0.14)',
    text: '#F5F6FA',
    textDim: 'rgba(245,246,250,0.62)',
    textMuted: 'rgba(245,246,250,0.38)',
    accent: '#8B5CFF',     // violet
    accent2: '#22D3EE',    // cyan
    hot: '#FF3D7A',
    ok: '#34E4A8',
    chip: 'rgba(139,92,255,0.16)',
    gradient: 'linear-gradient(135deg, #8B5CFF 0%, #22D3EE 100%)',
    liveGradient: 'linear-gradient(135deg, #FF3D7A 0%, #FF8A3D 100%)',
    navBg: 'rgba(10,11,16,0.82)',
    font: '"Pretendard Variable", Pretendard, -apple-system, system-ui, sans-serif',
    fontDisplay: '"Space Grotesk", "Pretendard Variable", system-ui, sans-serif',
    fontMono: '"JetBrains Mono", ui-monospace, monospace',
  },
  y2k: {
    name: 'Y2K Pop',
    bg: '#FFE9F3',
    bg2: '#F3DEFF',
    surface: '#FFFFFF',
    surface2: '#FFF4FA',
    line: 'rgba(90,20,120,0.10)',
    lineStrong: 'rgba(90,20,120,0.22)',
    text: '#2A0E48',
    textDim: 'rgba(42,14,72,0.62)',
    textMuted: 'rgba(42,14,72,0.42)',
    accent: '#FF3DA1',     // hot pink
    accent2: '#7B2CFF',    // violet
    hot: '#FF5B2E',
    ok: '#2EEBC2',
    chip: 'rgba(255,61,161,0.14)',
    gradient: 'linear-gradient(135deg, #FF3DA1 0%, #7B2CFF 50%, #2EEBC2 100%)',
    liveGradient: 'linear-gradient(135deg, #FF3DA1 0%, #FF8A3D 100%)',
    navBg: 'rgba(255,233,243,0.85)',
    font: '"Space Grotesk", "Pretendard Variable", system-ui, sans-serif',
    fontDisplay: '"Space Grotesk", system-ui, sans-serif',
    fontMono: '"JetBrains Mono", ui-monospace, monospace',
  },
};

// Artists — multi-genre (K-pop, J-pop, rock, utaite, hip-hop, VTuber-adjacent)
const ARTISTS = [
  { id: 1, name: 'LUMEN8', stage: '루멘에잇', genre: 'K-POP · 걸그룹', color1: '#FF3DA1', color2: '#7B2CFF', members: 5, live: true, viewers: 142_839, followers: '2.4M' },
  { id: 2, name: 'Kagerō', stage: 'カゲロウ', genre: 'J-ROCK · 밴드', color1: '#0F4C81', color2: '#E63946', members: 4, live: false, viewers: 0, followers: '890K' },
  { id: 3, name: 'NOIR7', stage: '누아르세븐', genre: 'K-POP · 보이그룹', color1: '#111111', color2: '#D4AF37', members: 7, live: false, viewers: 0, followers: '3.1M' },
  { id: 4, name: 'hanabi*', stage: '하나비', genre: '우타이테 · 솔로', color1: '#FF8A3D', color2: '#FFD23D', members: 1, live: true, viewers: 58_241, followers: '412K' },
  { id: 5, name: 'Velvet Static', stage: '벨벳 스태틱', genre: 'INDIE ROCK · 밴드', color1: '#5B1D4E', color2: '#C41E3A', members: 4, live: false, viewers: 0, followers: '124K' },
  { id: 6, name: 'ORBITAL', stage: '오비탈', genre: 'HIPHOP · 크루', color1: '#1E1E1E', color2: '#22D3EE', members: 3, live: false, viewers: 0, followers: '678K' },
];

// Home feed posts
const FEED_POSTS = [
  { id: 101, artistId: 1, type: 'artist', author: 'YUNA', authorRole: 'LUMEN8', time: '방금 전', body: '오늘 라이브 올게🌙 궁금한 거 댓글로 남겨주면 답해줄게~!', likes: 48_291, comments: 3124, pinned: true },
  { id: 102, artistId: 4, type: 'artist', author: 'hanabi*', authorRole: 'hanabi*', time: '2분 전', body: '새 커버곡 녹음 중... 힌트 한 조각 📼', likes: 11_204, comments: 892 },
  { id: 103, artistId: 1, type: 'fan', author: '별빛모아', authorRole: 'FAN', time: '5분 전', body: '어제 팬미팅 영상 돌려보는 중... 진짜 잊을 수 없을 거 같아 🥹', likes: 1_842, comments: 214 },
  { id: 104, artistId: 3, type: 'artist', author: 'KAI', authorRole: 'NOIR7', time: '12분 전', body: '연습실. 오늘도 수고했다 세븐츠.', likes: 62_114, comments: 4891 },
  { id: 105, artistId: 2, type: 'fan', author: '螢火', authorRole: 'FAN', time: '18분 전', body: '어제 라이브 셋리스트 진짜 미쳤음... Encore에서 운 사람 나뿐?', likes: 488, comments: 93 },
];

// DM threads
const DM_THREADS = [
  { id: 1, artistId: 1, name: 'YUNA', group: 'LUMEN8', last: '오늘도 고마워 ⭐️', time: '방금', unread: 2, online: true, subscribed: true },
  { id: 2, artistId: 4, name: 'hanabi*', group: 'hanabi*', last: '[[name]]아 들어봤어?!', time: '3분', unread: 1, online: true, subscribed: true },
  { id: 3, artistId: 1, name: 'RIN', group: 'LUMEN8', last: '연습 끝났다~', time: '1시간', unread: 0, online: false, subscribed: true },
  { id: 4, artistId: 3, name: 'KAI', group: 'NOIR7', time: '어제', last: '구독 필요 · 15 젤리/월', unread: 0, online: false, subscribed: false },
];

// sample seed chat lines for live
const LIVE_SEEDS = [
  { u: '루미에르', m: '언니 목소리 오늘 진짜 좋다🫶', t: 'fan' },
  { u: 'starseed_02', m: '카메라 좀 더 가까이!!' },
  { u: '별빛모아', m: 'YUNAAAAAA' },
  { u: 'moonlit', m: '다음곡 "Prism" 해줘요 제발' },
  { u: 'YUNA', m: '다들 저녁 먹었어?', t: 'artist' },
  { u: 'cometkid', m: 'ㅠㅠㅠㅠㅠㅠ 사랑해요' },
  { u: 'drift_ko', m: '지금 들어옴 놓친거 많음??' },
  { u: '푸른새벽', m: 'bgm 볼륨 ↑' },
  { u: 'velvetfan', m: '오늘 의상 역대급' },
  { u: 'utanoko', m: 'ハナビちゃん 応援してる〜' },
  { u: 'mika_j', m: '오빠 생일 축하해!!' },
  { u: 'neonpop', m: '🩵🩵🩵🩵' },
  { u: 'LUMINARY', m: '실화냐;; 머리 너무 예뻐' },
  { u: 'rainpeach', m: '첫 라이브인데 이게 뭐람... 너무 좋음' },
];

// Media gallery
const MEDIA = [
  { id: 1, artistId: 1, kind: 'video', title: '[MV] LUMEN8 — Prism (Official)', duration: '3:42', views: '14M', daysAgo: 7 },
  { id: 2, artistId: 1, kind: 'photo', title: '백스테이지 컷 #42', daysAgo: 1 },
  { id: 3, artistId: 4, kind: 'video', title: 'hanabi* covers「アイドル」', duration: '4:18', views: '2.1M', daysAgo: 3 },
  { id: 4, artistId: 3, kind: 'video', title: 'NOIR7 Dance Practice — MIRROR', duration: '5:02', views: '8.4M', daysAgo: 14 },
  { id: 5, artistId: 2, kind: 'photo', title: 'Tour Final — Osaka', daysAgo: 21 },
  { id: 6, artistId: 5, kind: 'video', title: 'Velvet Static — Live at CLUB FF', duration: '3:12', views: '48K', daysAgo: 30 },
];

// ————————————— Shared UI primitives —————————————

// Artist avatar — no SVG portraits, just layered gradient placeholders
function ArtistAvatar({ artist, size = 44, live = false, ring = false, t }) {
  const s = size;
  const stripe = `repeating-linear-gradient(135deg, ${artist.color1} 0 10px, ${artist.color2} 10px 20px)`;
  return (
    <div style={{ position: 'relative', width: s, height: s, flexShrink: 0 }}>
      {ring && (
        <div style={{
          position: 'absolute', inset: -3, borderRadius: '50%',
          background: live ? t.liveGradient : t.gradient,
          padding: 2,
        }}>
          <div style={{ width: '100%', height: '100%', borderRadius: '50%', background: t.bg }} />
        </div>
      )}
      <div style={{
        position: 'absolute', inset: ring ? 1 : 0,
        borderRadius: '50%', overflow: 'hidden',
        background: stripe,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontFamily: t.fontDisplay, fontWeight: 800,
        color: '#fff', fontSize: s * 0.32,
        letterSpacing: -0.5,
        boxShadow: ring ? 'none' : `0 0 0 1px ${t.line} inset`,
      }}>
        {artist.name.slice(0, 2).toUpperCase()}
      </div>
      {live && !ring && (
        <div style={{
          position: 'absolute', bottom: -2, right: -2,
          width: 14, height: 14, borderRadius: '50%',
          background: t.hot, border: `2px solid ${t.bg}`,
          boxShadow: `0 0 10px ${t.hot}`,
        }} />
      )}
    </div>
  );
}

// Media placeholder — striped block with mono label
function MediaPlaceholder({ artist, label, aspect = '16/9', t, kind = 'video', radius = 18 }) {
  return (
    <div style={{
      aspectRatio: aspect, width: '100%',
      borderRadius: radius, overflow: 'hidden', position: 'relative',
      background: `repeating-linear-gradient(45deg, ${artist.color1} 0 14px, ${artist.color2} 14px 28px)`,
    }}>
      <div style={{
        position: 'absolute', inset: 0,
        background: `linear-gradient(180deg, rgba(0,0,0,0) 40%, rgba(0,0,0,0.55) 100%)`,
      }} />
      <div style={{
        position: 'absolute', top: 10, left: 10,
        fontFamily: t.fontMono, fontSize: 10, letterSpacing: 0.5,
        padding: '4px 8px', borderRadius: 6,
        background: 'rgba(0,0,0,0.45)', color: '#fff',
        textTransform: 'uppercase',
      }}>{kind} · {artist.name}</div>
      <div style={{
        position: 'absolute', bottom: 10, left: 12, right: 12,
        color: '#fff', fontFamily: t.fontDisplay, fontWeight: 700,
        fontSize: 14, textShadow: '0 1px 4px rgba(0,0,0,0.5)',
      }}>{label}</div>
      {kind === 'video' && (
        <div style={{
          position: 'absolute', top: '50%', left: '50%',
          transform: 'translate(-50%, -50%)',
          width: 48, height: 48, borderRadius: '50%',
          background: 'rgba(255,255,255,0.92)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <div style={{
            width: 0, height: 0, marginLeft: 3,
            borderTop: '9px solid transparent',
            borderBottom: '9px solid transparent',
            borderLeft: '14px solid #111',
          }} />
        </div>
      )}
    </div>
  );
}

// Tab bar (5 icons, bottom)
function TabBar({ current, onNav, t, theme }) {
  const tabs = [
    { k: 'home', label: '홈', icon: '⌂' },
    { k: 'live', label: '라이브', icon: '●' },
    { k: 'media', label: '미디어', icon: '▤' },
    { k: 'dm', label: 'DM', icon: '✉' },
    { k: 'me', label: '마이', icon: '◐' },
  ];
  return (
    <div style={{
      position: 'absolute', bottom: 0, left: 0, right: 0, zIndex: 40,
      paddingBottom: 28, paddingTop: 8, paddingLeft: 8, paddingRight: 8,
      background: t.navBg,
      backdropFilter: 'blur(24px) saturate(180%)',
      WebkitBackdropFilter: 'blur(24px) saturate(180%)',
      borderTop: `1px solid ${t.line}`,
      display: 'flex', justifyContent: 'space-around',
    }}>
      {tabs.map(tab => {
        const active = current === tab.k;
        return (
          <button key={tab.k} onClick={() => onNav(tab.k)} style={{
            flex: 1, background: 'transparent', border: 'none',
            padding: '6px 0', cursor: 'pointer',
            display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3,
            fontFamily: t.font,
          }}>
            <div style={{
              fontSize: tab.k === 'live' ? 10 : 22,
              color: active ? t.text : t.textMuted,
              lineHeight: 1,
              display: 'flex', alignItems: 'center', gap: 4,
            }}>
              {tab.k === 'live' ? (
                <span style={{
                  width: 8, height: 8, borderRadius: '50%',
                  background: active ? t.hot : t.textMuted,
                  boxShadow: active ? `0 0 8px ${t.hot}` : 'none',
                }} />
              ) : tab.icon}
              {tab.k === 'live' && <span style={{ fontSize: 10, fontWeight: 700 }}>LIVE</span>}
            </div>
            <span style={{
              fontSize: 10, fontWeight: active ? 700 : 500,
              color: active ? t.text : t.textMuted,
              letterSpacing: -0.2,
            }}>{tab.label}</span>
          </button>
        );
      })}
    </div>
  );
}

// Status bar (light/dark aware)
function StatusBar({ dark, t }) {
  const c = dark ? '#fff' : t.text;
  return (
    <div style={{
      position: 'absolute', top: 0, left: 0, right: 0, zIndex: 50,
      height: 54, display: 'flex', alignItems: 'center',
      padding: '12px 28px 0', justifyContent: 'space-between',
      pointerEvents: 'none',
    }}>
      <span style={{
        fontFamily: '-apple-system, system-ui', fontWeight: 700,
        fontSize: 16, color: c,
      }}>9:41</span>
      <div style={{ display: 'flex', gap: 6, alignItems: 'center' }}>
        <svg width="17" height="11" viewBox="0 0 17 11"><rect x="0" y="6" width="3" height="4" rx="0.7" fill={c}/><rect x="4.5" y="4" width="3" height="6" rx="0.7" fill={c}/><rect x="9" y="2" width="3" height="8" rx="0.7" fill={c}/><rect x="13.5" y="0" width="3" height="10" rx="0.7" fill={c}/></svg>
        <svg width="15" height="11" viewBox="0 0 15 11"><path d="M7.5 3C9.5 3 11.3 3.8 12.6 5.1L13.6 4.1C12 2.5 9.9 1.5 7.5 1.5C5.1 1.5 3 2.5 1.4 4.1L2.4 5.1C3.7 3.8 5.5 3 7.5 3Z" fill={c}/><circle cx="7.5" cy="9" r="1.3" fill={c}/></svg>
        <div style={{
          width: 24, height: 11, border: `1px solid ${c}`, borderRadius: 3, position: 'relative',
          opacity: 0.85,
        }}>
          <div style={{ position: 'absolute', inset: 1, width: '72%', background: c, borderRadius: 1 }}/>
          <div style={{ position: 'absolute', right: -3, top: 3, width: 2, height: 4, background: c, borderRadius: 1 }}/>
        </div>
      </div>
    </div>
  );
}

// Dynamic-island-style top for dark phone
function DynamicIsland() {
  return (
    <div style={{
      position: 'absolute', top: 10, left: '50%', transform: 'translateX(-50%)',
      width: 118, height: 34, borderRadius: 22, background: '#000', zIndex: 60,
    }}/>
  );
}

// Phone shell
function Phone({ children, theme, t, width = 390, height = 844 }) {
  return (
    <div style={{
      width, height, borderRadius: 54, overflow: 'hidden',
      position: 'relative', background: t.bg,
      boxShadow: '0 50px 120px rgba(0,0,0,0.45), 0 0 0 10px #111, 0 0 0 11px #333',
      color: t.text, fontFamily: t.font,
    }}>
      <DynamicIsland/>
      <StatusBar dark={theme === 'dark'} t={t}/>
      {children}
      <div style={{
        position: 'absolute', bottom: 8, left: 0, right: 0,
        display: 'flex', justifyContent: 'center', zIndex: 80, pointerEvents: 'none',
      }}>
        <div style={{
          width: 134, height: 5, borderRadius: 100,
          background: theme === 'dark' ? 'rgba(255,255,255,0.6)' : 'rgba(0,0,0,0.3)',
        }}/>
      </div>
    </div>
  );
}

// Infinite logo mark (stylised ∞)
function Infinity8({ size = 28, color, color2, stroke = 6 }) {
  return (
    <svg width={size} height={size * 0.55} viewBox="0 0 100 56" fill="none">
      <defs>
        <linearGradient id={`infg-${color.replace('#','')}`} x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={color}/>
          <stop offset="100%" stopColor={color2 || color}/>
        </linearGradient>
      </defs>
      <path d="M 28 28 C 28 10, 8 10, 8 28 S 28 46, 28 28 C 28 10, 72 46, 72 28 S 92 10, 92 28 S 72 46, 72 28 C 72 10, 28 46, 28 28 Z"
        stroke={`url(#infg-${color.replace('#','')})`} strokeWidth={stroke} strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

Object.assign(window, {
  THEMES, ARTISTS, FEED_POSTS, DM_THREADS, LIVE_SEEDS, MEDIA,
  ArtistAvatar, MediaPlaceholder, TabBar, StatusBar, DynamicIsland, Phone, Infinity8,
});
