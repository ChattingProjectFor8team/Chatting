// Connectfin — Desktop global screens: Home, Jelly Shop, Shop

// ──────────────── Home ────────────────
function DesktopHome({ t, theme, onArtistOpen, onOpenLive, onNavGlobal }) {
  const [slideIdx, setSlideIdx] = React.useState(0);
  const [liveNow, setLiveNow] = React.useState(ON_LIVE_NOW);
  React.useEffect(() => {
    const id = setInterval(() => setSlideIdx(i => (i + 1) % HERO_SLIDES.length), 4500);
    return () => clearInterval(id);
  }, []);

  React.useEffect(() => {
    // 모든 아티스트의 라이브 목록에서 LIVE 상태만 필터
    Promise.all(
      ARTISTS.map(a =>
        window.ConnectfinAPI.api(`/api/v1/artists/${a.id}/lives`)
          .then(data => (data || []).filter(l => l.liveStatus === 'LIVE').map(l => ({
            ...l, artistId: a.id, artistName: a.name, artistColor1: a.color1, artistColor2: a.color2,
          })))
          .catch(() => [])
      )
    ).then(results => {
      const allLive = results.flat();
      if (allLive.length > 0) {
        setLiveNow(allLive.map(l => ({
          id: l.id, artistId: l.artistId, title: l.title,
          host: l.hostDisplayName || l.artistName,
          viewers: 0, // API에 viewerCount 없음
          artistName: l.artistName, color1: l.artistColor1, color2: l.artistColor2,
          status: 'LIVE',
        })));
      }
    });
  }, []);

  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: '24px 24px 60px' }}>
      {/* Hero carousel (2-up) */}
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16, marginBottom: 14 }}>
        {HERO_SLIDES.slice(0, 2).map(s => <HeroCard key={s.id} slide={s} t={t}/>)}
      </div>

      {/* Pagination dots */}
      <div style={{ display: 'flex', justifyContent: 'center', gap: 6, marginBottom: 24 }}>
        {HERO_SLIDES.map((_, i) => (
          <div key={i} style={{
            width: i === slideIdx ? 18 : 6, height: 6, borderRadius: 3,
            background: i === slideIdx ? t.text : t.textMuted, transition: 'all 200ms',
          }}/>
        ))}
      </div>

      {/* Find artists bar */}
      <div style={{
        padding: '14px 20px', borderRadius: 12, border: `1px solid ${t.line}`,
        background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface,
        display: 'flex', alignItems: 'center', gap: 14, marginBottom: 20, cursor: 'pointer',
      }}>
        <div style={{
          width: 48, height: 48, borderRadius: '50%',
          background: theme === 'dark' ? '#0A0B10' : '#F3DEFF',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 22, color: t.textDim,
        }}>+</div>
        <span style={{ fontSize: 14, color: t.textDim }}>내가 좋아하는 아티스트를 찾아보세요!</span>
      </div>

      {/* On LIVE */}
      <div style={{
        padding: 20, borderRadius: 12, border: `1px solid ${t.line}`,
        background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface, marginBottom: 20,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
          <div style={{ fontWeight: 800, fontSize: 15 }}>On LIVE</div>
          <div style={{
            padding: '1px 8px', borderRadius: 10, background: t.hot,
            color: '#fff', fontSize: 11, fontWeight: 800,
          }}>{liveNow.length}</div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 14 }}>
          {liveNow.map(live => {
            const a = ARTISTS.find(x => x.id === live.artistId);
            return <OnLiveCard key={live.id} live={live} artist={a} t={t} onOpen={() => onOpenLive(live.artistId)}/>;
          })}
        </div>
      </div>

      {/* DM pool */}
      <div style={{
        padding: 20, borderRadius: 12, border: `1px solid ${t.line}`,
        background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface,
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <div style={{ fontWeight: 800, fontSize: 15 }}>아티스트와 DM을 나눠 보세요!</div>
          <span style={{ fontSize: 12, color: t.textDim, cursor: 'pointer' }}>↻</span>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 10 }}>
          {ARTISTS.slice(0, 6).map(a => (
            <div key={a.id} onClick={() => onArtistOpen(a.id)} style={{
              padding: '10px 12px', borderRadius: 24, border: `1px solid ${t.line}`,
              display: 'flex', alignItems: 'center', gap: 8, cursor: 'pointer',
              background: theme === 'dark' ? 'rgba(0,0,0,0.3)' : 'rgba(255,255,255,0.7)',
            }}>
              <ArtistAvatar artist={a} size={28} t={t}/>
              <span style={{ fontSize: 13, fontWeight: 700, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{a.name}</span>
            </div>
          ))}
          <div style={{
            padding: '10px 12px', borderRadius: 24, border: `1px solid ${t.line}`,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, cursor: 'pointer',
            fontSize: 12, color: t.textDim, fontFamily: t.fontMono,
          }}>전체보기 →</div>
        </div>
      </div>
    </div>
  );
}

