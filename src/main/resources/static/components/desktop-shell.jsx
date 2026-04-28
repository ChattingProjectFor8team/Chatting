// Connectfin — Desktop shell: global nav + content column + right sidebar

function DesktopShell({ t, theme, children, activeArtist, onNavGlobal, globalView, onArtistOpen, onNavProfile, profileTab, authUser, onShowLogin, onLogout }) {
  return (
    <div style={{
      minHeight: '100vh', width: '100%', background: t.bg, color: t.text,
      fontFamily: t.font, display: 'flex',
    }}>
      <DesktopLeftNav t={t} theme={theme} globalView={globalView} onNavGlobal={onNavGlobal} onArtistOpen={onArtistOpen}/>
      <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column' }}>
        <DesktopTopbar t={t} theme={theme} activeArtist={activeArtist} globalView={globalView} profileTab={profileTab} onNavProfile={onNavProfile} onNavGlobal={onNavGlobal} authUser={authUser} onShowLogin={onShowLogin} onLogout={onLogout}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          {children}
        </div>
      </div>
    </div>
  );
}

// ───────── Left nav ─────────
function DesktopLeftNav({ t, theme, globalView, onNavGlobal, onArtistOpen }) {
  const dark = theme === 'dark';
  return (
    <aside style={{
      width: 240, flexShrink: 0, borderRight: `1px solid ${t.line}`,
      padding: '18px 16px', position: 'sticky', top: 0, height: '100vh',
      overflowY: 'auto', background: dark ? 'rgba(10,11,16,0.5)' : 'rgba(255,255,255,0.5)',
    }}>
      <button onClick={() => onNavGlobal('home')} aria-label="홈으로" style={{
        display: 'flex', alignItems: 'center', gap: 10, padding: '4px 8px 18px',
        background: 'transparent', border: 'none', cursor: 'pointer', textAlign: 'left',
        width: '100%', color: t.text,
      }}>
        <Infinity8 size={26} color={t.accent} color2={t.accent2} stroke={8}/>
        <div style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 18, letterSpacing: -0.4 }}>Connectfin</div>
      </button>

      <NavItem t={t} icon="⌂" label="홈" active={globalView === 'home'} onClick={() => onNavGlobal('home')}/>
      <NavItem t={t} icon="✉" label="DM" active={globalView === 'dm'} onClick={() => onNavGlobal('dm')}/>
      <NavItem t={t} icon="🎰" label="Raffle" active={globalView === 'raffle'} onClick={() => onNavGlobal('raffle')}/>
      <NavItem t={t} icon="⋯" label="더보기" />

      <NavSection t={t} label="내 커뮤니티"/>
      {ARTISTS.slice(0, 6).map(a => (
        <NavArtist key={a.id} artist={a} t={t} onClick={() => onArtistOpen(a.id)}/>
      ))}
      <ArtistSearchSection t={t} theme={theme} onArtistOpen={onArtistOpen}/>

      <NavSection t={t} label="서비스 바로가기"/>
      <NavItem t={t} icon="🛍" label="Shop" onClick={() => onNavGlobal('shop')} active={globalView === 'shop'}/>
      <NavItem t={t} icon="🍮" label="Jelly Shop" onClick={() => onNavGlobal('jelly')} active={globalView === 'jelly'}/>

      <div style={{
        marginTop: 20, padding: 12, borderRadius: 10, border: `1px dashed ${t.line}`,
        fontFamily: t.fontMono, fontSize: 10, color: t.textMuted, lineHeight: 1.5, letterSpacing: 0.3,
      }}>
        DEV NOTE<br/>
        좌측 네비 = user.subscribed_communities
        <br/>GET /me/communities
      </div>
    </aside>
  );
}

function NavItem({ icon, label, active, onClick, t }) {
  return (
    <button onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 12, width: '100%', textAlign: 'left',
      padding: '9px 10px', borderRadius: 10,
      background: active ? t.chip : 'transparent',
      color: active ? t.accent : t.text,
      border: 'none', cursor: onClick ? 'pointer' : 'default',
      fontSize: 14, fontWeight: active ? 700 : 500, fontFamily: t.font, marginBottom: 2,
    }}>
      <span style={{ fontSize: 16, width: 20, textAlign: 'center' }}>{icon}</span>
      <span>{label}</span>
    </button>
  );
}