function HeroCard({ slide, t }) {
  return (
    <div style={{
      aspectRatio: '16/9', borderRadius: 14, overflow: 'hidden', position: 'relative',
      background: `linear-gradient(135deg, ${slide.color1}, ${slide.color2})`,
      cursor: 'pointer',
    }}>
      <div style={{ position: 'absolute', inset: 0, background: 'linear-gradient(45deg, rgba(0,0,0,0.15) 0%, transparent 60%)' }}/>
      <div style={{
        position: 'absolute', top: 20, left: 20, color: slide.textDark ? '#111' : '#fff',
      }}>
        <div style={{ fontFamily: t.fontMono, fontSize: 10, fontWeight: 800, letterSpacing: 1, opacity: 0.85, marginBottom: 4 }}>{slide.artist}</div>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 26, fontWeight: 900, letterSpacing: -0.8, lineHeight: 1.1, whiteSpace: 'pre-line' }}>{slide.title}</div>
        <div style={{ fontSize: 12, marginTop: 8, opacity: 0.8 }}>{slide.body}</div>
      </div>
      <div style={{
        position: 'absolute', bottom: -20, right: -20, width: 140, height: 140, borderRadius: 24,
        background: slide.textDark ? 'rgba(255,255,255,0.4)' : 'rgba(255,255,255,0.15)',
        transform: 'rotate(12deg)',
      }}/>
      <div style={{
        position: 'absolute', bottom: 30, right: 30,
        fontFamily: 'ui-monospace, monospace', fontSize: 9, color: slide.textDark ? '#111' : '#fff',
        opacity: 0.6, letterSpacing: 0.5,
      }}>[{slide.kind}]</div>
    </div>
  );
}

function OnLiveCard({ live, artist, t, onOpen }) {
  return (
    <div onClick={onOpen} style={{ cursor: 'pointer' }}>
      <div style={{
        position: 'relative', aspectRatio: '16/10', borderRadius: 10, overflow: 'hidden',
        background: live.kind === 'voice'
          ? `linear-gradient(135deg, ${artist.color1}, ${artist.color2})`
          : `repeating-linear-gradient(45deg, ${artist.color1} 0 22px, ${artist.color2} 22px 44px)`,
      }}>
        <div style={{
          position: 'absolute', top: 10, left: 10, padding: '3px 8px', borderRadius: 5,
          background: live.status === 'LIVE' ? t.hot : t.accent,
          color: '#fff', fontSize: 10, fontWeight: 800, display: 'flex', alignItems: 'center', gap: 4,
        }}>
          {live.status === 'LIVE' && <span style={{ width: 5, height: 5, borderRadius: '50%', background: '#fff' }}/>}
          {live.status}
        </div>
        {live.kind === 'voice' && (
          <div style={{
            position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: 'rgba(255,255,255,0.9)', fontSize: 32,
          }}>{live.group}</div>
        )}
      </div>
      <div style={{ marginTop: 10, fontWeight: 700, fontSize: 14 }}>{live.title}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 4, fontSize: 11, color: t.textDim }}>
        <ArtistAvatar artist={artist} size={16} t={t}/>
        {live.host}
        <span>·</span>
        <span>{live.group}</span>
      </div>
    </div>
  );
}