function NavSection({ label, t }) {
  return (
    <div style={{
      fontFamily: t.fontMono, fontSize: 10, letterSpacing: 1,
      color: t.textMuted, padding: '18px 10px 8px',
    }}>{label.toUpperCase()}</div>
  );
}

function NavArtist({ artist, t, onClick }) {
  return (
    <button onClick={onClick} style={{
      display: 'flex', alignItems: 'center', gap: 10, width: '100%', textAlign: 'left',
      padding: '6px 10px', borderRadius: 10, background: 'transparent',
      color: t.text, border: 'none', cursor: 'pointer',
      fontSize: 13, fontWeight: 600, fontFamily: t.font, marginBottom: 2, position: 'relative',
    }}>
      <ArtistAvatar artist={artist} size={22} t={t}/>
      <span style={{ flex: 1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{artist.name}</span>
      {artist.live && (
        <span style={{
          width: 6, height: 6, borderRadius: '50%', background: t.hot,
          boxShadow: `0 0 8px ${t.hot}`, animation: 'connectfin-pulse 1.2s ease-in-out infinite',
        }}/>
      )}
    </button>
  );
}

// ───────── Top bar (profile tabs live here when on artist page) ─────────
function DesktopTopbar({ t, theme, activeArtist, globalView, profileTab, onNavProfile, onNavGlobal, authUser, onShowLogin, onLogout }) {
  const dark = theme === 'dark';
  const [jellyBalance, setJellyBalance] = React.useState(null);

  const fetchJelly = React.useCallback(() => {
    if (!authUser?.id) { setJellyBalance(null); return; }
    window.ConnectfinAPI.api('/api/payment/v1/jelly/balance', {
      headers: { 'X-User-Id': String(authUser.id) }
    })
      .then(data => setJellyBalance(data.currentBalance))
      .catch(err => { console.warn('Jelly balance failed:', err?.message || err); });
  }, [authUser?.id]);

  React.useEffect(() => { fetchJelly(); }, [fetchJelly]);

  React.useEffect(() => {
    const handler = () => fetchJelly();
    window.addEventListener('connectfin:jelly-changed', handler);
    return () => window.removeEventListener('connectfin:jelly-changed', handler);
  }, [fetchJelly]);

  return (
    <header style={{
      height: 56, flexShrink: 0, display: 'flex', alignItems: 'center',
      padding: '0 24px', borderBottom: `1px solid ${t.line}`,
      position: 'sticky', top: 0, zIndex: 20,
      background: dark ? 'rgba(10,11,16,0.72)' : 'rgba(255,255,255,0.8)',
      backdropFilter: 'blur(20px)',
    }}>
      {globalView === 'artist' && activeArtist ? (
        <>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, minWidth: 160 }}>
            <ArtistAvatar artist={activeArtist} size={28} t={t}/>
            <div style={{ fontFamily: t.fontDisplay, fontWeight: 800, fontSize: 16 }}>{activeArtist.name}</div>
            <button style={{
              padding: '4px 10px', borderRadius: 14, border: 'none',
              background: t.chip, color: t.accent, fontSize: 11, fontWeight: 700, cursor: 'pointer',
              fontFamily: t.fontMono, letterSpacing: 0.3,
            }}>+ 가입하기</button>
          </div>
          <nav style={{ display: 'flex', alignItems: 'center', gap: 4, marginLeft: 24, flex: 1 }}>
            {PROFILE_TABS.map(tab => {
              const active = profileTab === tab.k;
              return (
                <button key={tab.k} onClick={() => onNavProfile(tab.k)} style={{
                  padding: '18px 14px', background: 'transparent', border: 'none',
                  borderBottom: active ? `2px solid ${t.accent}` : '2px solid transparent',
                  color: active ? t.accent : t.text,
                  fontSize: 14, fontWeight: active ? 800 : 600, cursor: 'pointer',
                  fontFamily: t.font,
                }}>
                  {tab.label}{tab.external && <span style={{ fontSize: 10, marginLeft: 4, opacity: 0.6 }}>↗</span>}
                </button>
              );
            })}
          </nav>
        </>
      ) : (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 16 }}>
          {globalView === 'live' && (
            <button onClick={() => onNavGlobal && onNavGlobal('home')} aria-label="뒤로" style={{
              width: 36, height: 36, borderRadius: 10, border: `1px solid ${t.line}`,
              background: 'transparent', color: t.text, fontSize: 16, cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>←</button>
          )}
          <div style={{ fontFamily: t.fontDisplay, fontSize: 17, fontWeight: 800 }}>
            {globalView === 'home' ? '홈' : globalView === 'jelly' ? 'Jelly Shop' : globalView === 'shop' ? 'Shop' : globalView === 'dm' ? 'DM' : globalView === 'live' ? 'LIVE' : globalView === 'raffle' ? 'Raffle' : ''}
          </div>
        </div>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginLeft: 'auto' }}>
        <span style={{ fontFamily: t.fontMono, fontSize: 11, color: t.textDim }}>
          {jellyBalance !== null ? jellyBalance : '—'} 🍮
        </span>
        <span style={{ fontSize: 18, cursor: 'pointer' }}>🔔</span>
        {authUser ? (
          <button onClick={onLogout} style={{
            padding: '6px 14px', borderRadius: 14, border: `1px solid ${t.line}`,
            background: 'transparent', color: t.text, fontSize: 12, fontWeight: 600,
            cursor: 'pointer', fontFamily: t.fontMono,
          }}>로그아웃</button>
        ) : (
          <button onClick={onShowLogin} style={{
            padding: '6px 14px', borderRadius: 14, border: 'none',
            background: t.gradient, color: '#fff', fontSize: 12, fontWeight: 700,
            cursor: 'pointer', fontFamily: t.fontMono,
          }}>로그인</button>
        )}
      </div>
    </header>
  );
}