// ──────────────── Jelly Shop ────────────────
function DesktopJellyShop({ t, theme }) {
  const [tab, setTab] = React.useState('general');

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '260px 1fr', maxWidth: 1200, margin: '0 auto', padding: '24px 24px 60px', gap: 32 }}>
      {/* Left summary */}
      <div>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 22, fontWeight: 800, letterSpacing: -0.4, marginBottom: 20 }}>Jelly Shop</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
          <span style={{ fontSize: 24 }}>🍮</span>
          <span style={{ fontSize: 14, color: t.textDim }}>My Jelly</span>
        </div>
        <div style={{ fontFamily: t.fontDisplay, fontSize: 48, fontWeight: 800, letterSpacing: -1, marginBottom: 28 }}>142</div>

        {[
          { label: '충전젤리', value: '120' },
          { label: '적립젤리', value: '22' },
        ].map(row => (
          <div key={row.label} style={{
            display: 'flex', justifyContent: 'space-between', padding: '12px 0',
            borderBottom: `1px solid ${t.line}`, fontSize: 13,
          }}>
            <span style={{ color: t.textDim }}>{row.label}</span>
            <span style={{ fontWeight: 700 }}>{row.value}</span>
          </div>
        ))}

        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '14px 0' }}>
          <span style={{ color: t.textDim, fontSize: 13 }}>충전한도</span>
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontFamily: t.fontDisplay, fontSize: 20, fontWeight: 800 }}>1,500</div>
            <div style={{ fontSize: 10, color: t.textDim, fontFamily: t.fontMono }}>최대 1,500</div>
          </div>
        </div>

        <div style={{
          marginTop: 40, paddingTop: 20, borderTop: `1px solid ${t.line}`,
          fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, letterSpacing: 0.5,
        }}>
          CONNECTFIN
          <div style={{ fontSize: 9, marginTop: 2 }}>© 2026 Connectfin Inc.</div>
        </div>
      </div>

      {/* Right content */}
      <div>
        <div style={{ display: 'flex', gap: 24, borderBottom: `1px solid ${t.line}`, marginBottom: 24 }}>
          {[
            { k: 'intro', label: '소개' },
            { k: 'general', label: '일반 충전' },
            { k: 'auto', label: '자동 충전' },
            { k: 'history', label: '조회' },
            { k: 'faq', label: 'FAQ' },
          ].map(x => (
            <button key={x.k} onClick={() => setTab(x.k)} style={{
              padding: '12px 0', background: 'transparent', border: 'none',
              borderBottom: tab === x.k ? `2px solid ${t.text}` : '2px solid transparent',
              color: tab === x.k ? t.text : t.textDim,
              fontWeight: tab === x.k ? 800 : 500, fontSize: 14, cursor: 'pointer', fontFamily: t.font,
            }}>{x.label}</button>
          ))}
        </div>

        {tab === 'general' && <JellyGeneralRecharge t={t} theme={theme}/>}
        {tab === 'auto' && <JellyAutoRecharge t={t} theme={theme}/>}
        {tab === 'intro' && <JellyIntro t={t}/>}
        {tab === 'history' && <JellyHistory t={t}/>}
        {tab === 'faq' && <JellyFAQ t={t}/>}
      </div>
    </div>
  );
}