const PROFILE_TABS = [
  { k: 'highlight', label: 'Highlight' },
  { k: 'fan', label: 'Fan' },
  { k: 'artist', label: 'Artist' },
  { k: 'fanletter', label: 'Fan Letter' },
  { k: 'media', label: 'Media' },
  { k: 'live', label: 'LIVE' },
  { k: 'notice', label: 'Notice' },
  { k: 'shop', label: 'Shop', external: true },
  { k: 'admin', label: 'Admin' },
];

// ───────── Right sidebar ─────────
function DesktopRightSidebar({ t, theme, artist, onOpenDM, onOpenMembership }) {
  const dark = theme === 'dark';
  const hasToken = !!window.ConnectfinAPI?.getToken();

  const [membershipStatus, setMembershipStatus] = React.useState(null);
  const [dmStatus, setDmStatus] = React.useState(null);
  const [purchasingMembership, setPurchasingMembership] = React.useState(false);
  const [purchasingDm, setPurchasingDm] = React.useState(false);

  React.useEffect(() => {
    if (!hasToken || !artist?.id) return;
    window.ConnectfinAPI.api(`/api/v1/subscriptions/membership/${artist.id}/status`)
      .then(data => setMembershipStatus(data))
      .catch(err => { console.warn('Membership status failed:', err?.message || err); });
    window.ConnectfinAPI.api(`/api/v1/subscriptions/dm/${artist.id}/status`)
      .then(data => setDmStatus(data))
      .catch(err => { console.warn('DM status failed:', err?.message || err); });
  }, [hasToken, artist?.id]);

  const purchaseMembership = async () => {
    if (purchasingMembership || !hasToken) return;
    setPurchasingMembership(true);
    try {
      await window.ConnectfinAPI.api(`/api/v1/subscriptions/membership/${artist.id}`, { method: 'POST' });
      setMembershipStatus({ active: true, expiredAt: null });
      window.dispatchEvent(new CustomEvent('connectfin:jelly-changed'));
      alert('팬 멤버십 구매 완료!');
    } catch (err) {
      alert('구매 실패: ' + (err.message || '젤리가 부족합니다'));
    } finally {
      setPurchasingMembership(false);
    }
  };

  const purchaseDm = async () => {
    if (purchasingDm || !hasToken) return;
    setPurchasingDm(true);
    try {
      await window.ConnectfinAPI.api(`/api/v1/subscriptions/dm/${artist.id}`, { method: 'POST' });
      setDmStatus({ active: true, expiredAt: null });
      window.dispatchEvent(new CustomEvent('connectfin:jelly-changed'));
      alert('DM 구독 완료!');
    } catch (err) {
      alert('구매 실패: ' + (err.message || '젤리가 부족합니다'));
    } finally {
      setPurchasingDm(false);
    }
  };

  return (
    <aside style={{ width: 320, flexShrink: 0, padding: '20px 20px 20px 0', display: 'flex', flexDirection: 'column', gap: 14 }}>
      {/* Digital membership */}
      <div style={{
        borderRadius: 14, padding: 18, border: `1px solid ${t.line}`,
        background: dark ? '#141624' : t.surface,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <div style={{ width: 20, height: 20, borderRadius: 6, background: t.accent2, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: '#fff' }}>D</div>
          <div style={{ fontWeight: 800, fontSize: 14 }}>{artist?.name}. Digital membership</div>
        </div>
        <div style={{ fontSize: 12, color: t.textDim, lineHeight: 1.5, marginBottom: 12 }}>
          디지털 멤버십에 가입하고 다양한 혜택을 즐겨보세요.
        </div>
        {dmStatus?.active ? (
          <div style={{
            width: '100%', padding: '10px 0', borderRadius: 10, textAlign: 'center',
            background: t.surface, border: `1px solid ${t.line}`,
            fontWeight: 700, fontSize: 13, color: t.accent2, fontFamily: t.font,
          }}>✓ DM 구독 중{dmStatus.expiredAt ? ` (~ ${new Date(dmStatus.expiredAt).toLocaleDateString()})` : ''}</div>
        ) : (
          <button onClick={purchaseDm} disabled={purchasingDm} style={{
            width: '100%', padding: '10px 0', borderRadius: 10, border: 'none',
            background: t.accent2, color: dark ? '#071014' : '#fff',
            fontWeight: 800, fontSize: 13, cursor: 'pointer', fontFamily: t.font,
            opacity: purchasingDm ? 0.5 : 1,
          }}>{purchasingDm ? '구매 중...' : 'DM 구독하기 (15 Jelly)'}</button>
        )}
      </div>

      {/* Premium membership */}
      <div style={{
        borderRadius: 14, padding: 18, border: `1px solid ${t.line}`,
        background: dark ? '#141624' : t.surface,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          <div style={{ width: 20, height: 20, borderRadius: 6, background: t.accent, display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, color: '#fff' }}>M</div>
          <div style={{ fontWeight: 800, fontSize: 14 }}>{artist?.name}. Membership</div>
        </div>
        <div style={{ fontSize: 12, color: t.textDim, lineHeight: 1.5, marginBottom: 12 }}>
          지금 멤버십에 가입하고 특별한 혜택을 누려보세요.
        </div>
        {membershipStatus?.active ? (
          <div style={{
            width: '100%', padding: '10px 0', borderRadius: 10, textAlign: 'center',
            background: t.surface, border: `1px solid ${t.line}`,
            fontWeight: 700, fontSize: 13, color: t.accent, fontFamily: t.font,
          }}>✓ 멤버십 구독 중{membershipStatus.expiredAt ? ` (~ ${new Date(membershipStatus.expiredAt).toLocaleDateString()})` : ''}</div>
        ) : (
          <button onClick={purchaseMembership} disabled={purchasingMembership} style={{
            width: '100%', padding: '10px 0', borderRadius: 10, border: 'none',
            background: t.accent2, color: dark ? '#071014' : '#fff',
            fontWeight: 800, fontSize: 13, cursor: 'pointer', fontFamily: t.font,
            opacity: purchasingMembership ? 0.5 : 1,
          }}>{purchasingMembership ? '구매 중...' : '멤버십 가입하기 (9 Jelly)'}</button>
        )}
      </div>

      {/* DM widget */}
      <div style={{
        borderRadius: 14, padding: 18, border: `1px solid ${t.line}`,
        background: dark ? '#141624' : t.surface,
      }}>
        <div style={{ fontWeight: 800, fontSize: 14, marginBottom: 10 }}>Connectfin DM</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <ArtistAvatar artist={artist || ARTISTS[0]} size={32} t={t}/>
          <div style={{ flex: 1 }}>
            <div style={{ fontWeight: 700, fontSize: 13 }}>{artist?.name || ARTISTS[0].name}.</div>
            <div style={{
              display: 'inline-block', padding: '3px 10px', borderRadius: 12,
              background: t.accent2, color: dark ? '#071014' : '#fff',
              fontSize: 11, fontWeight: 700, marginTop: 2,
            }}>지금 뭐해?</div>
          </div>
        </div>
        {dmStatus?.active ? (
          <button onClick={onOpenDM} style={{
            width: '100%', padding: '10px 0', borderRadius: 10, border: 'none',
            background: t.accent2, color: dark ? '#071014' : '#fff',
            fontWeight: 800, fontSize: 13, cursor: 'pointer', fontFamily: t.font,
          }}>DM 보내기</button>
        ) : (
          <button onClick={purchaseDm} disabled={purchasingDm} style={{
            width: '100%', padding: '10px 0', borderRadius: 10, border: 'none',
            background: t.line, color: t.textDim,
            fontWeight: 800, fontSize: 13, cursor: 'pointer', fontFamily: t.font,
            opacity: purchasingDm ? 0.5 : 1,
          }}>{purchasingDm ? '구매 중...' : 'DM 구독 필요 (15 Jelly)'}</button>
        )}
      </div>

      {/* About artist */}
      {artist && (
        <div style={{
          borderRadius: 14, padding: 18, border: `1px solid ${t.line}`,
          background: dark ? '#141624' : t.surface,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
            <ArtistAvatar artist={artist} size={36} t={t}/>
            <div>
              <div style={{ fontWeight: 800, fontSize: 15 }}>{artist.name}</div>
              <div style={{ fontSize: 10, color: t.textDim, fontFamily: t.fontMono, letterSpacing: 0.3 }}>
                {artist.genre}
              </div>
            </div>
          </div>
          <div style={{ display: 'flex', gap: 8, marginBottom: 10, fontSize: 12, color: t.textDim }}>
            <span>▶ YouTube</span>
            <span>✖ X</span>
            <span>◐ IG</span>
          </div>
          <div style={{ fontSize: 12, color: t.textDim, lineHeight: 1.55 }}>
            팬과 아티스트가 가장 가까운 거리에서 연결되는 디지털 경험.
            {artist.stage}의 무대 뒤 순간들, 새로운 이야기들을 Connectfin에서 만나보세요.
          </div>
        </div>
      )}
    </aside>
  );
}

// ───────── Artist search (left nav) ─────────
function ArtistSearchSection({ t, theme, onArtistOpen }) {
  const [query, setQuery] = React.useState('');
  const [results, setResults] = React.useState(null);
  const [popular, setPopular] = React.useState([]);
  const [showSearch, setShowSearch] = React.useState(false);
  const debounceRef = React.useRef(null);

  // 인기 검색어 로드 (최초 1회)
  React.useEffect(() => {
    window.ConnectfinAPI.api('/api/member/v1/artists/search/popular?offset=0')
      // ConnectfinAPI.api는 ApiResponse 래퍼를 벗긴 뒤 json.data만 반환한다.
      // 이 엔드포인트의 data는 배열이 아니라 OffsetSliceResponse이므로
      // data.content를 읽어야 하고, 그대로 data.slice(...)를 호출하면 런타임에서 깨진다.
      .then(data => setPopular(Array.isArray(data?.content) ? data.content.slice(0, 5) : []))
      .catch(err => { console.warn('API error suppressed:', err?.message || err); });
  }, []);

  // 검색 (debounce 300ms)
  React.useEffect(() => {
    if (!query.trim()) {
      setResults(null);
      return;
    }
    if (!window.ConnectfinAPI.getToken()) return;

    clearTimeout(debounceRef.current);
    debounceRef.current = setTimeout(() => {
      window.ConnectfinAPI.api(`/api/member/v3/artists/search?keyword=${encodeURIComponent(query)}`)
        .then(data => setResults(data.content || []))
        .catch(err => { console.warn('Search failed:', err?.message || err); setResults([]); });
    }, 300);
    return () => clearTimeout(debounceRef.current);
  }, [query]);

  if (!showSearch) {
    return (
      <button onClick={() => setShowSearch(true)} style={{
        display: 'flex', alignItems: 'center', gap: 12, width: '100%', textAlign: 'left',
        padding: '9px 10px', borderRadius: 10,
        background: 'transparent', color: t.text,
        border: 'none', cursor: 'pointer',
        fontSize: 14, fontWeight: 500, fontFamily: t.font, marginBottom: 2,
      }}>
        <span style={{ fontSize: 16, width: 20, textAlign: 'center' }}>⌕</span>
        <span>커뮤니티 찾기</span>
      </button>
    );
  }

  return (
    <div style={{ marginBottom: 8 }}>
      <div style={{ display: 'flex', gap: 6, marginBottom: 8 }}>
        <input
          type="text" value={query} onChange={e => setQuery(e.target.value)}
          placeholder="아티스트 검색..." autoFocus
          style={{
            flex: 1, padding: '8px 12px', borderRadius: 8,
            border: `1px solid ${t.line}`,
            background: theme === 'dark' ? '#12141C' : '#fff',
            color: t.text, fontSize: 13, fontFamily: t.font,
          }}
        />
        <button onClick={() => { setShowSearch(false); setQuery(''); setResults(null); }} style={{
          padding: '6px 10px', borderRadius: 8, border: `1px solid ${t.line}`,
          background: 'transparent', color: t.textDim, fontSize: 12, cursor: 'pointer',
        }}>✕</button>
      </div>

      {/* 검색 결과 */}
      {results !== null && (
        <div style={{ marginBottom: 8 }}>
          {results.length === 0 ? (
            <div style={{ padding: '8px 10px', fontSize: 12, color: t.textDim }}>검색 결과가 없습니다</div>
          ) : results.map(a => (
            <button key={a.id} onClick={() => {
              // ARTISTS에 없는 백엔드 결과면 즉시 머지 → 프로필이 빈 화면이 되는 것을 방지
              if (Array.isArray(window.ARTISTS) && !window.ARTISTS.some(x => x.id === a.id)) {
                const hue = (a.id * 47) % 360;
                window.ARTISTS.push({
                  id: a.id, name: a.name, slug: a.slug,
                  stage: a.name, genre: 'ARTIST',
                  color1: `hsl(${hue}, 70%, 60%)`, color2: `hsl(${hue}, 70%, 80%)`,
                  members: 1, live: false, viewers: 0, followers: '—',
                  profileImageUrl: a.profileImageUrl, fromBackend: true,
                });
                window.dispatchEvent(new CustomEvent('connectfin:artists-changed'));
              }
              onArtistOpen(a.id); setShowSearch(false); setQuery(''); setResults(null);
            }} style={{
              display: 'flex', alignItems: 'center', gap: 10, width: '100%', textAlign: 'left',
              padding: '8px 10px', borderRadius: 8,
              background: 'transparent', border: 'none', cursor: 'pointer', color: t.text,
            }}>
              <div style={{
                width: 28, height: 28, borderRadius: '50%', background: t.gradient,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                color: '#fff', fontSize: 10, fontWeight: 800,
              }}>{a.name?.slice(0, 2)}</div>
              <div>
                <div style={{ fontSize: 13, fontWeight: 600 }}>{a.name}</div>
                <div style={{ fontSize: 10, color: t.textDim, fontFamily: t.fontMono }}>@{a.slug}</div>
              </div>
            </button>
          ))}
        </div>
      )}

      {/* 인기 검색어 (검색 중이 아닐 때만) */}
      {results === null && popular.length > 0 && (
        <div style={{ padding: '4px 10px' }}>
          <div style={{ fontSize: 10, color: t.textMuted, fontFamily: t.fontMono, letterSpacing: 0.5, marginBottom: 6 }}>인기 검색어</div>
          {popular.map((item, i) => (
            <button key={item.keyword} onClick={() => setQuery(item.keyword)} style={{
              display: 'flex', alignItems: 'center', gap: 8, width: '100%', textAlign: 'left',
              padding: '5px 0', background: 'transparent', border: 'none',
              cursor: 'pointer', color: t.text, fontSize: 12,
            }}>
              <span style={{ fontFamily: t.fontMono, fontSize: 11, color: t.accent, fontWeight: 700, width: 16 }}>{i + 1}</span>
              <span>{item.keyword}</span>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

Object.assign(window, {
  DesktopShell, DesktopRightSidebar, PROFILE_TABS,
});