function JellyGeneralRecharge({ t, theme }) {
  return (
    <div>
      <div style={{ fontFamily: t.fontDisplay, fontSize: 22, fontWeight: 800, marginBottom: 20 }}>일반 충전</div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {JELLY_PACKS.map(p => (
          <div key={p.jelly} style={{
            padding: '14px 20px', border: p.best ? `1.5px solid ${t.ok}` : `1px solid ${t.line}`, borderRadius: 10,
            background: theme === 'dark' ? 'rgba(20,22,36,0.55)' : t.surface,
            display: 'flex', alignItems: 'center', gap: 16, cursor: 'pointer', position: 'relative',
          }}>
            {p.best && (
              <div style={{
                position: 'absolute', top: -10, left: 20, padding: '3px 10px', borderRadius: 10,
                background: t.ok, color: theme === 'dark' ? '#071014' : '#fff',
                fontSize: 11, fontWeight: 800,
              }}>👍 Best Offer</div>
            )}
            <div style={{
              width: 40, height: 40, borderRadius: '50%',
              background: p.jelly >= 160 ? '#FFC857' : p.jelly >= 60 ? '#22D3EE' : '#FBBF24',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 20,
            }}>🍮</div>
            <div style={{ flex: 1 }}>
              <div style={{ fontFamily: t.fontDisplay, fontSize: 18, fontWeight: 800 }}>젤리 {p.jelly}</div>
              {p.bonus > 0 && (
                <div style={{ fontSize: 11, color: t.textDim }}>
                  적립젤리 <span style={{ color: t.accent2, fontWeight: 800 }}>{p.bonus}</span>
                </div>
              )}
            </div>
            <div style={{ textAlign: 'right' }}>
              <div style={{ fontSize: 14, fontWeight: 700 }}>{p.price}</div>
              {p.benefit && (
                <div style={{
                  marginTop: 4, display: 'inline-block', padding: '2px 6px', borderRadius: 4,
                  background: t.chip, color: t.hot, fontSize: 10, fontWeight: 800,
                }}>{p.benefit}</div>
              )}
            </div>
          </div>
        ))}
      </div>
      <div style={{ marginTop: 20, fontSize: 11, color: t.textDim, lineHeight: 1.6, fontFamily: t.fontMono }}>
        · 젤리는 Connectfin 서비스 내에서 사용 가능한 결제수단입니다.<br/>
        · 충전젤리는 환불이 가능하나, 적립젤리는 환불 대상이 아닙니다.<br/>
        · GET /me/jelly · POST /jelly/recharge (idempotency-key 필수)
      </div>
    </div>
  );
}

function JellyAutoRecharge({ t, theme }) {
  return (
    <div>
      <div style={{ fontFamily: t.fontDisplay, fontSize: 22, fontWeight: 800, marginBottom: 20 }}>자동 충전</div>
      <button style={{
        padding: '8px 16px', borderRadius: 18, border: `1px solid ${t.line}`,
        background: 'transparent', color: t.text, fontSize: 12, fontWeight: 700, cursor: 'pointer', marginBottom: 14,
        fontFamily: t.font,
      }}>자동 결제 수단 관리</button>
      <div style={{
        padding: '20px', border: `1px dashed ${t.lineStrong}`, borderRadius: 12,
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
        color: t.textDim, cursor: 'pointer', fontSize: 13, marginBottom: 20,
      }}>
        <span style={{
          width: 22, height: 22, borderRadius: '50%', background: t.ok,
          color: theme === 'dark' ? '#071014' : '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 14, fontWeight: 800,
        }}>+</span>
        젤리 자동 결제 수단 추가
      </div>
      <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 10 }}>자동 결제 서비스 유의 사항</div>
      <ul style={{ paddingLeft: 18, color: t.textDim, fontSize: 12, lineHeight: 1.8, margin: 0 }}>
        <li>젤리 자동 결제 수단은 최대 5개까지 등록 가능하며, 유효한 결제 수단을 등록하여야 합니다.</li>
        <li>젤리 자동 결제 수단은 젤리와 관련된 자동 결제 및 충전 서비스만을 지원합니다.</li>
        <li>젤리 자동 결제 수단이 등록되지 않은 상태이거나, 등록한 젤리 자동 결제 수단으로 결제가 불가능한 경우 젤리 자동 결제 및 충전 서비스가 진행되지 않습니다.</li>
        <li>Connectfin 계정 탈퇴 시, 등록한 자동 결제 수단은 즉시 비활성화됩니다.</li>
      </ul>
    </div>
  );
}

function JellyIntro({ t }) {
  return (
    <div style={{ color: t.textDim, fontSize: 13, lineHeight: 1.7 }}>
      🍮 <b style={{ color: t.text }}>Jelly</b>는 Connectfin 내에서 사용할 수 있는 가상 결제수단입니다.<br/><br/>
      · 아티스트에게 팬레터를 보낼 때<br/>
      · 멤버십 전용 컨텐츠를 결제할 때<br/>
      · Raffle 응모 · DM 구독 갱신 등에 사용됩니다.<br/><br/>
      <span style={{ fontFamily: t.fontMono, fontSize: 11 }}>
        · 충전한도 월 1,500 / 1회 충전 최대 240<br/>
        · 결제 이력은 "조회" 탭에서 확인할 수 있습니다.
      </span>
    </div>
  );
}

function JellyHistory({ t }) {
  return (
    <div style={{ color: t.textDim, fontSize: 13 }}>
      최근 90일 이내 거래 내역이 없습니다.
      <div style={{ fontFamily: t.fontMono, fontSize: 11, marginTop: 16, opacity: 0.7 }}>
        GET /me/jelly/ledger?cursor=...
      </div>
    </div>
  );
}

function JellyFAQ({ t }) {
  const items = [
    { q: '젤리는 환불이 가능한가요?', a: '충전젤리는 환불이 가능하며, 적립젤리는 환불 대상이 아닙니다.' },
    { q: '자동 충전은 어떻게 설정하나요?', a: '자동 충전 탭에서 결제 수단을 등록한 후 활성화할 수 있습니다.' },
    { q: '결제 오류가 발생했어요.', a: '일시적인 네트워크 오류인 경우가 많습니다. 다시 시도해주세요.' },
  ];
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
      {items.map((it, i) => (
        <div key={i} style={{ padding: 14, border: `1px solid ${t.line}`, borderRadius: 10 }}>
          <div style={{ fontSize: 13, fontWeight: 700, marginBottom: 6 }}>Q. {it.q}</div>
          <div style={{ fontSize: 12, color: t.textDim }}>{it.a}</div>
        </div>
      ))}
    </div>
  );
}

// ──────────────── Shop (external stub) ────────────────
function DesktopShop({ t, theme }) {
  return (
    <div style={{ maxWidth: 1200, margin: '0 auto', padding: '60px 24px', textAlign: 'center' }}>
      <div style={{ fontSize: 64, marginBottom: 20 }}>🛍</div>
      <div style={{ fontFamily: t.fontDisplay, fontSize: 28, fontWeight: 800, marginBottom: 10 }}>Connectfin Shop</div>
      <div style={{ color: t.textDim, fontSize: 14, marginBottom: 30 }}>
        공식 MD 상품과 한정 아이템을 만나보세요
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 16, textAlign: 'left' }}>
        {ARTISTS.slice(0, 8).map(a => (
          <div key={a.id} style={{
            aspectRatio: '3/4', borderRadius: 12, overflow: 'hidden', position: 'relative',
            background: `linear-gradient(135deg, ${a.color1}, ${a.color2})`,
            cursor: 'pointer',
          }}>
            <div style={{ position: 'absolute', bottom: 14, left: 14, right: 14, color: '#fff' }}>
              <div style={{ fontSize: 10, opacity: 0.8, letterSpacing: 1, fontFamily: t.fontMono }}>OFFICIAL MD</div>
              <div style={{ fontWeight: 800, fontSize: 15, marginTop: 2 }}>{a.name}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

Object.assign(window, {
  DesktopHome, DesktopJellyShop, DesktopShop,
});
